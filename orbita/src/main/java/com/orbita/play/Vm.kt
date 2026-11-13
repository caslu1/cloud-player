package com.orbita.play

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class LoadState { IDLE, LOADING, READY, ERROR }

/** Estado do app: conta, catálogo (canais, filmes, séries), guia e jogos. */
@androidx.compose.runtime.Stable
class Vm(ctx: Context) {
    private val ctx: Context = ctx.applicationContext
    val store = Store(ctx.applicationContext)

    init { Tmdb.init(ctx.applicationContext) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var source: Source? = null; private set
    var account by mutableStateOf<Account?>(null); private set
    var status by mutableStateOf<AccountStatus?>(null)
    /** Situação no site de ativação (null = desligado ou ainda sem resposta). */
    var act by mutableStateOf<ActState?>(null)
    private var actBusy = false

    var liveState by mutableStateOf(LoadState.IDLE); private set
    var liveError by mutableStateOf<String?>(null); private set
    var liveCats by mutableStateOf<List<Cat>>(emptyList()); private set
    var channels by mutableStateOf<List<Channel>>(emptyList()); private set
    var channelById: Map<String, Channel> = emptyMap(); private set
    var channelsByCat: Map<String, List<Channel>> = emptyMap(); private set
    /** Listas sem as versões repetidas de qualidade (o que aparece nas listas de canais). */
    var uniqueChannels: List<Channel> = emptyList(); private set
    var uniqueByCat: Map<String, List<Channel>> = emptyMap(); private set

    var movieState by mutableStateOf(LoadState.IDLE); private set
    var movieCats by mutableStateOf<List<Cat>>(emptyList()); private set
    var movies by mutableStateOf<List<Movie>>(emptyList()); private set
    var movieById: Map<String, Movie> = emptyMap(); private set
    var moviesByCat: Map<String, List<Movie>> = emptyMap(); private set

    var seriesState by mutableStateOf(LoadState.IDLE); private set
    var seriesCats by mutableStateOf<List<Cat>>(emptyList()); private set
    var series by mutableStateOf<List<Series>>(emptyList()); private set
    var seriesById: Map<String, Series> = emptyMap(); private set
    var seriesByCat: Map<String, List<Series>> = emptyMap(); private set

    /** Guia por canal; o mapa é um estado, então os itens visíveis atualizam quando o guia chega. */
    val epg = StateMap<List<Prog>>()
    private val epgTime = ConcurrentHashMap<String, Long>()
    private val epgRunning = ConcurrentHashMap.newKeySet<String>()
    private val epgGate = Semaphore(4)

    val movieInfos = StateMap<MovieInfo>()
    val seriesInfos = StateMap<SeriesInfo>()


    fun liveExt() = if (store.liveFormat.value == "m3u8") "m3u8" else "ts"

    fun sourceFor(a: Account): Source = if (a.type == "xtream") XtreamSource(a) { liveExt() } else M3uSource(a.m3uUrl)

    /** Tenta entrar. Lança exceção com mensagem em português se der errado. */
    suspend fun login(a: Account, name: String? = null) {
        withContext(Dispatchers.IO) {
            val src = sourceFor(a)
            if (src is XtreamSource) status = src.verify() else src.loadLive()
            source = src
        }
        store.saveAccount(a)
        store.upsertPlaylist(name, a)
        account = a
        startLoading()
    }

    /** Troca para outra lista (tela Playlists): testa a nova antes de largar a atual. */
    suspend fun switchTo(a: Account, name: String? = null) {
        val src = withContext(Dispatchers.IO) { sourceFor(a).also { if (it is XtreamSource) status = it.verify() else it.loadLive() } }
        clearData()
        source = src
        store.saveAccount(a)
        store.upsertPlaylist(name, a)
        account = a
        startLoading()
    }

    /** Apaga uma lista; se era a que está em uso, passa para a próxima ou volta ao login. */
    fun removePlaylist(pl: Playlist) {
        store.removePlaylist(pl)
        if (account?.let { pl.same(it) } == true) {
            val next = store.playlists.firstOrNull()
            if (next == null) logout() else scope.launch { runCatching { switchTo(next.acc) } }
        }
    }

    /** Consulta o site: situação do aparelho e listas cadastradas. Sem conta, entra com a 1ª lista do site. */
    fun checkActivation() {
        if (!Activation.enabled() || actBusy) return
        actBusy = true
        scope.launch {
            val a = withContext(Dispatchers.IO) { runCatching { Activation.register(ctx) }.getOrNull() }
            actBusy = false
            if (a == null) return@launch
            act = a
            val first = a.playlists.firstOrNull()
            if (account == null && first != null && !a.locked && store.lastSiteList() != first.key) {
                store.setLastSiteList(first.key)
                runCatching { login(first.account()) }
            }
        }
    }

    fun resume(): Boolean {
        val a = store.account() ?: return false
        if (store.playlists.none { it.same(a) }) store.upsertPlaylist(null, a)
        account = a
        source = sourceFor(a)
        startLoading()
        scope.launch { status = withContext(Dispatchers.IO) { source?.status() } }
        return true
    }

    fun logout() {
        store.logout()
        clearData()
        account = null
    }

    private fun clearData() {
        source = null; status = null
        liveState = LoadState.IDLE; movieState = LoadState.IDLE; seriesState = LoadState.IDLE
        channels = emptyList(); movies = emptyList(); series = emptyList()
        liveCats = emptyList(); movieCats = emptyList(); seriesCats = emptyList()
        epg.clear(); epgTime.clear(); movieInfos.clear(); seriesInfos.clear()
    }

    fun startLoading() {
        loadLive()
        scope.launch {
            loadMovies().join()
            loadSeries()
        }
    }

    private class LiveData(
        val cats: List<Cat>, val list: List<Channel>, val byId: Map<String, Channel>, val byCat: Map<String, List<Channel>>,
        val unique: List<Channel>, val uniqueByCat: Map<String, List<Channel>>,
    )

    fun loadLive(): Job = scope.launch {
        val src = source ?: return@launch
        liveState = LoadState.LOADING; liveError = null
        // tudo o que é pesado (agrupar dezenas de milhares de itens) roda fora da thread da tela
        runCatching {
            withContext(Dispatchers.Default) {
                val (rawCats, raw) = withContext(Dispatchers.IO) { src.loadLive() }
                val list = raw.distinctBy { it.id }
                val byCat = list.groupBy { it.catId }
                val cats0 = rawCats.distinctBy { it.id }
                val cats = cats0.filter { byCat.containsKey(it.id) } +
                    byCat.keys.filter { k -> cats0.none { it.id == k } }.map { Cat(it, if (it.isBlank()) "Outros" else it) }
                val catNames = cats.associate { it.id to it.name }
                LiveData(cats, list, list.associateBy { it.id }, byCat, Quality.unique(list), byCat.mapValues { Quality.unique(it.value) })
            }
        }.onSuccess { d -> if (source !== src) return@launch;
            channelById = d.byId; channelsByCat = d.byCat; uniqueChannels = d.unique; uniqueByCat = d.uniqueByCat
            liveCats = d.cats; channels = d.list
            liveState = LoadState.READY
        }.onFailure { if (source !== src) return@launch; liveError = it.message ?: "Falha ao carregar os canais."; liveState = LoadState.ERROR }
    }

    fun loadMovies(): Job = scope.launch {
        val src = source ?: return@launch
        if (movieState == LoadState.LOADING) return@launch
        movieState = LoadState.LOADING
        runCatching {
            withContext(Dispatchers.Default) {
                val (rawCats, raw) = withContext(Dispatchers.IO) { src.loadMovies() }
                val list = raw.distinctBy { it.id }
                val byCat = list.groupBy { it.catId }
                Triple(rawCats.distinctBy { it.id }.filter { byCat.containsKey(it.id) }, list, byCat) to list.associateBy { it.id }
            }
        }.onSuccess { (t, byId) -> if (source !== src) return@launch;
            movieById = byId; moviesByCat = t.third; movieCats = t.first; movies = t.second
            movieState = LoadState.READY
        }.onFailure { if (source === src) movieState = LoadState.ERROR }
    }

    fun loadSeries(): Job = scope.launch {
        val src = source ?: return@launch
        if (seriesState == LoadState.LOADING) return@launch
        seriesState = LoadState.LOADING
        runCatching {
            withContext(Dispatchers.Default) {
                val (rawCats, raw) = withContext(Dispatchers.IO) { src.loadSeries() }
                val list = raw.distinctBy { it.id }
                val byCat = list.groupBy { it.catId }
                Triple(rawCats.distinctBy { it.id }.filter { byCat.containsKey(it.id) }, list, byCat) to list.associateBy { it.id }
            }
        }.onSuccess { (t, byId) -> if (source !== src) return@launch;
            seriesById = byId; seriesByCat = t.third; seriesCats = t.first; series = t.second
            seriesState = LoadState.READY
        }.onFailure { if (source === src) seriesState = LoadState.ERROR }
    }

    fun reloadAll() {
        epg.clear(); epgTime.clear()
        val a = account ?: return
        source = sourceFor(a)
        movieState = LoadState.IDLE; seriesState = LoadState.IDLE
        startLoading()
    }

    fun requestEpg(c: Channel, limit: Int = 12) {
        if (source !is XtreamSource) return
        val t = epgTime[c.id]
        val stale = t == null || System.currentTimeMillis() - t > 15 * 60_000 ||
            epg[c.id]?.lastOrNull()?.let { it.end < System.currentTimeMillis() } == true
        if (!stale || !epgRunning.add(c.id)) return
        scope.launch {
            try {
                val list = epgGate.withPermit { withContext(Dispatchers.IO) { runCatching { source?.epg(c, limit) }.getOrNull() } }
                epgTime[c.id] = System.currentTimeMillis()
                if (list != null) epg[c.id] = normalizeEpg(list)
            } finally { epgRunning.remove(c.id) }
        }
    }

    // ---- guia completo (dias) do canal em foco
    val fullEpg = StateMap<List<Prog>>()
    private val fullTime = HashMap<String, Long>()

    fun requestFullEpg(c: Channel) {
        if (source !is XtreamSource) return
        val t = fullTime[c.id]
        if (t != null && System.currentTimeMillis() - t < 30 * 60_000) return
        fullTime[c.id] = System.currentTimeMillis()
        scope.launch {
            val list = epgGate.withPermit { withContext(Dispatchers.IO) { runCatching { source?.epgFull(c) }.getOrNull() } }
            if (!list.isNullOrEmpty()) fullEpg[c.id] = normalizeEpg(list) else fullTime.remove(c.id)
        }
    }

    // ---- controle parental e categorias ocultas
    var adultUnlocked by mutableStateOf(false)
    private val adultRx = Regex("""(?i)(adult|adulto|xxx|\+18|18\+|\[18\]|porn|sexy|\bhot\b|er[oó]tic)""")
    fun isAdult(name: String) = adultRx.containsMatchIn(name)

    /** Aparece na lista (pode estar com cadeado). */
    fun catVisible(kind: String, c: Cat) = !store.isHidden("$kind:${c.id}") && !(isAdult(c.name) && store.adultMode.value == "hide")
    /** Precisa de senha para abrir. */
    fun catLocked(c: Cat) = isAdult(c.name) && store.adultMode.value == "lock" && !adultUnlocked
    /** Pode ser mostrada e aberta agora (entra em Todos, buscas e lançamentos). */
    fun catAllowed(kind: String, c: Cat) = catVisible(kind, c) && !catLocked(c)

    private var allowedKey: String? = null
    private var allowedList: List<Channel> = emptyList()
    /** "Todos os canais" sem as categorias ocultas/bloqueadas. */
    fun allowedChannels(): List<Channel> {
        val blocked = liveCats.filter { !catAllowed("live", it) }.map { it.id }.toSet()
        if (blocked.isEmpty()) return uniqueChannels
        val key = blocked.sorted().joinToString(",") + "#" + uniqueChannels.size
        if (key != allowedKey) { allowedList = uniqueChannels.filter { it.catId !in blocked }; allowedKey = key }
        return allowedList
    }

    fun nowOn(c: Channel): Prog? { val t = System.currentTimeMillis(); return epg[c.id]?.lastOrNull { it.start <= t && t < it.end } }

    fun requestMovieInfo(m: Movie) {
        if (movieInfos.containsKey(m.id)) return
        scope.launch {
            val i = withContext(Dispatchers.IO) { source?.movieInfo(m) } ?: return@launch
            movieInfos[m.id] = i
        }
    }

    suspend fun seriesInfo(s: Series): SeriesInfo {
        seriesInfos[s.id]?.let { return it }
        val src = source ?: throw IllegalStateException("Sem conta")
        val raw = withContext(Dispatchers.IO) { src.seriesInfo(s) }
        val i = SeriesInfo(raw.plot, raw.genre, raw.year, raw.rating, raw.backdrop, raw.cast, raw.seasons.mapValues { (_, l) -> l.distinctBy { it.id } })
        seriesInfos[s.id] = i
        return i
    }

    fun catName(id: String) = liveCats.firstOrNull { it.id == id }?.name.orEmpty()

    // ---- TMDB (logo, fundo, sinopse)
    val meta = StateMap<Tmdb.Meta>()
    private val metaMiss = ConcurrentHashMap.newKeySet<String>()
    private val metaRunning = ConcurrentHashMap.newKeySet<String>()
    private val metaGate = Semaphore(4)
    private var flushJob: Job? = null

    fun metaOf(m: Movie) = meta["m:${m.id}"]
    fun metaOf(s: Series) = meta["s:${s.id}"]
    fun requestMeta(m: Movie) = requestMeta("m:${m.id}", m.name, false, m.tmdb)
    fun requestMeta(s: Series) = requestMeta("s:${s.id}", s.name, true, s.tmdb)

    private fun requestMeta(key: String, name: String, tv: Boolean, hint: String?) {
        if (!Tmdb.enabled || meta.containsKey(key) || metaMiss.contains(key) || !metaRunning.add(key)) return
        scope.launch {
            try {
                val r = metaGate.withPermit { withContext(Dispatchers.IO) { runCatching { Tmdb.find(name, tv, hint) }.getOrNull() } }
                if (r != null) meta[key] = r else metaMiss.add(key)
            } finally { metaRunning.remove(key) }
            if (flushJob?.isActive != true) flushJob = scope.launch { delay(4_000); withContext(Dispatchers.IO) { Tmdb.flush() } }
        }
    }

    // ---- canais da lista agrupados pelo nome (versões FHD/HD/SD do mesmo canal)
    private var indexOf: List<Channel>? = null
    private var index: Map<String, List<Channel>> = emptyMap()

    /** Canais da lista indexados pelo nome normalizado (para achar "Sportv 2" na lista do usuário). */
    fun channelIndex(): Map<String, List<Channel>> {
        if (indexOf !== channels) { index = channels.groupBy { Names.channelKey(it.name) }; indexOf = channels }
        return index
    }

    fun play(item: PlayItem) {
        if (item.kind == Kind.LIVE) item.channel?.let { store.addRecent(it.id) }
    }
}

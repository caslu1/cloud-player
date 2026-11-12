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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class LoadState { IDLE, LOADING, READY, ERROR }

class Match(
    val home: String, val away: String, val comp: String?, val start: Long, val end: Long,
    val desc: String, val channels: MutableList<Channel>,
) {
    fun isLive(now: Long = System.currentTimeMillis()) = now in start until end
    fun isOver(now: Long = System.currentTimeMillis()) = now >= end
}

/** Estado do app: conta, catálogo (canais, filmes, séries), guia e jogos. */
class Vm(ctx: Context) {
    val store = Store(ctx.applicationContext)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var source: Source? = null; private set
    var account by mutableStateOf<Account?>(null); private set
    var status by mutableStateOf<AccountStatus?>(null)

    var liveState by mutableStateOf(LoadState.IDLE); private set
    var liveError by mutableStateOf<String?>(null); private set
    var liveCats by mutableStateOf<List<Cat>>(emptyList()); private set
    var channels by mutableStateOf<List<Channel>>(emptyList()); private set
    var channelById: Map<String, Channel> = emptyMap(); private set
    var channelsByCat: Map<String, List<Channel>> = emptyMap(); private set

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
    val epg = mutableStateMapOf<String, List<Prog>>()
    private val epgTime = ConcurrentHashMap<String, Long>()
    private val epgRunning = ConcurrentHashMap.newKeySet<String>()
    private val epgGate = Semaphore(4)

    val movieInfos = mutableStateMapOf<String, MovieInfo>()
    val seriesInfos = mutableStateMapOf<String, SeriesInfo>()

    var matchState by mutableStateOf(LoadState.IDLE); private set
    var matches by mutableStateOf<List<Match>>(emptyList()); private set
    var matchProgress by mutableStateOf(0f); private set
    var footballChannels by mutableStateOf<List<Channel>>(emptyList()); private set
    private var matchJob: Job? = null

    fun liveExt() = if (store.liveFormat.value == "m3u8") "m3u8" else "ts"

    fun sourceFor(a: Account): Source = if (a.type == "xtream") XtreamSource(a) { liveExt() } else M3uSource(a.m3uUrl)

    /** Tenta entrar. Lança exceção com mensagem em português se der errado. */
    suspend fun login(a: Account) {
        withContext(Dispatchers.IO) {
            val src = sourceFor(a)
            if (src is XtreamSource) status = src.verify() else src.loadLive()
            source = src
        }
        store.saveAccount(a)
        account = a
        startLoading()
    }

    fun resume(): Boolean {
        val a = store.account() ?: return false
        account = a
        source = sourceFor(a)
        startLoading()
        scope.launch { status = withContext(Dispatchers.IO) { source?.status() } }
        return true
    }

    fun logout() {
        matchJob?.cancel()
        store.logout()
        source = null; account = null; status = null
        liveState = LoadState.IDLE; movieState = LoadState.IDLE; seriesState = LoadState.IDLE; matchState = LoadState.IDLE
        channels = emptyList(); movies = emptyList(); series = emptyList(); matches = emptyList()
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

    fun loadLive(): Job = scope.launch {
        val src = source ?: return@launch
        liveState = LoadState.LOADING; liveError = null
        runCatching { withContext(Dispatchers.IO) { src.loadLive() } }
            .onSuccess { (rawCats, raw) ->
                val cats = rawCats.distinctBy { it.id }; val list = raw.distinctBy { it.id }
                channelById = list.associateBy { it.id }
                channelsByCat = list.groupBy { it.catId }
                liveCats = cats.filter { channelsByCat.containsKey(it.id) } +
                    channelsByCat.keys.filter { k -> cats.none { it.id == k } }.map { Cat(it, if (it.isBlank()) "Outros" else it) }
                channels = list
                liveState = LoadState.READY
                footballChannels = list.filter { c ->
                    MatchLogic.isSportsName(c.name) || MatchLogic.isSportsName(liveCats.firstOrNull { it.id == c.catId }?.name.orEmpty())
                }
            }
            .onFailure { liveError = it.message ?: "Falha ao carregar os canais."; liveState = LoadState.ERROR }
    }

    fun loadMovies(): Job = scope.launch {
        val src = source ?: return@launch
        if (movieState == LoadState.LOADING) return@launch
        movieState = LoadState.LOADING
        runCatching { withContext(Dispatchers.IO) { src.loadMovies() } }
            .onSuccess { (rawCats, raw) ->
                val cats = rawCats.distinctBy { it.id }; val list = raw.distinctBy { it.id }
                movieById = list.associateBy { it.id }
                moviesByCat = list.groupBy { it.catId }
                movieCats = cats.filter { moviesByCat.containsKey(it.id) }
                movies = list
                movieState = LoadState.READY
            }
            .onFailure { movieState = LoadState.ERROR }
    }

    fun loadSeries(): Job = scope.launch {
        val src = source ?: return@launch
        if (seriesState == LoadState.LOADING) return@launch
        seriesState = LoadState.LOADING
        runCatching { withContext(Dispatchers.IO) { src.loadSeries() } }
            .onSuccess { (rawCats, raw) ->
                val cats = rawCats.distinctBy { it.id }; val list = raw.distinctBy { it.id }
                seriesById = list.associateBy { it.id }
                seriesByCat = list.groupBy { it.catId }
                seriesCats = cats.filter { seriesByCat.containsKey(it.id) }
                series = list
                seriesState = LoadState.READY
            }
            .onFailure { seriesState = LoadState.ERROR }
    }

    fun reloadAll() {
        epg.clear(); epgTime.clear(); matchState = LoadState.IDLE
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
                if (list != null) epg[c.id] = list
            } finally { epgRunning.remove(c.id) }
        }
    }

    fun nowOn(c: Channel): Prog? = epg[c.id]?.firstOrNull { it.isNow() }

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

    /** Procura jogos no guia dos canais de esporte. */
    fun loadMatches(force: Boolean = false) {
        if (!force && (matchState == LoadState.LOADING || matchState == LoadState.READY)) return
        val src = source
        if (src !is XtreamSource) { matches = emptyList(); matchState = LoadState.READY; return }
        matchJob?.cancel()
        matchJob = scope.launch {
            matchState = LoadState.LOADING; matchProgress = 0f
            val list = footballChannels.distinctBy { it.id }.sortedBy { if (it.name.contains("futebol", true) || it.name.contains("premiere", true)) 0 else 1 }.take(90)
            val gate = Semaphore(6)
            var done = 0
            val found = HashMap<String, Match>()
            list.map { c ->
                async {
                    val progs = gate.withPermit { withContext(Dispatchers.IO) { runCatching { src.epg(c, 40) }.getOrNull() } }
                    done++; matchProgress = done.toFloat() / list.size
                    if (progs != null) {
                        if (!epg.containsKey(c.id)) { epg[c.id] = progs; epgTime[c.id] = System.currentTimeMillis() }
                        for (p in progs) {
                            if (p.end < System.currentTimeMillis() - 6 * 3600_000L) continue
                            val t = MatchLogic.parseTitle(p.title) ?: continue
                            val k = MatchLogic.key(t.home, t.away, p.start)
                            val m = found.getOrPut(k) { Match(t.home, t.away, t.comp, p.start, p.end, p.desc, ArrayList()) }
                            if (m.channels.none { it.id == c.id }) m.channels += c
                        }
                    }
                }
            }.awaitAll()
            matches = found.values.sortedWith(compareBy({ it.start }, { it.home }))
            matchState = LoadState.READY
        }
    }

    fun play(item: PlayItem) {
        if (item.kind == Kind.LIVE) item.channel?.let { store.addRecent(it.id) }
    }
}

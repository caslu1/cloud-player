package com.orbita.play

import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import kotlinx.coroutines.delay

class OrbitaApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Se o app fechar por erro, guarda o motivo para mostrar na próxima abertura
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { java.io.File(filesDir, "crash.txt").writeText(android.util.Log.getStackTraceString(e)) }
            prev?.uncaughtException(t, e)
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { Net.client }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        // pôsteres e logos ficam no aparelho: abrir de novo é instantâneo
        .diskCache { coil.disk.DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(250L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .allowRgb565(true)
        .crossfade(false)
        .build()
}

enum class Tab(val title: String) { CANAIS("CANAIS"), FILMES("FILMES"), SERIES("SÉRIES") }

/** Telas empilhadas por cima das abas. [returnKey] devolve o foco ao item que abriu a tela. */
sealed class Route(val returnKey: String?) {
    class MovieDetail(val movie: Movie, rk: String?) : Route(rk)
    class SeriesDetail(val series: Series, rk: String?) : Route(rk)
    class CategoryGrid(val movies: Boolean, val cat: Cat, rk: String?) : Route(rk)
    class Settings(rk: String?) : Route(rk)
    class Search(rk: String?) : Route(rk)
    class Playlists(rk: String?) : Route(rk)
    class AddPlaylist(rk: String?) : Route(rk)
}

/** Estado de navegação/telas que sobrevive enquanto o app está aberto. */
@androidx.compose.runtime.Stable
class Ui {
    var tab by mutableStateOf(Tab.CANAIS)
    /** Tela inicial (blocos) aberta; as seções abrem a partir dela. */
    var home by mutableStateOf(true)
    val stack = mutableStateListOf<Route>()
    var playerReturnKey: String? = null
    var liveCat by mutableStateOf("__all")
    /** Pedido de senha em aberto (controle parental). */
    var pinPrompt by mutableStateOf<PinPrompt?>(null)
    /** Janela do Recarregar: 0 fechada, 1 carregando, 2 concluído. */
    var reloading by mutableIntStateOf(0)

    /** Abre uma categoria adulta bloqueada depois da senha. */
    fun unlockAdult(vm: Vm, then: () -> Unit) {
        pinPrompt = PinPrompt("Conteúdo bloqueado", "Digite a senha para ver os canais adultos.") { pin ->
            if (pin == vm.store.pin()) { vm.adultUnlocked = true; then(); null } else "Senha incorreta"
        }
    }
    var query by mutableStateOf("")
    var libraryMenu by mutableIntStateOf(0)
    var liveCatShown: String? = null
    var movieCat by mutableStateOf<String?>(null)
    var seriesCat by mutableStateOf<String?>(null)
    var movieGrid = androidx.compose.foundation.lazy.grid.LazyGridState()
    var seriesGrid = androidx.compose.foundation.lazy.grid.LazyGridState()
    var movieCatList = androidx.compose.foundation.lazy.LazyListState()
    var seriesCatList = androidx.compose.foundation.lazy.LazyListState()
    var catList = androidx.compose.foundation.lazy.LazyListState()
    var chList = androidx.compose.foundation.lazy.LazyListState()
    var movieRows = androidx.compose.foundation.lazy.LazyListState()
    var seriesRows = androidx.compose.foundation.lazy.LazyListState()

    /** Abre uma seção a partir do início sempre do começo (categoria padrão, listas no topo). */
    fun openSection(t: Tab) {
        liveCat = "__all"; liveCatShown = null
        movieCat = null; seriesCat = null
        movieGrid = androidx.compose.foundation.lazy.grid.LazyGridState()
        seriesGrid = androidx.compose.foundation.lazy.grid.LazyGridState()
        movieCatList = androidx.compose.foundation.lazy.LazyListState()
        seriesCatList = androidx.compose.foundation.lazy.LazyListState()
        catList = androidx.compose.foundation.lazy.LazyListState()
        chList = androidx.compose.foundation.lazy.LazyListState()
        movieRows = androidx.compose.foundation.lazy.LazyListState()
        seriesRows = androidx.compose.foundation.lazy.LazyListState()
        FocusMem.restore = null
        tab = t; home = false
    }

    fun push(r: Route) { stack.add(r) }
    fun pop() {
        if (stack.isEmpty()) return
        val r = stack.removeAt(stack.size - 1)
        FocusMem.restore = r.returnKey
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var vm: Vm
    private lateinit var pb: Playback
    private val ui = Ui()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val crashFile = java.io.File(filesDir, "crash.txt")
        val crash = runCatching { crashFile.takeIf { it.exists() }?.readText() }.getOrNull()
        vm = Vm(this)
        pb = Playback(this, vm)
        vm.resume()
        vm.checkActivation()
        setContent {
            var lastCrash by remember { mutableStateOf(crash) }
            // vídeo de abertura só quando o app é aberto do zero (não nos testes)
            var splash by remember { mutableStateOf(savedInstanceState == null && android.os.Build.FINGERPRINT != "robolectric") }
            val c = lastCrash
            if (splash) VideoSplash { splash = false }
            else if (c != null) Scaled { CrashScreen(c) { crashFile.delete(); lastCrash = null } }
            else App(vm, pb, ui) { finish() }
        }
    }

    override fun onStop() {
        super.onStop()
        pb.saveProgress()
        pb.exo.pause()
    }

    override fun onStart() {
        super.onStart()
        val c = pb.current ?: return
        if (c.kind == Kind.LIVE) { pb.exo.seekToDefaultPosition(); pb.exo.prepare(); pb.exo.play() }
    }

    override fun onDestroy() {
        super.onDestroy()
        pb.release()
    }
}

/** Ajusta a escala para que o layout tenha sempre 960×540 dp (o design 1920×1080 ÷ 2), em qualquer TV ou TV box. */
@Composable
fun Scaled(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(C.bg)) {
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        val scale = minOf(w / 960f, h / 540f).coerceAtLeast(0.5f)
        // listas fixas em todo o app: só o quadro do foco anda; rola apenas ao chegar na borda
        // (sem isso, na Android TV a lista rola a cada seta para manter o item no meio)
        CompositionLocalProvider(LocalDensity provides Density(scale, 1f)) { TvScroll { content() } }
    }
}

@Composable
fun App(vm: Vm, pb: Playback, ui: Ui, exit: () -> Unit) {
    Scaled {
        Box(Modifier.fillMaxSize().background(C.screen)) {
            when {
                vm.act?.locked == true -> ActivationScreen(vm)
                vm.account == null -> LoginScreen(vm)
                pb.fullscreen && pb.current != null -> FullPlayer(vm, pb)
                else -> Shell(vm, pb, ui, exit)
            }
            // aviso de primeiro acesso e o teclado do app ficam por cima de tudo
            if (!vm.store.disclaimerSeen.value) Disclaimer { vm.store.acceptDisclaimer() }
            TvKeyboard()
        }
    }
    // revalida a ativação de tempos em tempos; vencido/bloqueado para o vídeo
    LaunchedEffect(Unit) { while (true) { delay(10 * 60_000L); vm.checkActivation() } }
    LaunchedEffect(vm.act?.locked) { if (vm.act?.locked == true) { pb.stop(); pb.fullscreen = false } }
    // Volta o foco para quem abriu o player
    LaunchedEffect(pb.fullscreen) {
        if (pb.fullscreen) ui.playerReturnKey = FocusMem.last
        else if (ui.playerReturnKey != null) { FocusMem.restore = ui.playerReturnKey; ui.playerReturnKey = null }
    }
}

/** Abre um item no player em tela cheia. */
fun Playback.openFull(item: PlayItem, items: List<PlayItem> = emptyList(), startMs: Long = 0L) {
    if (current?.key != item.key) play(item, items, startMs)
    else if (startMs > 0 && item.kind != Kind.LIVE) exo.seekTo(startMs)
    fullscreen = true
}

@Composable
private fun Shell(vm: Vm, pb: Playback, ui: Ui, exit: () -> Unit) {
    var toast by remember { mutableStateOf<String?>(null) }
    var exitAsk by remember { mutableStateOf(false) }
    val top = ui.stack.lastOrNull()

    BackHandler {
        when {
            ui.stack.isNotEmpty() -> ui.pop()
            !ui.home -> { FocusMem.restore = "home:${ui.tab.name}"; ui.home = true }
            else -> exitAsk = true
        }
    }
    val openSearch = { ui.push(Route.Search(FocusMem.last)) }
    val openSettings = { ui.push(Route.Settings(FocusMem.last)) }
    val reload = { vm.reloadAll(); ui.reloading = 1 }
    // a prévia de canal não continua tocando por trás da tela inicial
    LaunchedEffect(ui.home) { if (ui.home && !pb.fullscreen && pb.current?.kind == Kind.LIVE) pb.stop() }
    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }
    // devolve o foco ao item que abriu o pedido de senha
    var pinReturn by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(ui.pinPrompt) {
        if (ui.pinPrompt != null) { if (pinReturn == null) pinReturn = FocusMem.last }
        else if (pinReturn != null) { FocusMem.restoreTo(pinReturn!!); pinReturn = null }
    }

    Box(Modifier.fillMaxSize()) {
        when (top) {
            is Route.MovieDetail -> MovieDetail(vm, pb, ui, top.movie)
            is Route.SeriesDetail -> SeriesDetail(vm, pb, ui, top.series)
            is Route.CategoryGrid -> CategoryGrid(vm, pb, ui, top)
            is Route.Settings -> SettingsScreen(vm, pb, ui)
            is Route.Search -> SearchScreen(vm, pb, ui)
            is Route.Playlists -> PlaylistsScreen(vm, ui)
            is Route.AddPlaylist -> LoginScreen(vm, adding = true) { ui.stack.clear(); ui.home = true }
            null -> if (ui.home) {
                when (vm.store.homeLayout.value) {
                    "lite" -> HomeLite(vm, pb, ui, openSearch, reload, openSettings)
                    "cards" -> HomeCards(vm, pb, ui, openSearch, reload, openSettings)
                    "round" -> HomeCards(vm, pb, ui, openSearch, reload, openSettings, style = "round")
                    "square" -> HomeCards(vm, pb, ui, openSearch, reload, openSettings, style = "square")
                    else -> HomeScreen(vm, pb, ui, openSearch, reload, openSettings)
                }
            } else {
                when (ui.tab) {
                    Tab.CANAIS -> ChannelsScreen(vm, pb, ui)
                    Tab.FILMES -> MoviesScreen(vm, pb, ui)
                    Tab.SERIES -> SeriesScreen(vm, pb, ui)
                }
            }
        }
        ui.pinPrompt?.let { p -> PinDialog(p) { if (ui.pinPrompt === p) ui.pinPrompt = null } }
        ReloadDialog(vm, ui)
        // aviso de atualização: só na tela inicial
        UpdateCheckOnStart()
        if (ui.home && ui.stack.isEmpty()) UpdateDialog()
        if (exitAsk) ConfirmDialog("Sair do Orbita Play?", "O que estiver tocando será fechado.", "Sair", onConfirm = { pb.stop(); exit() }, onCancel = { exitAsk = false })
        toast?.let {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).background(Color(0xEE15121F), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Txt(it, F.s(9.sp, C.white, FontWeight.Medium))
            }
        }
    }
}

/** Fundo com gradiente roxo do design. */
@Composable
fun ScreenBackground() = WallpaperView(Wall.index)

@Composable
fun BrandSplash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Logo(Modifier.wrapContentSize(), 30f)
        }
    }
}

/** Mostra o erro da última vez que o app fechou sozinho (para enviar ao suporte). */
@Composable
fun CrashScreen(text: String, onContinue: () -> Unit) {
    val r = remember { FocusRequester() }
    InitialFocus(r)
    Column(Modifier.fillMaxSize().background(C.bg).padding(24.dp)) {
        Txt("O app fechou inesperadamente da última vez", F.s(14.sp, C.white, FontWeight.Bold, F.outfit))
        Spacer(Modifier.height(4.dp))
        Txt("Tire uma foto desta tela e envie para o suporte. Depois pressione OK para continuar.", F.s(9.sp, C.dim), maxLines = 2)
        Spacer(Modifier.height(8.dp))
        ActionButton("Continuar", primary = true, requester = r) { onContinue() }
        Spacer(Modifier.height(8.dp))
        Txt(text.take(2500), F.s(7.sp, C.text), maxLines = 40)
    }
}

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

enum class Tab(val label: String) { CANAIS("Canais"), FILMES("Filmes"), SERIES("Séries"), FUTEBOL("Futebol"), BUSCAR("Buscar"), BIBLIOTECA("Biblioteca") }

/** Telas empilhadas por cima das abas. [returnKey] devolve o foco ao item que abriu a tela. */
sealed class Route(val returnKey: String?) {
    class MovieDetail(val movie: Movie, rk: String?) : Route(rk)
    class SeriesDetail(val series: Series, rk: String?) : Route(rk)
    class CategoryGrid(val movies: Boolean, val cat: Cat, rk: String?) : Route(rk)
    class Settings(rk: String?) : Route(rk)
}

/** Estado de navegação/telas que sobrevive enquanto o app está aberto. */
class Ui {
    var tab by mutableStateOf(Tab.CANAIS)
    val stack = mutableStateListOf<Route>()
    var playerReturnKey: String? = null
    var liveCat by mutableStateOf("__all")
    var footballMenu by mutableIntStateOf(0)
    var footballDay by mutableIntStateOf(0)
    var query by mutableStateOf("")
    var libraryMenu by mutableIntStateOf(0)
    var liveCatShown: String? = null
    var movieCat by mutableStateOf<String?>(null)
    var seriesCat by mutableStateOf<String?>(null)
    val movieGrid = androidx.compose.foundation.lazy.grid.LazyGridState()
    val seriesGrid = androidx.compose.foundation.lazy.grid.LazyGridState()
    val movieCatList = androidx.compose.foundation.lazy.LazyListState()
    val seriesCatList = androidx.compose.foundation.lazy.LazyListState()
    val catList = androidx.compose.foundation.lazy.LazyListState()
    val chList = androidx.compose.foundation.lazy.LazyListState()
    val movieRows = androidx.compose.foundation.lazy.LazyListState()
    val seriesRows = androidx.compose.foundation.lazy.LazyListState()
    val matchList = androidx.compose.foundation.lazy.LazyListState()

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
        setContent {
            var lastCrash by remember { mutableStateOf(crash) }
            val c = lastCrash
            if (c != null) Scaled { CrashScreen(c) { crashFile.delete(); lastCrash = null } }
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
        CompositionLocalProvider(LocalDensity provides Density(scale, 1f)) { content() }
    }
}

@Composable
fun App(vm: Vm, pb: Playback, ui: Ui, exit: () -> Unit) {
    Scaled {
        Box(Modifier.fillMaxSize().background(C.screen)) {
            when {
                vm.account == null -> LoginScreen(vm)
                pb.fullscreen && pb.current != null -> FullPlayer(vm, pb)
                else -> Shell(vm, pb, ui, exit)
            }
        }
    }
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
    var lastBack by remember { mutableLongStateOf(0L) }
    var toast by remember { mutableStateOf<String?>(null) }
    val top = ui.stack.lastOrNull()
    val tabReq = remember { FocusRequester() }

    BackHandler {
        when {
            ui.stack.isNotEmpty() -> ui.pop()
            FocusMem.last?.startsWith("tab:") != true -> { FocusMem.restore = "tab:${ui.tab.name}"; runCatching { tabReq.requestFocus() } }
            System.currentTimeMillis() - lastBack < 2500 -> { pb.stop(); exit() }
            else -> { lastBack = System.currentTimeMillis(); toast = "Pressione VOLTAR de novo para sair" }
        }
    }
    LaunchedEffect(toast) { if (toast != null) { delay(2500); toast = null } }

    Box(Modifier.fillMaxSize()) {
        when (top) {
            is Route.MovieDetail -> MovieDetail(vm, pb, ui, top.movie)
            is Route.SeriesDetail -> SeriesDetail(vm, pb, ui, top.series)
            is Route.CategoryGrid -> CategoryGrid(vm, pb, ui, top)
            is Route.Settings -> SettingsScreen(vm, pb, ui)
            null -> {
                when (ui.tab) {
                    Tab.CANAIS -> ChannelsScreen(vm, pb, ui)
                    Tab.FILMES -> MoviesScreen(vm, pb, ui)
                    Tab.SERIES -> SeriesScreen(vm, pb, ui)
                    Tab.FUTEBOL -> FootballScreen(vm, pb, ui)
                    Tab.BUSCAR -> SearchScreen(vm, pb, ui)
                    Tab.BIBLIOTECA -> LibraryScreen(vm, pb, ui)
                }
                TopBar(ui, tabReq) { ui.push(Route.Settings(FocusMem.last)) }
            }
        }
        toast?.let {
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).background(Color(0xEE15121F), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Txt(it, F.s(9.sp, C.white, FontWeight.Medium))
            }
        }
    }
}

@Composable
fun TopBar(ui: Ui, tabReq: FocusRequester, onSettings: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(10_000) } }
    Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 29.dp), verticalAlignment = Alignment.CenterVertically) {
        Logo()
        Spacer(Modifier.width(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (t in Tab.entries) {
                Pill(
                    t.label, ui.tab == t, Modifier.padding(end = 3.dp),
                    requester = if (ui.tab == t) tabReq else null,
                    onFocus = { FocusMem.last = "tab:${t.name}" }, // só troca de aba com OK
                    onClick = { ui.tab = t }
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Focus(onClick = onSettings, memKey = "settings") { f ->
            Box(
                Modifier.size(22.dp).background(if (f) C.accent else Color.Transparent, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Ic.Settings, 14.dp, desc = "Configurações") }
        }
        Spacer(Modifier.width(10.dp))
        Txt(hhmm(now), F.s(11.sp, C.white, FontWeight.Bold))
    }
}

/** Fundo com gradiente roxo do design. */
@Composable
fun ScreenBackground() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF1F1150), Color(0xFF0A0716), Color(0xFF040308)), radius = 1400f, center = androidx.compose.ui.geometry.Offset(200f, 0f))
        )
    )
}

@Composable
fun BrandSplash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.logo_mark), null, Modifier.size(60.dp))
            Spacer(Modifier.height(10.dp))
            Logo(Modifier.wrapContentSize(), 16f)
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

package com.orbita.play

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed as listItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Locale

/** Um item mostrável em linhas e grades (filme ou série). */
class VodEntry(
    val key: String, val title: String, val image: String?, val rating: Float, val open: () -> Unit, val longPress: () -> Unit,
    val movie: Movie? = null, val series: Series? = null,
)

class VodRow(val key: String, val title: String, val entries: List<VodEntry>, val wide: List<Progress>? = null, val more: (() -> Unit)? = null)

fun movieItem(m: Movie, sub: String = "") = PlayItem(Kind.MOVIE, "mv:${m.id}", m.name, sub, m.url, m.poster)

fun openTrailer(ctx: Context, trailer: String?, title: String) {
    val uri = when {
        trailer.isNullOrBlank() -> Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode("$title trailer"))
        trailer.startsWith("http") -> Uri.parse(trailer)
        else -> Uri.parse("https://www.youtube.com/watch?v=$trailer")
    }
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Instale o app do YouTube para ver o trailer.", Toast.LENGTH_LONG).show()
    }
}

fun stars(r: Float) = String.format(Locale("pt", "BR"), "★ %.1f", r)

/** Rolagem estilo TV: o item com foco fica sempre perto do início (a linha/pôster não "pula" pela tela). */
@OptIn(ExperimentalFoundationApi::class)
private val tvPivot = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val target = containerSize * 0.035f
        return offset - target
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvScroll(content: @Composable () -> Unit) = CompositionLocalProvider(LocalBringIntoViewSpec provides tvPivot) { content() }

/** Fundo com a imagem do destaque à direita, escurecida para o texto ficar legível. */
@Composable
fun Backdrop(image: String?, seed: String) {
    Box(Modifier.fillMaxSize().background(C.bg)) {
        val c = C.hash(seed)
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF120E2E), c.copy(alpha = 0.45f), Color(0xFF1A0F2A)))))
        if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.9f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0xF50B0A1C), 0.42f to Color(0xB00B0A1C), 0.75f to Color(0x330B0A1C), 1f to Color(0x100B0A1C))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x990B0A1C), 0.2f to Color.Transparent, 0.45f to Color(0x550B0A1C), 0.62f to Color(0xEE0B0A1C), 1f to C.bg)))
    }
}

/** Título: logo do TMDB quando existe, senão o nome em letras grandes. */
@Composable
fun TitleArt(title: String, logo: String?, big: Boolean = true) {
    if (logo != null) {
        AsyncImage(
            logo, title,
            Modifier.widthIn(max = if (big) 330.dp else 230.dp).heightIn(max = if (big) 92.dp else 52.dp),
            contentScale = ContentScale.Fit, alignment = Alignment.CenterStart
        )
    } else Txt(title, F.s(if (big) 30.sp else 20.sp, C.white, FontWeight.Bold, FontFamily.Serif), maxLines = 2)
}

/** Dados do destaque (filme ou série), já juntando provedor + TMDB. */
class HeroData(
    val key: String, val label: String, val title: String, val logo: String?, val backdrop: String?, val chips: List<String>,
    val rating: Float, val genre: String?, val plot: String?,
)

class HeroSel {
    var key by mutableStateOf<String?>(null)
    var row by mutableIntStateOf(0)
}

@Composable
fun HeroBlock(h: HeroData, compact: Boolean, buttons: @Composable () -> Unit) {
    Column(Modifier.padding(start = 28.dp, top = if (compact) 60.dp else 62.dp).width(460.dp)) {
        Txt(h.label, F.s(8.sp, C.cyan, FontWeight.SemiBold, spacing = 2.sp))
        Spacer(Modifier.height(6.dp))
        TitleArt(h.title, h.logo, big = !compact)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            h.chips.forEach { MetaChip(it) }
            if (h.rating > 0) MetaChip(stars(h.rating), C.star, Color(0x33FBBF24))
            h.genre?.let { Txt(it, F.s(8.5.sp, C.dim), Modifier.padding(start = 2.dp)) }
        }
        if (!compact) {
            Spacer(Modifier.height(8.dp))
            Txt(h.plot ?: "", F.s(9.5.sp, C.text.copy(alpha = 0.92f)), maxLines = 3, modifier = Modifier.height(46.dp))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { buttons() }
        }
    }
}

/** Pôster com escala suave no foco (sem recompor a tela inteira). */
@Composable
fun BigPoster(
    e: VodEntry, image: String?, width: Dp, memKey: String, requester: FocusRequester? = null, onFocus: () -> Unit,
) {
    Focus(Modifier.width(width), onClick = e.open, onFocus = onFocus, onLongClick = e.longPress, requester = requester, memKey = memKey) { f ->
        val sc by animateFloatAsState(if (f) 1.07f else 1f, tween(140), label = "s")
        val shape = RoundedCornerShape(9.dp)
        Column {
            Box(
                Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.width(width).height(width * 1.5f).clip(shape)
                    .then(if (f) Modifier.border(2.5.dp, C.white, shape) else Modifier)
            ) {
                GeneratedArt(e.title, Modifier.fillMaxSize(), big = true)
                if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (e.rating > 0f) {
                    Box(Modifier.align(Alignment.TopEnd).padding(5.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xCC000000)).padding(horizontal = 4.dp, vertical = 1.5.dp)) {
                        Txt(String.format(Locale.US, "★ %.1f", e.rating), F.s(7.sp, C.star, FontWeight.Bold))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Txt(e.title, F.s(8.5.sp, if (f) C.white else C.dim, if (f) FontWeight.SemiBold else FontWeight.Normal), Modifier.width(width))
        }
    }
}

@Composable
fun ContinueCard(p: Progress, image: String?, memKey: String, onFocus: () -> Unit, onLong: () -> Unit, onClick: () -> Unit) {
    Focus(Modifier.width(214.dp), onClick = onClick, onFocus = onFocus, onLongClick = onLong, memKey = memKey) { f ->
        val sc by animateFloatAsState(if (f) 1.05f else 1f, tween(140), label = "s")
        Column {
            val shape = RoundedCornerShape(9.dp)
            Box(
                Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.fillMaxWidth().height(120.dp).clip(shape)
                    .then(if (f) Modifier.border(2.5.dp, C.white, shape) else Modifier)
            ) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0E1530), C.hash(p.title)))))
                if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000)))))
                Txt(p.title, F.s(12.sp, C.white, FontWeight.Bold, F.outfit), Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 12.dp, end = 10.dp))
                ProgressBar(p.fraction, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp), C.accent, Color(0x55FFFFFF))
            }
            Spacer(Modifier.height(6.dp))
            Txt(
                if (p.kind == Kind.EPISODE) "${p.subtitle} · faltam ${p.remainingMin} min" else "Faltam ${p.remainingMin} min",
                F.s(8.sp, C.dim)
            )
        }
    }
}

/** Linhas de cartões. Lê o TMDB só dentro de cada item, então chegar uma capa não redesenha a tela toda. */
@Composable
fun VodRows(
    vm: Vm, rows: List<VodRow>, state: LazyListState, modifier: Modifier, sel: HeroSel,
    firstReq: FocusRequester?, wideImage: (Progress) -> String?, onWide: (Progress) -> Unit, onWideLong: (Progress) -> Unit,
) {
    TvScroll {
        LazyColumn(state = state, modifier = modifier, contentPadding = PaddingValues(bottom = 120.dp)) {
            listItemsIndexed(rows, key = { _, r -> r.key }) { rowIdx, row ->
                Column(Modifier.padding(top = 8.dp)) {
                    Txt(row.title, F.s(11.5.sp, C.white, FontWeight.SemiBold), Modifier.padding(start = 28.dp, bottom = 8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 28.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val wide = row.wide
                        if (wide != null) {
                            items(wide, key = { it.key }) { p ->
                                ContinueCard(p, wideImage(p), "${row.key}:${p.key}", onFocus = { sel.key = p.key; sel.row = rowIdx }, onLong = { onWideLong(p) }) { onWide(p) }
                            }
                        } else {
                            listItemsIndexed(row.entries, key = { _, e -> e.key }) { i, e ->
                                val m = e.movie; val s = e.series
                                LaunchedEffect(e.key) { delay(200); m?.let { vm.requestMeta(it) }; s?.let { vm.requestMeta(it) } }
                                // capa do TMDB quando encontrada; senão a do provedor
                                val img = (m?.let { vm.metaOf(it) } ?: s?.let { vm.metaOf(it) })?.posterUrl() ?: e.image
                                BigPoster(
                                    e, img, 112.dp, "${row.key}:${e.key}", if (rowIdx == 0 && i == 0) firstReq else null,
                                    onFocus = { sel.key = e.key; sel.row = rowIdx }
                                )
                            }
                            val more = row.more
                            if (more != null) item(key = "more") {
                                Focus(Modifier.width(112.dp), onClick = more, memKey = "${row.key}:more") { f ->
                                    Box(
                                        Modifier.width(112.dp).height(168.dp).clip(RoundedCornerShape(9.dp)).background(if (f) C.accent else C.item),
                                        contentAlignment = Alignment.Center
                                    ) { Txt("Ver todos ›", F.s(9.5.sp, C.white, FontWeight.SemiBold)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StateRetry(msg: String, retry: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        EmptyState(msg)
        val r = remember { FocusRequester() }
        ActionButton("Tentar de novo", primary = true, requester = r) { retry() }
        InitialFocus(r)
    }
}

// ---------------------------------------------------------------- Filmes

@Composable
fun MoviesScreen(vm: Vm, pb: Playback, ui: Ui) {
    LaunchedEffect(Unit) { if (vm.movieState == LoadState.IDLE || vm.movieState == LoadState.ERROR) vm.loadMovies() }
    if (vm.movieState != LoadState.READY) {
        ScreenBackground()
        if (vm.movieState == LoadState.ERROR) StateRetry("Não foi possível carregar os filmes.") { vm.loadMovies() }
        else Loading("Carregando filmes…", Modifier.fillMaxSize())
        return
    }
    if (vm.movies.isEmpty()) { ScreenBackground(); EmptyState("Sua lista não tem filmes", "", Modifier.fillMaxSize()); return }

    val latest = remember(vm.movies) { vm.movies.sortedByDescending { it.added }.take(80) }
    val cont = vm.store.continueWatching(Kind.MOVIE).mapNotNull { vm.movieById[it.key.removePrefix("mv:")] }
    val mine = vm.store.myList.filter { it.startsWith("m:") }.mapNotNull { vm.movieById[it.removePrefix("m:")] }
    fun entry(m: Movie) = VodEntry(m.id, m.name, m.poster, m.rating, { ui.push(Route.MovieDetail(m, FocusMem.last)) }, { vm.store.toggleList("m:${m.id}") }, movie = m)

    val cats = remember(vm.movieCats, cont.size, mine.size) {
        buildList {
            if (cont.isNotEmpty()) add(BrowseCat("__cont", "▶  Continuar assistindo", cont.size))
            add(BrowseCat("__new", "✦  Lançamentos", latest.size))
            if (mine.isNotEmpty()) add(BrowseCat("__mine", "＋  Minha lista", mine.size))
            vm.movieCats.forEach { add(BrowseCat(it.id, it.name, vm.moviesByCat[it.id]?.size ?: 0)) }
        }
    }
    VodBrowse(
        vm, cats, ui.movieCat, { ui.movieCat = it }, ui.movieCatList, ui.movieGrid,
        entriesOf = { id ->
            when (id) {
                "__cont" -> cont.map(::entry)
                "__new" -> latest.map(::entry)
                "__mine" -> mine.map(::entry)
                else -> vm.moviesByCat[id].orEmpty().map(::entry)
            }
        },
        hero = { e ->
            val m = e?.movie ?: latest.first()
            LaunchedEffect(m.id) { vm.requestMeta(m); vm.requestMovieInfo(m) }
            movieHero(vm, m, "FILME")
        },
        empty = "Nenhum filme nesta categoria",
    )
}

private fun movieHero(vm: Vm, m: Movie, label: String): HeroData {
    val t = vm.metaOf(m)
    val info = vm.movieInfos[m.id]
    val dur = info?.durationSec?.takeIf { it > 0 } ?: t?.runtimeMin?.takeIf { it > 0 }?.times(60)
    return HeroData(
        m.id, label, t?.title ?: Tmdb.clean(m.name).first, t?.logoUrl(), t?.backdropUrl() ?: info?.backdrop ?: m.poster,
        listOfNotNull(t?.year ?: info?.year, dur?.let(::fmtDuration), info?.age),
        t?.rating?.takeIf { it > 0 } ?: info?.rating?.takeIf { it > 0 } ?: m.rating,
        t?.genres ?: info?.genre, t?.overview ?: info?.plot,
    )
}

@Composable
private fun MovieButtons(vm: Vm, pb: Playback, ctx: Context, m: Movie, req: FocusRequester) {
    val prog = vm.store.progress["mv:${m.id}"]?.takeIf { !it.finished && it.pos > 30_000 }
    ActionButton(if (prog != null) "Continuar" else "Assistir", primary = true, icon = "▶", requester = req) {
        pb.openFull(movieItem(m, vm.metaOf(m)?.genres ?: vm.movieInfos[m.id]?.genre.orEmpty()), startMs = prog?.pos ?: 0L)
    }
    ActionButton(if (vm.store.inList("m:${m.id}")) "✓ Na minha lista" else "+ Minha lista") { vm.store.toggleList("m:${m.id}") }
    ActionButton("Trailer") { openTrailer(ctx, vm.metaOf(m)?.trailer ?: vm.movieInfos[m.id]?.trailer, m.name) }
}

@Composable
fun MovieDetail(vm: Vm, pb: Playback, ui: Ui, m: Movie) {
    val ctx = LocalContext.current
    LaunchedEffect(m.id) { vm.requestMovieInfo(m); vm.requestMeta(m) }
    val info = vm.movieInfos[m.id]
    val t = vm.metaOf(m)
    val h = movieHero(vm, m, "FILME")
    val prog = vm.store.progress["mv:${m.id}"]?.takeIf { !it.finished && it.pos > 30_000 }
    val playReq = remember { FocusRequester() }
    InitialFocus(playReq)
    Box(Modifier.fillMaxSize()) {
        Backdrop(h.backdrop, m.name)
        // capa e informações juntas, centralizadas na tela (sem espaço vazio no meio em telas largas)
        Row(
            Modifier.align(Alignment.Center).widthIn(max = 900.dp).fillMaxWidth().padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(220.dp).height(330.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))) {
                GeneratedArt(h.title, Modifier.fillMaxSize(), big = true)
                (t?.posterUrl("w500") ?: m.poster)?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Spacer(Modifier.width(34.dp))
            Column(Modifier.weight(1f)) {
                Txt("FILME", F.s(9.sp, C.cyan, FontWeight.SemiBold, spacing = 2.sp))
                Spacer(Modifier.height(8.dp))
                if (h.logo != null) {
                    AsyncImage(
                        h.logo, h.title, Modifier.widthIn(max = 380.dp).heightIn(max = 110.dp),
                        contentScale = ContentScale.Fit, alignment = Alignment.CenterStart
                    )
                } else Txt(h.title, F.s(30.sp, C.white, FontWeight.Bold, FontFamily.Serif), maxLines = 2)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    h.chips.forEach { BigChip(it) }
                    if (h.rating > 0) BigChip(stars(h.rating), C.star, Color(0x33FBBF24))
                }
                h.genre?.let { Spacer(Modifier.height(8.dp)); Txt(it, F.s(11.sp, C.dim, FontWeight.Medium)) }
                Spacer(Modifier.height(12.dp))
                Txt(h.plot ?: if (info == null && t == null) "Carregando informações…" else "", F.s(12.sp, C.text), maxLines = 6)
                info?.cast?.let { Spacer(Modifier.height(10.dp)); Txt("Elenco: $it", F.s(10.sp, C.dim), maxLines = 2) }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(if (prog != null) "Continuar ${fmtTime(prog.pos)}" else "Assistir", primary = true, icon = "▶", requester = playReq) {
                        pb.openFull(movieItem(m, h.genre.orEmpty()), startMs = prog?.pos ?: 0L)
                    }
                    if (prog != null) ActionButton("Do início") { pb.openFull(movieItem(m, h.genre.orEmpty()), startMs = 1L); pb.exo.seekTo(0) }
                    ActionButton(if (vm.store.inList("m:${m.id}")) "✓ Na minha lista" else "+ Minha lista") { vm.store.toggleList("m:${m.id}") }
                    ActionButton("Trailer") { openTrailer(ctx, t?.trailer ?: info?.trailer, m.name) }
                }
                if (prog != null) {
                    Spacer(Modifier.height(14.dp))
                    ProgressBar(prog.fraction, Modifier.width(260.dp).height(4.dp).clip(RoundedCornerShape(2.dp)), C.accent)
                    Txt("Faltam ${prog.remainingMin} min", F.s(9.5.sp, C.dim), Modifier.padding(top = 5.dp))
                }
            }
        }
        HintBar(listOf("OK" to "Selecionar", "VOLTAR" to "Filmes"), Modifier.align(Alignment.BottomStart).padding(bottom = 14.dp))
    }
}

@Composable
private fun BigChip(text: String, color: Color = C.white, bg: Color = Color(0x33FFFFFF)) {
    Box(Modifier.clip(RoundedCornerShape(5.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.5.dp)) {
        Txt(text, F.s(9.5.sp, color, FontWeight.Bold))
    }
}

@Composable
fun CategoryGrid(vm: Vm, pb: Playback, ui: Ui, r: Route.CategoryGrid) {
    val entries: List<VodEntry> = remember(r.cat.id, r.movies) {
        if (r.movies) vm.moviesByCat[r.cat.id].orEmpty().map { m ->
            VodEntry(m.id, m.name, m.poster, m.rating, { ui.push(Route.MovieDetail(m, FocusMem.last)) }, { vm.store.toggleList("m:${m.id}") }, movie = m)
        } else vm.seriesByCat[r.cat.id].orEmpty().map { s ->
            VodEntry(s.id, s.name, s.cover, s.rating, { ui.push(Route.SeriesDetail(s, FocusMem.last)) }, { vm.store.toggleList("s:${s.id}") }, series = s)
        }
    }
    val first = remember { FocusRequester() }
    InitialFocus(first)
    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.fillMaxSize().padding(top = 22.dp)) {
            Row(Modifier.padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(r.cat.name, F.s(17.sp, C.white, FontWeight.Bold, F.outfit), Modifier.weight(1f))
                Txt("${entries.size} títulos", F.s(9.sp, C.dim))
            }
            Spacer(Modifier.height(10.dp))
            TvScroll {
                LazyVerticalGrid(
                    GridCells.Fixed(7), Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 28.dp, end = 28.dp, bottom = 40.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(entries, key = { _, e -> e.key }) { i, e ->
                        LaunchedEffect(e.key) { delay(200); e.movie?.let { vm.requestMeta(it) }; e.series?.let { vm.requestMeta(it) } }
                        val img = (e.movie?.let { vm.metaOf(it) } ?: e.series?.let { vm.metaOf(it) })?.posterUrl() ?: e.image
                        BigPoster(e, img, 108.dp, "grid:${e.key}", if (i == 0) first else null, onFocus = {})
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Séries

@Composable
fun SeriesScreen(vm: Vm, pb: Playback, ui: Ui) {
    LaunchedEffect(Unit) { if (vm.seriesState == LoadState.IDLE || vm.seriesState == LoadState.ERROR) vm.loadSeries() }
    if (vm.seriesState != LoadState.READY) {
        ScreenBackground()
        if (vm.seriesState == LoadState.ERROR) StateRetry("Não foi possível carregar as séries.") { vm.loadSeries() }
        else Loading("Carregando séries…", Modifier.fillMaxSize())
        return
    }
    if (vm.series.isEmpty()) { ScreenBackground(); EmptyState("Sua lista não tem séries", "", Modifier.fillMaxSize()); return }

    val latest = remember(vm.series) { vm.series.sortedByDescending { it.added }.take(80) }
    val cont = vm.store.continueWatching(Kind.EPISODE).mapNotNull { p -> p.seriesId?.let { vm.seriesById[it] } }.distinctBy { it.id }
    val mine = vm.store.myList.filter { it.startsWith("s:") }.mapNotNull { vm.seriesById[it.removePrefix("s:")] }
    fun entry(s: Series) = VodEntry(s.id, s.name, s.cover, s.rating, { ui.push(Route.SeriesDetail(s, FocusMem.last)) }, { vm.store.toggleList("s:${s.id}") }, series = s)

    val cats = remember(vm.seriesCats, cont.size, mine.size) {
        buildList {
            if (cont.isNotEmpty()) add(BrowseCat("__cont", "▶  Continuar assistindo", cont.size))
            add(BrowseCat("__new", "✦  Novidades", latest.size))
            if (mine.isNotEmpty()) add(BrowseCat("__mine", "＋  Minha lista", mine.size))
            vm.seriesCats.forEach { add(BrowseCat(it.id, it.name, vm.seriesByCat[it.id]?.size ?: 0)) }
        }
    }
    VodBrowse(
        vm, cats, ui.seriesCat, { ui.seriesCat = it }, ui.seriesCatList, ui.seriesGrid,
        entriesOf = { id ->
            when (id) {
                "__cont" -> cont.map(::entry)
                "__new" -> latest.map(::entry)
                "__mine" -> mine.map(::entry)
                else -> vm.seriesByCat[id].orEmpty().map(::entry)
            }
        },
        hero = { e ->
            val s = e?.series ?: latest.first()
            LaunchedEffect(s.id) { vm.requestMeta(s) }
            seriesHero(vm, s, "SÉRIE")
        },
        empty = "Nenhuma série nesta categoria",
    )
}

fun seriesHero(vm: Vm, s: Series, label: String, info: SeriesInfo? = null): HeroData {
    val t = vm.metaOf(s)
    val seasons = info?.seasons?.size ?: t?.seasons ?: 0
    return HeroData(
        s.id, if (seasons > 0) "$label · $seasons TEMPORADA${if (seasons > 1) "S" else ""}" else label, t?.title ?: Tmdb.clean(s.name).first, t?.logoUrl(),
        t?.backdropUrl() ?: info?.backdrop ?: s.backdrop ?: s.cover,
        listOfNotNull(t?.year ?: info?.year ?: s.year),
        t?.rating?.takeIf { it > 0 } ?: info?.rating?.takeIf { it > 0 } ?: s.rating,
        t?.genres ?: info?.genre ?: s.genre, t?.overview ?: info?.plot ?: s.plot,
    )
}

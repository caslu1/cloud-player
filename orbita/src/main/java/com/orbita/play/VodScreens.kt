package com.orbita.play

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Locale

/** Um item mostrável em linhas e grades (filme ou série). */
class VodEntry(val key: String, val title: String, val image: String?, val rating: Float, val open: () -> Unit, val longPress: () -> Unit)

class VodRow(val key: String, val title: String, val entries: List<VodEntry>, val wide: List<Progress>? = null, val more: (() -> Unit)? = null)

class Hero(
    val key: String, val title: String, val backdrop: String?, val chips: List<String>, val rating: Float, val genre: String?,
    val plot: String?, val label: String,
)

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

private fun stars(r: Float) = String.format(Locale("pt", "BR"), "★ %.1f", r)

/** Fundo com a imagem do destaque à direita, escurecida para o texto ficar legível. */
@Composable
fun Backdrop(image: String?, seed: String) {
    Box(Modifier.fillMaxSize().background(C.bg)) {
        val c = C.hash(seed)
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF120E2E), c.copy(alpha = 0.55f), Color(0xFF1A0F2A)))))
        if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.85f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0xF20B0A1C), 0.45f to Color(0xB30B0A1C), 1f to Color(0x220B0A1C))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x880B0A1C), 0.25f to Color.Transparent, 0.5f to Color(0x660B0A1C), 0.62f to Color(0xF00B0A1C), 1f to C.bg)))
    }
}

@Composable
fun HeroBlock(h: Hero, buttons: @Composable () -> Unit) {
    Column(Modifier.padding(start = 24.dp, top = 58.dp).width(430.dp)) {
        Txt(h.label, F.s(8.sp, C.cyan, FontWeight.SemiBold, spacing = 2.sp))
        Spacer(Modifier.height(4.dp))
        Txt(h.title, F.s(30.sp, C.white, FontWeight.Bold, FontFamily.Serif), maxLines = 2)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            h.chips.forEach { MetaChip(it) }
            if (h.rating > 0) MetaChip(stars(h.rating), C.star, Color(0x33FBBF24))
            h.genre?.let { Txt(it, F.s(8.5.sp, C.dim), Modifier.padding(start = 2.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Txt(h.plot ?: "", F.s(9.5.sp, C.text.copy(alpha = 0.92f)), maxLines = 3, modifier = Modifier.height(46.dp))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { buttons() }
    }
}

/** Linhas de cartões (Continuar assistindo, Lançamentos, categorias...). */
@Composable
fun VodRows(rows: List<VodRow>, state: LazyListState, onFocusEntry: (String) -> Unit, onWide: (Progress) -> Unit, onWideLong: (Progress) -> Unit, modifier: Modifier) {
    LazyColumn(state = state, modifier = modifier, contentPadding = PaddingValues(bottom = 40.dp)) {
        items(rows, key = { it.key }) { row ->
            Column(Modifier.padding(top = 10.dp)) {
                Txt(row.title, F.s(11.sp, C.white, FontWeight.SemiBold), Modifier.padding(start = 24.dp, bottom = 7.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    val wide = row.wide
                    if (wide != null) {
                        items(wide, key = { it.key }) { p ->
                            WideCard(
                                p.title, p.image, p.fraction,
                                if (p.kind == Kind.EPISODE) "${p.subtitle} · faltam ${p.remainingMin} min" else "Faltam ${p.remainingMin} min",
                                memKey = "${row.key}:${p.key}", onFocus = { onFocusEntry(p.key) }, onLongClick = { onWideLong(p) }
                            ) { onWide(p) }
                        }
                    } else {
                        items(row.entries, key = { it.key }) { e ->
                            Poster(e.title, e.image, rating = e.rating, memKey = "${row.key}:${e.key}", onFocus = { onFocusEntry(e.key) }, onLongClick = e.longPress) { e.open() }
                        }
                        val more = row.more
                        if (more != null) item(key = "more") {
                            Focus(Modifier.width(84.dp), onClick = more, memKey = "${row.key}:more") { f ->
                                Box(
                                    Modifier.width(84.dp).height(126.dp).clip(RoundedCornerShape(7.dp)).background(if (f) C.accent else C.item),
                                    contentAlignment = Alignment.Center
                                ) { Txt("Ver todos ›", F.s(9.sp, C.white, FontWeight.SemiBold)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Filmes

@Composable
fun MoviesScreen(vm: Vm, pb: Playback, ui: Ui) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { if (vm.movieState == LoadState.IDLE || vm.movieState == LoadState.ERROR) vm.loadMovies() }
    if (vm.movieState != LoadState.READY) {
        ScreenBackground()
        if (vm.movieState == LoadState.ERROR) StateRetry("Não foi possível carregar os filmes.") { vm.loadMovies() }
        else Loading("Carregando filmes…", Modifier.fillMaxSize())
        return
    }
    if (vm.movies.isEmpty()) { ScreenBackground(); EmptyState("Sua lista não tem filmes", "", Modifier.fillMaxSize()); return }

    val latest = remember(vm.movies) { vm.movies.sortedByDescending { it.added }.take(30) }
    val cont = vm.store.continueWatching(Kind.MOVIE)
    val mine = vm.store.myList.filter { it.startsWith("m:") }.mapNotNull { vm.movieById[it.removePrefix("m:")] }

    fun entry(m: Movie) = VodEntry(m.id, m.name, m.poster, m.rating, { ui.push(Route.MovieDetail(m, FocusMem.last)) }, { vm.store.toggleList("m:${m.id}") })

    val rows = remember(vm.movies, cont, mine) {
        buildList {
            if (cont.isNotEmpty()) add(VodRow("cont", "Continuar assistindo", emptyList(), wide = cont))
            add(VodRow("new", "Lançamentos", latest.map(::entry)))
            if (mine.isNotEmpty()) add(VodRow("mine", "Minha lista", mine.map(::entry)))
            vm.movieCats.forEach { c ->
                val l = vm.moviesByCat[c.id].orEmpty()
                add(VodRow("c" + c.id, c.name, l.take(30).map(::entry), more = if (l.size > 30) ({ ui.push(Route.CategoryGrid(true, c, FocusMem.last)) }) else null))
            }
        }
    }

    var focusKey by remember { mutableStateOf<String?>(null) }
    var heroMovie by remember { mutableStateOf(latest.first()) }
    LaunchedEffect(focusKey) {
        val k = focusKey ?: return@LaunchedEffect
        delay(350)
        val m = vm.movieById[k] ?: vm.movieById[k.removePrefix("mv:")] ?: return@LaunchedEffect
        heroMovie = m
    }
    LaunchedEffect(heroMovie.id) { vm.requestMovieInfo(heroMovie) }
    val info = vm.movieInfos[heroMovie.id]
    val hero = Hero(
        heroMovie.id, heroMovie.name, info?.backdrop ?: heroMovie.poster,
        listOfNotNull(info?.year, info?.durationSec?.takeIf { it > 0 }?.let(::fmtDuration), info?.age),
        info?.rating?.takeIf { it > 0 } ?: heroMovie.rating, info?.genre, info?.plot, "EM DESTAQUE"
    )
    val playReq = remember { FocusRequester() }
    InitialFocus(playReq)

    Box(Modifier.fillMaxSize()) {
        Backdrop(hero.backdrop, hero.title)
        HeroBlock(hero) {
            val prog = vm.store.progress["mv:${heroMovie.id}"]
            ActionButton(if (prog != null && !prog.finished && prog.pos > 30_000) "Continuar" else "Assistir", primary = true, icon = "▶", requester = playReq) {
                pb.openFull(movieItem(heroMovie, info?.genre.orEmpty()), startMs = prog?.takeIf { !it.finished }?.pos ?: 0L)
            }
            ActionButton(if (vm.store.inList("m:${heroMovie.id}")) "✓ Na minha lista" else "+ Minha lista") { vm.store.toggleList("m:${heroMovie.id}") }
            ActionButton("Trailer") { openTrailer(ctx, info?.trailer, heroMovie.name) }
        }
        VodRows(
            rows, ui.movieRows, onFocusEntry = { focusKey = it },
            onWide = { p ->
                val m = vm.movieById[p.key.removePrefix("mv:")]
                pb.openFull(PlayItem(Kind.MOVIE, p.key, p.title, p.subtitle, m?.url ?: p.url, p.image), startMs = p.pos)
            },
            onWideLong = { p -> vm.movieById[p.key.removePrefix("mv:")]?.let { ui.push(Route.MovieDetail(it, FocusMem.last)) } },
            modifier = Modifier.fillMaxSize().padding(top = 250.dp)
        )
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

@Composable
fun MovieDetail(vm: Vm, pb: Playback, ui: Ui, m: Movie) {
    val ctx = LocalContext.current
    LaunchedEffect(m.id) { vm.requestMovieInfo(m) }
    val info = vm.movieInfos[m.id]
    val prog = vm.store.progress["mv:${m.id}"]?.takeIf { !it.finished && it.pos > 30_000 }
    val playReq = remember { FocusRequester() }
    InitialFocus(playReq)
    Box(Modifier.fillMaxSize()) {
        Backdrop(info?.backdrop ?: m.poster, m.name)
        Row(Modifier.fillMaxSize().padding(start = 24.dp, end = 40.dp, top = 40.dp)) {
            Column(Modifier.weight(1f)) {
                Txt("FILME", F.s(8.sp, C.cyan, FontWeight.SemiBold, spacing = 2.sp))
                Spacer(Modifier.height(4.dp))
                Txt(m.name, F.s(30.sp, C.white, FontWeight.Bold, FontFamily.Serif), maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOfNotNull(info?.year, info?.durationSec?.takeIf { it > 0 }?.let(::fmtDuration), info?.age).forEach { MetaChip(it) }
                    val r = info?.rating?.takeIf { it > 0 } ?: m.rating
                    if (r > 0) MetaChip(stars(r), C.star, Color(0x33FBBF24))
                    info?.genre?.let { Txt(it, F.s(8.5.sp, C.dim)) }
                }
                Spacer(Modifier.height(10.dp))
                Txt(info?.plot ?: if (vm.source is XtreamSource && info == null) "Carregando informações…" else "", F.s(9.5.sp, C.text), Modifier.width(430.dp), maxLines = 5)
                info?.cast?.let { Spacer(Modifier.height(8.dp)); Txt("Elenco: $it", F.s(8.5.sp, C.dim), Modifier.width(430.dp), maxLines = 2) }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(if (prog != null) "Continuar ${fmtTime(prog.pos)}" else "Assistir", primary = true, icon = "▶", requester = playReq) {
                        pb.openFull(movieItem(m, info?.genre.orEmpty()), startMs = prog?.pos ?: 0L)
                    }
                    if (prog != null) ActionButton("Do início") { pb.openFull(movieItem(m, info?.genre.orEmpty()), startMs = 1L); pb.exo.seekTo(0) }
                    ActionButton(if (vm.store.inList("m:${m.id}")) "✓ Na minha lista" else "+ Minha lista") { vm.store.toggleList("m:${m.id}") }
                    ActionButton("Trailer") { openTrailer(ctx, info?.trailer, m.name) }
                }
                if (prog != null) {
                    Spacer(Modifier.height(10.dp))
                    ProgressBar(prog.fraction, Modifier.width(200.dp).height(3.dp).clip(RoundedCornerShape(2.dp)), C.accent)
                    Txt("Faltam ${prog.remainingMin} min", F.s(8.sp, C.dim), Modifier.padding(top = 4.dp))
                }
            }
            Box(Modifier.padding(top = 20.dp).width(150.dp).height(225.dp).clip(RoundedCornerShape(10.dp))) {
                GeneratedArt(m.name, Modifier.fillMaxSize(), big = true)
                if (m.poster != null) AsyncImage(m.poster, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        HintBar(listOf("OK" to "Selecionar", "VOLTAR" to "Filmes"), Modifier.align(Alignment.BottomStart).padding(bottom = 14.dp))
    }
}

@Composable
fun CategoryGrid(vm: Vm, pb: Playback, ui: Ui, r: Route.CategoryGrid) {
    val entries: List<VodEntry> = if (r.movies) vm.moviesByCat[r.cat.id].orEmpty().map { m ->
        VodEntry(m.id, m.name, m.poster, m.rating, { ui.push(Route.MovieDetail(m, FocusMem.last)) }, { vm.store.toggleList("m:${m.id}") })
    } else vm.seriesByCat[r.cat.id].orEmpty().map { s ->
        VodEntry(s.id, s.name, s.cover, s.rating, { ui.push(Route.SeriesDetail(s, FocusMem.last)) }, { vm.store.toggleList("s:${s.id}") })
    }
    val first = remember { FocusRequester() }
    InitialFocus(first)
    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.fillMaxSize().padding(top = 20.dp)) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(r.cat.name, F.s(16.sp, C.white, FontWeight.Bold, F.outfit), Modifier.weight(1f))
                Txt("${entries.size} títulos", F.s(9.sp, C.dim))
            }
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                GridCells.Fixed(9), Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 30.dp, top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(entries, key = { _, e -> e.key }) { i, e ->
                    Poster(e.title, e.image, rating = e.rating, requester = if (i == 0) first else null, memKey = "grid:${e.key}", onLongClick = e.longPress) { e.open() }
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

    val latest = remember(vm.series) { vm.series.sortedByDescending { it.added }.take(30) }
    val cont = vm.store.continueWatching(Kind.EPISODE)
    val mine = vm.store.myList.filter { it.startsWith("s:") }.mapNotNull { vm.seriesById[it.removePrefix("s:")] }
    fun entry(s: Series) = VodEntry(s.id, s.name, s.cover, s.rating, { ui.push(Route.SeriesDetail(s, FocusMem.last)) }, { vm.store.toggleList("s:${s.id}") })
    val rows = remember(vm.series, cont, mine) {
        buildList {
            if (cont.isNotEmpty()) add(VodRow("cont", "Continuar assistindo", emptyList(), wide = cont))
            add(VodRow("new", "Adicionadas recentemente", latest.map(::entry)))
            if (mine.isNotEmpty()) add(VodRow("mine", "Minha lista", mine.map(::entry)))
            vm.seriesCats.forEach { c ->
                val l = vm.seriesByCat[c.id].orEmpty()
                add(VodRow("c" + c.id, c.name, l.take(30).map(::entry), more = if (l.size > 30) ({ ui.push(Route.CategoryGrid(false, c, FocusMem.last)) }) else null))
            }
        }
    }
    var focusKey by remember { mutableStateOf<String?>(null) }
    var heroS by remember { mutableStateOf(latest.first()) }
    LaunchedEffect(focusKey) {
        val k = focusKey ?: return@LaunchedEffect
        delay(350)
        val s = vm.seriesById[k] ?: vm.store.progress[k]?.seriesId?.let { vm.seriesById[it] } ?: return@LaunchedEffect
        heroS = s
    }
    val hero = Hero(heroS.id, heroS.name, heroS.backdrop ?: heroS.cover, listOfNotNull(heroS.year), heroS.rating, heroS.genre, heroS.plot, "SÉRIE")
    val playReq = remember { FocusRequester() }
    InitialFocus(playReq)
    Box(Modifier.fillMaxSize()) {
        Backdrop(hero.backdrop, hero.title)
        HeroBlock(hero) {
            val last = vm.store.lastEpisodeOf(heroS.id)
            ActionButton(if (last != null) "Continuar T${last.season} · E${last.episode}" else "Ver episódios", primary = true, icon = "▶", requester = playReq) {
                ui.push(Route.SeriesDetail(heroS, FocusMem.last))
            }
            ActionButton(if (vm.store.inList("s:${heroS.id}")) "✓ Na minha lista" else "+ Minha lista") { vm.store.toggleList("s:${heroS.id}") }
        }
        VodRows(
            rows, ui.seriesRows, onFocusEntry = { focusKey = it },
            onWide = { p -> p.seriesId?.let { vm.seriesById[it] }?.let { ui.push(Route.SeriesDetail(it, FocusMem.last)) } },
            onWideLong = { p -> p.seriesId?.let { vm.seriesById[it] }?.let { ui.push(Route.SeriesDetail(it, FocusMem.last)) } },
            modifier = Modifier.fillMaxSize().padding(top = 250.dp)
        )
    }
}


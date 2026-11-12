package com.orbita.play

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ícones redondos do canto superior direito (busca, recarregar, configurações) + relógio. */
@Composable
fun TopActions(onSearch: () -> Unit, onReload: () -> Unit, onSettings: () -> Unit, showDate: Boolean = false) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(10_000) } }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RoundAction(Ic.Search, "Buscar", "top:search", onSearch)
        RoundAction(Ic.Refresh, "Recarregar lista", "top:reload", onReload)
        RoundAction(Ic.Settings, "Configurações", "top:settings", onSettings)
        Column(Modifier.padding(start = 6.dp), horizontalAlignment = Alignment.End) {
            Txt(hhmm(now), F.s(if (showDate) 15.sp else 11.sp, C.white, FontWeight.Bold, F.outfit))
            if (showDate) Txt(remember(now / 3_600_000) { SimpleDateFormat("EEE, d MMM", Locale("pt", "BR")).format(Date(now)) }, F.s(7.5.sp, C.dim))
        }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, desc: String, key: String, onClick: () -> Unit) {
    Focus(onClick = onClick, memKey = key) { f ->
        Box(
            Modifier.size(26.dp).clip(CircleShape)
                .background(if (f) C.accent else Color(0x1AFFFFFF))
                .border(1.dp, if (f) C.white else Color(0x14FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, 13.dp, desc = desc) }
    }
}

private class HomeTile(val tab: Tab, val label: String, val icon: ImageVector, val sub: String)

/** Tela inicial: 4 seções e "Continuar assistindo", em superfícies sólidas e sóbrias. */
@Composable
fun HomeScreen(vm: Vm, pb: Playback, ui: Ui, onSearch: () -> Unit, onReload: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
            // topo: logo + ações
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Logo()
                Spacer(Modifier.weight(1f))
                TopActions(onSearch, onReload, onSettings, showDate = true)
            }
            Spacer(Modifier.height(22.dp))
            val tiles = listOf(
                HomeTile(Tab.CANAIS, "CANAIS", Ic.LiveTv, countLabel(vm.allowedChannels().size, "canal", "canais", vm.liveState == LoadState.READY)),
                HomeTile(Tab.FILMES, "FILMES", Ic.Movie, countLabel(vm.movies.size, "título", "títulos", vm.movieState == LoadState.READY)),
                HomeTile(Tab.SERIES, "SÉRIES", Ic.Series, countLabel(vm.series.size, "título", "títulos", vm.seriesState == LoadState.READY)),
                HomeTile(Tab.FUTEBOL, "FUTEBOL", Ic.Soccer, "Ao vivo, hoje e amanhã"),
            )
            val first = remember { FocusRequester() }
            InitialFocus(first)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                tiles.forEach { t ->
                    BigTile(t, Modifier.weight(1f), if (t.tab == ui.tab) first else null) { ui.openSection(t.tab) }
                }
            }
            Spacer(Modifier.height(34.dp))
            ContinueRow(vm, pb, ui)
        }
    }
}

private fun countLabel(n: Int, one: String, many: String, ready: Boolean) =
    if (!ready) "Carregando…" else if (n == 1) "1 $one" else "${fmtCount(n)} $many"

@Composable
private fun BigTile(t: HomeTile, modifier: Modifier, requester: FocusRequester?, onClick: () -> Unit) {
    Focus(modifier, onClick = onClick, requester = requester, memKey = "home:${t.tab.name}") { f ->
        val sc by animateFloatAsState(if (f) 1.04f else 1f, tween(120), label = "s")
        val shape = RoundedCornerShape(12.dp)
        Column(
            Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.fillMaxWidth().height(150.dp).clip(shape)
                .background(if (f) C.white else Color(0xE6131019))
                .border(1.dp, if (f) C.white else Color(0xFF231E33), shape)
                .padding(18.dp)
        ) {
            Icon(t.icon, 28.dp, if (f) C.accent else C.accentText)
            Spacer(Modifier.weight(1f))
            Txt(t.label, F.s(17.sp, if (f) Color(0xFF0B0A12) else C.white, FontWeight.Bold, F.outfit, 0.6.sp))
            Spacer(Modifier.height(2.dp))
            Txt(t.sub, F.s(8.5.sp, if (f) Color(0xFF5B5670) else C.dim, FontWeight.Medium))
        }
    }
}

@Composable
private fun ContinueRow(vm: Vm, pb: Playback, ui: Ui) {
    val items = vm.store.continueWatching().take(12)
    Column(Modifier.fillMaxWidth()) {
        Txt("Continuar assistindo", F.s(13.sp, C.white, FontWeight.Bold, F.outfit), Modifier.padding(bottom = 10.dp))
        if (items.isEmpty()) {
            Txt("Os filmes e episódios que você começar aparecem aqui.", F.s(9.sp, C.dim))
            return
        }
        LazyRow(contentPadding = PaddingValues(end = 32.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items, key = { it.key }) { p -> HomeContinueCard(vm, pb, ui, p) }
        }
    }
}

@Composable
private fun HomeContinueCard(vm: Vm, pb: Playback, ui: Ui, p: Progress) {
    val movie = if (p.kind == Kind.MOVIE) vm.movieById[p.key.removePrefix("mv:")] else null
    val series = p.seriesId?.let { vm.seriesById[it] }
    // capa horizontal do TMDB quando já conhecida
    val meta = movie?.let { vm.metaOf(it) } ?: series?.let { vm.metaOf(it) }
    LaunchedEffect(p.key) { movie?.let { vm.requestMeta(it) }; series?.let { vm.requestMeta(it) } }
    val image = meta?.backdropUrl() ?: p.image
    Focus(
        Modifier.width(200.dp), memKey = "home:cont:${p.key}",
        onClick = {
            val item = when {
                movie != null -> movieItem(movie)
                p.url.isNotBlank() -> PlayItem(p.kind, p.key, p.title, p.subtitle, p.url, p.image, series = series)
                else -> null
            }
            if (item != null) pb.openFull(item, startMs = p.pos)
            else if (series != null) ui.push(Route.SeriesDetail(series, FocusMem.last))
        },
        onLongClick = {
            if (movie != null) ui.push(Route.MovieDetail(movie, FocusMem.last))
            else if (series != null) ui.push(Route.SeriesDetail(series, FocusMem.last))
        },
    ) { f ->
        val sc by animateFloatAsState(if (f) 1.05f else 1f, tween(120), label = "s")
        Column {
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.fillMaxWidth().height(112.dp).clip(shape)
                    .background(Color(0xFF15121F))
                    .then(if (f) Modifier.border(2.dp, C.white, shape) else Modifier)
            ) {
                if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (f) Box(
                    Modifier.align(Alignment.Center).size(30.dp).clip(CircleShape).background(C.white),
                    contentAlignment = Alignment.Center
                ) { Icon(Ic.Play, 16.dp, Color(0xFF0B0A12)) }
                ProgressBar(p.fraction, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp), C.accent, Color(0x66000000))
            }
            Spacer(Modifier.height(7.dp))
            Txt(meta?.title ?: p.title, F.s(9.5.sp, if (f) C.white else C.text, FontWeight.SemiBold), maxLines = 1)
            Txt(
                if (p.kind == Kind.EPISODE) "${p.subtitle.substringBefore(" — ")} · ${p.remainingMin} min restantes" else "${p.remainingMin} min restantes",
                F.s(8.sp, C.dim), maxLines = 1
            )
        }
    }
}

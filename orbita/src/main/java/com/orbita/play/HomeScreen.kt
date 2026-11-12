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

/** Tela inicial: papel de parede escolhido, saudação, 4 blocos grandes e "Continuar assistindo". */
@Composable
fun HomeScreen(vm: Vm, pb: Playback, ui: Ui, onSearch: () -> Unit, onReload: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.padding(start = 28.dp, top = 62.dp)) {
            val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            Txt(if (h < 5) "Boa noite" else if (h < 12) "Bom dia" else if (h < 18) "Boa tarde" else "Boa noite", F.s(22.sp, C.white, FontWeight.Bold, F.outfit))
            Spacer(Modifier.height(2.dp))
            Txt("O que vamos assistir hoje?", F.s(10.sp, C.dim))
        }

        // topo: logo + ações
        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo()
            Spacer(Modifier.weight(1f))
            TopActions(onSearch, onReload, onSettings, showDate = true)
        }

        Column(Modifier.fillMaxSize().padding(top = 134.dp)) {
            val tiles = listOf(
                HomeTile(Tab.CANAIS, "CANAIS", Ic.LiveTv, countLabel(vm.allowedChannels().size, "canal", "canais", vm.liveState == LoadState.READY)),
                HomeTile(Tab.FILMES, "FILMES", Ic.Movie, countLabel(vm.movies.size, "título", "títulos", vm.movieState == LoadState.READY)),
                HomeTile(Tab.SERIES, "SÉRIES", Ic.Series, countLabel(vm.series.size, "título", "títulos", vm.seriesState == LoadState.READY)),
                HomeTile(Tab.FUTEBOL, "FUTEBOL", Ic.Soccer, "Ao vivo, hoje e amanhã"),
            )
            val first = remember { FocusRequester() }
            InitialFocus(first)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                tiles.forEach { t ->
                    BigTile(t, if (t.tab == ui.tab) first else null) { ui.openSection(t.tab) }
                }
            }
            Spacer(Modifier.height(26.dp))
            ContinueRow(vm, pb, ui)
        }
    }
}

private fun countLabel(n: Int, one: String, many: String, ready: Boolean) =
    if (!ready) "Carregando…" else if (n == 1) "1 $one" else "${fmtCount(n)} $many"

@Composable
private fun BigTile(t: HomeTile, requester: FocusRequester?, onClick: () -> Unit) {
    Focus(Modifier.width(194.dp), onClick = onClick, requester = requester, memKey = "home:${t.tab.name}") { f ->
        val sc by animateFloatAsState(if (f) 1.07f else 1f, tween(140), label = "s")
        val shape = RoundedCornerShape(16.dp)
        Box(
            Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.fillMaxWidth().height(138.dp).clip(shape)
                .background(
                    if (f) Brush.linearGradient(listOf(Color(0xFF9378FF), Color(0xFF5A36E0)))
                    else Brush.linearGradient(listOf(Color(0xCC1A1530), Color(0xCC0E0B1A)))
                )
                .border(if (f) 2.dp else 1.dp, if (f) C.white else Color(0x1FFFFFFF), shape)
        ) {
            // ícone grande e apagado no canto, só decoração
            Icon(t.icon, 96.dp, Color.White.copy(alpha = if (f) 0.16f else 0.06f), Modifier.align(Alignment.TopEnd).padding(top = 0.dp).graphicsLayer { translationX = 22f; translationY = -14f })
            Column(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 12.dp)) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (f) Color(0x33FFFFFF) else C.accentSoft),
                    contentAlignment = Alignment.Center
                ) { Icon(t.icon, 20.dp, if (f) C.white else C.accentText) }
                Spacer(Modifier.height(10.dp))
                Txt(t.label, F.s(15.sp, C.white, FontWeight.Bold, F.outfit, 0.8.sp))
                Txt(t.sub, F.s(8.sp, if (f) Color(0xE6FFFFFF) else C.dim))
            }
        }
    }
}

@Composable
private fun ContinueRow(vm: Vm, pb: Playback, ui: Ui) {
    val items = vm.store.continueWatching().take(12)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 28.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Ic.Resume, 13.dp, C.accentText)
            Spacer(Modifier.width(6.dp))
            Txt("Continuar assistindo", F.s(12.sp, C.white, FontWeight.Bold, F.outfit))
            if (items.isNotEmpty()) Txt("  ${items.size} ${if (items.size == 1) "TÍTULO" else "TÍTULOS"}", F.s(8.sp, C.dim, FontWeight.Medium))
        }
        if (items.isEmpty()) {
            Txt("Comece um filme ou episódio e ele aparece aqui para você continuar de onde parou.", F.s(9.sp, C.dim), Modifier.padding(start = 28.dp))
            return
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Modifier.width(168.dp), memKey = "home:cont:${p.key}",
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
        val sc by animateFloatAsState(if (f) 1.06f else 1f, tween(140), label = "s")
        Column {
            val shape = RoundedCornerShape(9.dp)
            Box(
                Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.fillMaxWidth().height(94.dp).clip(shape)
                    .then(if (f) Modifier.border(2.dp, C.white, shape) else Modifier)
            ) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0E1530), C.hash(p.title)))))
                if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))))
                Box(
                    Modifier.align(Alignment.Center).size(28.dp).clip(CircleShape).background(if (f) C.accent else Color(0x8C000000)),
                    contentAlignment = Alignment.Center
                ) { Icon(Ic.Play, 15.dp) }
                ProgressBar(p.fraction, Modifier.align(Alignment.BottomCenter).padding(horizontal = 6.dp, vertical = 6.dp).fillMaxWidth().height(3.dp), C.accent, Color(0x55FFFFFF))
            }
            Spacer(Modifier.height(5.dp))
            Txt(meta?.title ?: p.title, F.s(9.5.sp, C.white, FontWeight.SemiBold), maxLines = 1)
            Txt(
                if (p.kind == Kind.EPISODE) "${p.subtitle.substringBefore(" — ")} · faltam ${p.remainingMin} min" else "Faltam ${p.remainingMin} min",
                F.s(8.sp, C.dim), maxLines = 1
            )
        }
    }
}

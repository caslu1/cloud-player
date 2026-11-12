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

private class HomeTile(val key: String, val label: String, val icon: ImageVector, val c1: Color, val c2: Color, val onClick: () -> Unit)

/**
 * Tela inicial: logo no centro, fundo com capas do seu catálogo, uma fileira de blocos quadrados,
 * Buscar/Recarregar e o rodapé com versão, status e ID do aparelho.
 */
@Composable
fun HomeScreen(vm: Vm, pb: Playback, ui: Ui, onSearch: () -> Unit, onReload: () -> Unit, onSettings: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(10_000) } }
    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        PosterWall(vm)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0x99050409), 0.45f to Color(0xC7050409), 1f to Color(0xF2050409))))

        // relógio no canto
        Column(Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 30.dp), horizontalAlignment = Alignment.End) {
            Txt(hhmm(now), F.s(15.sp, C.white, FontWeight.Bold, F.outfit))
            Txt(remember(now / 3_600_000) { SimpleDateFormat("EEE, d MMM", Locale("pt", "BR")).format(Date(now)) }, F.s(7.5.sp, C.dim))
        }

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(30.dp))
            Logo(size = 22f)
            Spacer(Modifier.height(34.dp))
            val tiles = listOf(
                HomeTile("CANAIS", "Canais", Ic.LiveTv, Color(0xFF8E73FF), Color(0xFF4B2BD0)) { ui.openSection(Tab.CANAIS) },
                HomeTile("FILMES", "Filmes", Ic.Movie, Color(0xFFFF6FA8), Color(0xFF9A2A86)) { ui.openSection(Tab.FILMES) },
                HomeTile("SERIES", "Séries", Ic.Series, Color(0xFF5FB0FF), Color(0xFF2346C8)) { ui.openSection(Tab.SERIES) },
                HomeTile("FUTEBOL", "Futebol", Ic.Soccer, Color(0xFF5FE0A0), Color(0xFF13804F)) { ui.openSection(Tab.FUTEBOL) },
                HomeTile("settings", "Configurações", Ic.Settings, Color(0xFFB9B4CC), Color(0xFF5A5570), onSettings),
            )
            val first = remember { FocusRequester() }
            InitialFocus(first)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                tiles.forEach { t -> SquareTile(t, if (t.key == ui.tab.name) first else null) }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WideButton(Ic.Search, "Buscar", "home:search", onSearch)
                WideButton(Ic.Refresh, "Recarregar", "home:reload", onReload)
            }
            Spacer(Modifier.height(22.dp))
            ContinueRow(vm, pb, ui)
        }

        // rodapé
        val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
        val st = vm.status
        val status = buildString {
            append(if (st?.status == null || st.status.equals("Active", true)) "Ativa" else st.status)
            st?.expires?.let { append(" · vence " + SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(it))) }
        }
        val deviceId = remember {
            runCatching { android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.ANDROID_ID) }
                .getOrNull().orEmpty().uppercase().take(8).ifEmpty { "—" }
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 30.dp, vertical = 14.dp)) {
            Column {
                Txt("Versão: $ver", F.s(8.sp, C.dim))
                Txt("Status: $status", F.s(8.sp, C.dim))
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Txt("ID do aparelho: $deviceId", F.s(8.sp, C.dim))
                vm.account?.takeIf { it.type == "xtream" }?.let { Txt("Usuário: ${it.user}", F.s(8.sp, C.dim)) }
            }
        }
    }
}

/** Fundo com capas inclinadas do próprio catálogo (bem apagadas). */
@Composable
private fun PosterWall(vm: Vm) {
    val posters = remember(vm.movies, vm.series) {
        (vm.movies.sortedByDescending { it.added }.take(18).mapNotNull { it.poster } +
            vm.series.sortedByDescending { it.added }.take(14).mapNotNull { it.cover }).filter { it.isNotBlank() }
    }
    if (posters.isEmpty()) return
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp))) {
        Column(
            Modifier.align(Alignment.Center).graphicsLayer { rotationZ = -10f; scaleX = 1.25f; scaleY = 1.25f; alpha = 0.32f },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (r in 0 until 4) Row(Modifier.graphicsLayer { translationX = if (r % 2 == 0) 0f else 60f }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (c in 0 until 9) {
                    val url = posters[(r * 9 + c) % posters.size]
                    AsyncImage(url, null, Modifier.size(100.dp, 150.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF15121F)), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

@Composable
private fun SquareTile(t: HomeTile, requester: FocusRequester?) {
    Focus(Modifier.size(124.dp), onClick = t.onClick, requester = requester, memKey = "home:${t.key}") { f ->
        val sc by animateFloatAsState(if (f) 1.08f else 1f, tween(120), label = "s")
        val shape = RoundedCornerShape(12.dp)
        Column(
            Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.fillMaxSize().clip(shape)
                .background(if (f) Color(0xF2221A4A) else Color(0xCC110E22))
                .border(if (f) 2.dp else 1.dp, if (f) C.white else Color(0x33FFFFFF), shape),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            // ícone num quadrado com brilho (estilo "app")
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(t.c1, t.c2)))
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color.Transparent))))
                Icon(t.icon, 28.dp)
            }
            Spacer(Modifier.height(12.dp))
            Txt(t.label, F.s(10.5.sp, C.white, FontWeight.SemiBold))
        }
    }
}

@Composable
private fun WideButton(icon: ImageVector, label: String, key: String, onClick: () -> Unit) {
    Focus(onClick = onClick, memKey = key) { f ->
        val shape = RoundedCornerShape(8.dp)
        Row(
            Modifier.width(170.dp).height(32.dp).clip(shape).background(if (f) C.white else Color(0xCC110E22))
                .border(1.dp, if (f) C.white else Color(0x33FFFFFF), shape),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, 14.dp, if (f) Color(0xFF0B0A12) else C.white)
            Spacer(Modifier.width(8.dp))
            Txt(label, F.s(10.sp, if (f) Color(0xFF0B0A12) else C.white, FontWeight.SemiBold))
        }
    }
}

@Composable
private fun ContinueRow(vm: Vm, pb: Playback, ui: Ui) {
    val items = vm.store.continueWatching().take(12)
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        Txt("Continuar assistindo", F.s(11.sp, C.white, FontWeight.Bold, F.outfit), Modifier.padding(start = 30.dp, bottom = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 30.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Modifier.width(150.dp), memKey = "home:cont:${p.key}",
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
                Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.fillMaxWidth().height(84.dp).clip(shape)
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

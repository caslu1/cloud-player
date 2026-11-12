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


private class LiteTile(val key: String, val label: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * Tela inicial Lite: logo no centro, fundo com capas do seu catálogo, uma fileira de blocos quadrados,
 * Buscar/Recarregar e o rodapé com versão, status, MAC e chave.
 */
@Composable
fun HomeLite(vm: Vm, pb: Playback, ui: Ui, onSearch: () -> Unit, onReload: () -> Unit, onSettings: () -> Unit) {
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
            Spacer(Modifier.height(31.dp))
            Logo(size = 24f)
            Spacer(Modifier.height(52.dp))
            val tiles = listOf(
                LiteTile("CANAIS", "Canais", LineIc.Tv) { ui.openSection(Tab.CANAIS) },
                LiteTile("FILMES", "Filmes", LineIc.Film) { ui.openSection(Tab.FILMES) },
                LiteTile("SERIES", "Séries", LineIc.Series) { ui.openSection(Tab.SERIES) },
                LiteTile("FUTEBOL", "Futebol", LineIc.Ball) { ui.openSection(Tab.FUTEBOL) },
                LiteTile("settings", "Configurações", LineIc.Sliders, onSettings),
            )
            val first = remember { FocusRequester() }
            InitialFocus(first)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                tiles.forEach { t -> SquareTile(t, if (t.key == ui.tab.name) first else null) }
            }
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WideButton(Ic.Search, "Buscar", "home:search", onSearch)
                WideButton(Ic.Refresh, "Recarregar", "home:reload", onReload)
            }
        }

        // rodapé
        val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
        val st = vm.status
        val status = buildString {
            append(if (st?.status == null || st.status.equals("Active", true)) "Ativa" else st.status)
            st?.expires?.let { append(" · vence " + SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(it))) }
        }
        val mac = remember { Activation.mac(ctx) }
        val key = remember { Activation.key(ctx) }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 30.dp, vertical = 14.dp)) {
            Column {
                Txt("Versão: $ver", F.s(8.sp, C.dim))
                Txt("Status: $status", F.s(8.sp, C.dim))
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Txt("MAC: $mac", F.s(8.sp, C.dim))
                Txt("Chave: $key", F.s(8.sp, C.dim))
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
private fun SquareTile(t: LiteTile, requester: FocusRequester?) {
    Focus(Modifier.size(124.dp), onClick = t.onClick, requester = requester, memKey = "home:${t.key}") { f ->
        val sc by animateFloatAsState(if (f) 1.08f else 1f, tween(120), label = "s")
        val shape = RoundedCornerShape(10.dp)
        Column(
            Modifier.graphicsLayer { scaleX = sc; scaleY = sc }.fillMaxSize().clip(shape)
                .background(if (f) Color(0xFFF4F2FA) else Color(0xD1100D1C))
                .border(1.dp, if (f) C.white else Color(0x24FFFFFF), shape),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Icon(t.icon, 40.dp, if (f) Color(0xFF2A1D6E) else Color(0xFFE9E6F5))
            Spacer(Modifier.height(14.dp))
            Txt(t.label, F.s(10.5.sp, if (f) Color(0xFF120F1F) else Color(0xFFE9E6F5), FontWeight.SemiBold))
        }
    }
}

@Composable
private fun WideButton(icon: ImageVector, label: String, key: String, onClick: () -> Unit) {
    Focus(onClick = onClick, memKey = key) { f ->
        val shape = RoundedCornerShape(8.dp)
        Row(
            Modifier.width(170.dp).height(32.dp).clip(shape).background(if (f) Color(0xFFF4F2FA) else Color(0xD1100D1C))
                .border(1.dp, if (f) C.white else Color(0x24FFFFFF), shape),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, 14.dp, if (f) Color(0xFF0B0A12) else C.white)
            Spacer(Modifier.width(8.dp))
            Txt(label, F.s(10.sp, if (f) Color(0xFF0B0A12) else C.white, FontWeight.SemiBold))
        }
    }
}


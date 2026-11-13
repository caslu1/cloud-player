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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class VitrineCard(val key: String, val title: String, val count: String, val sub: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * Tela inicial Vitrine: logo no centro, cartões de vidro grandes (o focado inclina e ganha a aba roxa),
 * botões redondos no topo e relógio + dados do aparelho no rodapé.
 */
@Composable
fun HomeCards(vm: Vm, pb: Playback, ui: Ui, onSearch: () -> Unit, onReload: () -> Unit, onSettings: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(10_000) } }
    val nf = remember { NumberFormat.getIntegerInstance(Locale("pt", "BR")) }
    fun cnt(n: Int, one: String, many: String) = if (n == 0) "Carregando…" else "${nf.format(n)} " + (if (n == 1) one else many)

    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        // capa de fundo (lançamento mais recente), bem escura
        val hero = remember(vm.series, vm.movies) {
            vm.series.sortedByDescending { it.added }.firstNotNullOfOrNull { it.backdrop?.takeIf { b -> b.isNotBlank() } }
                ?: vm.movies.sortedByDescending { it.added }.firstNotNullOfOrNull { it.poster?.takeIf { b -> b.isNotBlank() } }
        }
        if (hero != null) AsyncImage(hero, null, Modifier.fillMaxSize().graphicsLayer { alpha = 0.38f }, contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color(0xB3050409), 0.5f to Color(0x8C050409), 1f to Color(0xF2050409))))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0x99050409), 0.6f to Color(0x00050409))))

        // topo: logo no centro, botões redondos à direita
        Logo(Modifier.align(Alignment.TopCenter).padding(top = 58.dp), size = 20f)
        Row(Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundBtn(Ic.Search, "Buscar", "home:search", onSearch)
            RoundBtn(Ic.Refresh, "Recarregar", "home:reload", onReload)
            RoundBtn(Ic.Settings, "Configurações", "home:settings", onSettings)
        }

        val cards = listOf(
            VitrineCard("CANAIS", "Canais ao vivo", cnt(vm.channels.size, "canal", "canais"), "${vm.liveCats.size} categorias", DuoIc.Tv) { ui.openSection(Tab.CANAIS) },
            VitrineCard("FILMES", "Filmes", cnt(vm.movies.size, "filme", "filmes"), "${vm.movieCats.size} categorias", DuoIc.Camera) { ui.openSection(Tab.FILMES) },
            VitrineCard("SERIES", "Séries", cnt(vm.series.size, "série", "séries"), "${vm.seriesCats.size} categorias", DuoIc.Series) { ui.openSection(Tab.SERIES) },
            VitrineCard("FUTEBOL", "Futebol", if (vm.matches.isEmpty()) "Jogos de hoje" else cnt(vm.matches.size, "jogo", "jogos"), "Ao vivo, hoje e amanhã", DuoIc.Ball) { ui.openSection(Tab.FUTEBOL) },
        )
        val first = remember { FocusRequester() }
        InitialFocus(first)
        LazyRow(
            Modifier.align(Alignment.TopStart).padding(top = 150.dp).fillMaxWidth().height(190.dp),
            contentPadding = PaddingValues(start = 46.dp, end = 46.dp, top = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(cards, key = { it.key }) { c -> GlassCard(c, if (c.key == ui.tab.name) first else null) }
        }

        // rodapé: relógio, data e aparelho
        val st = vm.status
        val status = buildString {
            append(if (st?.status == null || st.status.equals("Active", true)) "Ativa" else st.status)
            st?.expires?.let { append(" · vence " + SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(it))) }
        }
        val mac = remember { Activation.mac(ctx) }
        val key = remember { Activation.key(ctx) }
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 46.dp, end = 46.dp, bottom = 34.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0x1FFFFFFF)), contentAlignment = Alignment.Center) {
                Icon(Ic.Key, 15.dp, C.white)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Txt("MAC  $mac", F.s(8.5.sp, C.text, FontWeight.Medium))
                Txt("Chave  $key", F.s(8.sp, C.dim))
            }
            Spacer(Modifier.weight(1f))
            Txt(hhmm(now), F.s(20.sp, C.white, FontWeight.Bold, F.outfit))
            Spacer(Modifier.width(14.dp))
            Box(Modifier.width(1.dp).height(30.dp).background(Color(0x33FFFFFF)))
            Spacer(Modifier.width(14.dp))
            Column {
                Txt(remember(now / 3_600_000) { SimpleDateFormat("EEEE, d 'de' MMM", Locale("pt", "BR")).format(Date(now)).replaceFirstChar { it.uppercase() } }, F.s(10.5.sp, C.white, FontWeight.SemiBold))
                Txt("Assinatura: $status", F.s(8.sp, C.dim))
            }
        }
    }
}

@Composable
private fun GlassCard(c: VitrineCard, requester: FocusRequester?) {
    Focus(Modifier.size(214.dp, 124.dp), onClick = c.onClick, requester = requester, memKey = "home:${c.key}") { f ->
        val k by animateFloatAsState(if (f) 1f else 0f, tween(160), label = "k")
        val shape = RoundedCornerShape(12.dp)
        Box(Modifier.fillMaxSize().graphicsLayer {
            rotationZ = -3.5f * k; scaleX = 1f + 0.07f * k; scaleY = 1f + 0.07f * k; translationY = -10f * k * density
        }) {
            // aba roxa em cima do cartão focado
            if (f) Box(
                Modifier.align(Alignment.TopStart).offset(x = 58.dp, y = (-5).dp).size(56.dp, 10.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(C.accent)
            )
            Box(
                Modifier.fillMaxSize().clip(shape)
                    .background(Brush.linearGradient(
                        if (f) listOf(Color(0x4D9B84FF), Color(0x26FFFFFF), Color(0x1A7C5CFF))
                        else listOf(Color(0x1FFFFFFF), Color(0x0FFFFFFF), Color(0x14FFFFFF))
                    ))
                    .border(1.dp, if (f) Color(0x80C9BCFF) else Color(0x24FFFFFF), shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Txt(c.title, F.s(14.sp, C.white, FontWeight.SemiBold, F.outfit), Modifier.align(Alignment.TopStart))
                Column(Modifier.align(Alignment.BottomStart)) {
                    Txt(c.count, F.s(9.5.sp, C.white, FontWeight.SemiBold))
                    Spacer(Modifier.height(3.dp))
                    Txt(c.sub, F.s(7.5.sp, if (f) C.text else C.dim))
                }
                androidx.compose.foundation.Image(androidx.compose.ui.graphics.vector.rememberVectorPainter(c.icon), null, Modifier.align(Alignment.CenterEnd).offset(y = 8.dp).size(54.dp))
            }
        }
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, desc: String, key: String, onClick: () -> Unit) {
    Focus(Modifier.size(34.dp), onClick = onClick, memKey = key) { f ->
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(if (f) C.white else Color(0x29FFFFFF))
                .border(1.dp, if (f) C.white else Color(0x1AFFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, 16.dp, if (f) Color(0xFF0B0A12) else C.white, desc = desc) }
    }
}

/** Ícones de duas cores (traço branco + detalhe roxo) da tela Vitrine. */
private object DuoIc {
    private val purple = Color(0xFF9B84FF)
    private fun duo(name: String, line: String, fill: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(addPathNodes(fill), fill = SolidColor(purple))
            .addPath(addPathNodes(line), fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            .build()

    val Tv by lazy { duo("dtv",
        "M6,7.5 H18 A3,3 0 0 1 21,10.5 V17 A3,3 0 0 1 18,20 H6 A3,3 0 0 1 3,17 V10.5 A3,3 0 0 1 6,7.5 Z M8.5,3 L12,6.5 L15.5,3",
        "M10.4,10.5 L15.2,13.75 L10.4,17 Z") }
    val Camera by lazy { duo("dcam",
        "M5.5,8 A2.6,2.6 0 1 0 5.51,8 Z M11.5,8 A2.6,2.6 0 1 0 11.51,8 Z M4,11.5 H15 A1.5,1.5 0 0 1 16.5,13 V19 A1.5,1.5 0 0 1 15,20.5 H4 A1.5,1.5 0 0 1 2.5,19 V13 A1.5,1.5 0 0 1 4,11.5 Z M16.5,14.5 L21.5,12 V20 L16.5,17.5",
        "M6,14 H11 V18 H6 Z") }
    val Series by lazy { duo("dser",
        "M6,4 H18 A3,3 0 0 1 21,7 V17 A3,3 0 0 1 18,20 H6 A3,3 0 0 1 3,17 V7 A3,3 0 0 1 6,4 Z M3,8.5 H21 M8,4 L9.5,8.5 M13,4 L14.5,8.5",
        "M10,11.2 L15.2,14.25 L10,17.3 Z") }
    val Ball by lazy { duo("dball",
        "M12,3 A9,9 0 1 0 12.01,3 Z M12.00,5.60 L12.00,8.70 M15.14,10.98 L18.09,10.02 M13.94,14.67 L15.76,17.18 M10.06,14.67 L8.24,17.18 M8.86,10.98 L5.91,10.02",
        "M12.00,8.70 L15.14,10.98 L13.94,14.67 L10.06,14.67 L8.86,10.98 Z") }
}

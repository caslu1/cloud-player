package com.orbita.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ptBR = Locale("pt", "BR")

internal fun dayIndex(t: Long): Int {
    val c = Calendar.getInstance(); val today = Calendar.getInstance()
    c.timeInMillis = t
    today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0)
    return Math.floorDiv(t - today.timeInMillis, 86_400_000L).toInt()
}

internal fun dayLabel(i: Int): String = when (i) {
    -1 -> "Ontem"; 0 -> "Hoje"; 1 -> "Amanhã"
    else -> {
        val c = Calendar.getInstance(); c.add(Calendar.DAY_OF_YEAR, i)
        SimpleDateFormat("EEE d", ptBR).format(c.time).replace(".", "").replaceFirstChar { it.uppercase() }
    }
}

private val menu = listOf("Jogos de hoje", "Ao vivo agora", "Próximos dias", "Times que sigo", "Canais de futebol")

@Composable
fun FootballScreen(vm: Vm, pb: Playback, ui: Ui) {
    if (FootballApi.enabled) { ApiFootballScreen(vm, pb, ui); return }
    ScreenBackground()
    LaunchedEffect(vm.liveState) { if (vm.liveState == LoadState.READY) vm.loadMatches() }
    if (vm.liveState != LoadState.READY) { Loading("Carregando canais…", Modifier.fillMaxSize()); return }

    val now = System.currentTimeMillis()
    val all = vm.matches
    val follows = vm.store.followedTeams.toList()
    fun followed(m: Match) = vm.store.follows(m.home) || vm.store.follows(m.away)
    val days = remember(all) { all.map { dayIndex(it.start) }.filter { it >= -1 }.distinct().sorted().ifEmpty { listOf(0) } }
    val day = if (ui.footballDay in days) ui.footballDay else if (0 in days) 0 else days.first()

    val shown: List<Match> = when (ui.footballMenu) {
        0 -> all.filter { dayIndex(it.start) == day }
        1 -> all.filter { it.isLive(now) }
        2 -> all.filter { dayIndex(it.start) >= 1 }
        3 -> all.filter { followed(it) }
        else -> emptyList()
    }.sortedWith(compareBy({ !followed(it) }, { if (it.isOver(now)) 1 else 0 }, { it.start }))

    var selected by remember { mutableStateOf<Match?>(null) }
    var selectedCh by remember { mutableStateOf<Channel?>(null) }
    val firstReq = remember { FocusRequester() }
    val menuReq = remember { FocusRequester() }
    InitialFocus(if (ui.footballMenu == 4 || shown.isNotEmpty()) firstReq else menuReq, vm.matchState)

    fun watch(c: Channel) {
        val items = vm.footballChannels.map { liveItem(it) }
        if (pb.current?.channel?.id == c.id) pb.openFull(liveItem(c), items) else pb.play(liveItem(c), items)
    }

    Column(Modifier.fillMaxSize().padding(top = 18.dp)) {
        Row(Modifier.weight(1f).padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
            // Menu
            Panel(Modifier.width(170.dp).fillMaxHeight()) {
                Column {
                    PanelHeader("Futebol")
                    menu.forEachIndexed { i, label ->
                        val count = when (i) {
                            0 -> all.count { dayIndex(it.start) == 0 }
                            1 -> all.count { it.isLive(now) }
                            2 -> all.count { dayIndex(it.start) >= 1 }
                            3 -> all.count { followed(it) }
                            else -> vm.footballChannels.size
                        }
                        MenuRow(label, count, ui.footballMenu == i, "fb:menu:$i", if (i == ui.footballMenu) menuReq else null,
                            onFocus = {}, onRight = { runCatching { firstReq.requestFocus() } }) { ui.footballMenu = i; runCatching { firstReq.requestFocus() } }
                    }
                    Spacer(Modifier.weight(1f))
                    Focus(Modifier.padding(10.dp).fillMaxWidth(), onClick = { vm.loadMatches(force = true) }, memKey = "fb:refresh") { f ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (f) C.accent else C.chip).padding(7.dp),
                            contentAlignment = Alignment.Center
                        ) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Ic.Refresh, 12.dp); Spacer(Modifier.width(6.dp)); Txt("Atualizar jogos", F.s(8.5.sp, C.white, FontWeight.SemiBold)) } }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            // Lista
            Panel(Modifier.width(470.dp).fillMaxHeight()) {
                Column {
                    if (ui.footballMenu == 4) {
                        PanelHeader("Canais de futebol", "${vm.footballChannels.size} canais")
                        LazyColumn(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            itemsIndexed(vm.footballChannels, key = { _, c -> c.id }) { i, c ->
                                ChannelRow(
                                    vm, c, playing = pb.current?.channel?.id == c.id, fav = vm.store.isFav(c.id),
                                    requester = if (i == 0) firstReq else null,
                                    onFocus = { selectedCh = c; selected = null }, onClick = { watch(c) }, onLong = { vm.store.toggleFav(c.id) }
                                )
                            }
                        }
                        return@Column
                    }
                    val title = when (ui.footballMenu) {
                        0 -> "Jogos de ${if (day == 0) "hoje" else dayLabel(day).lowercase()} · ${SimpleDateFormat("EEE dd/MM", ptBR).format(Date(System.currentTimeMillis() + day * 86_400_000L)).replace(".", "")}"
                        else -> menu[ui.footballMenu]
                    }
                    PanelHeader(title, "${shown.size} jogos")
                    if (ui.footballMenu == 0) {
                        LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(days, key = { it }) { d ->
                                ChipPill(dayLabel(d), d == day, onFocus = { ui.footballDay = d }) { ui.footballDay = d }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    when {
                        vm.matchState == LoadState.LOADING && all.isEmpty() ->
                            Loading("Procurando jogos na programação dos canais…", Modifier.fillMaxSize(), vm.matchProgress)
                        vm.source !is XtreamSource -> EmptyState("Jogos indisponíveis", "A agenda de jogos usa o guia (EPG) do login Xtream Codes. Veja os canais de futebol no menu ao lado.", Modifier.fillMaxSize())
                        shown.isEmpty() -> EmptyState(
                            when (ui.footballMenu) { 1 -> "Nenhum jogo ao vivo agora"; 3 -> "Nenhum jogo dos times que você segue"; else -> "Nenhum jogo encontrado" },
                            if (ui.footballMenu == 3) "Segure OK sobre um jogo para seguir os times." else "Os jogos vêm da programação (EPG) dos canais esportivos da sua lista.",
                            Modifier.fillMaxSize()
                        )
                        else -> LazyColumn(state = ui.matchList, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(shown, key = { _, m -> MatchLogic.key(m.home, m.away, m.start) }) { i, m ->
                                MatchRow(vm, m, followed(m), if (i == 0) firstReq else null,
                                    onFocus = { selected = m; selectedCh = m.channels.firstOrNull() },
                                    onLong = { if (vm.store.follows(m.home) && !vm.store.follows(m.away)) vm.store.toggleTeam(m.away) else vm.store.toggleTeam(m.home) }
                                ) { m.channels.firstOrNull()?.let { watch(it) } }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            // Prévia + detalhes
            Column(Modifier.weight(1f).fillMaxHeight()) {
                MatchPreview(vm, pb, selectedCh ?: selected?.channels?.firstOrNull(), selected, Modifier.fillMaxWidth().height(158.dp))
                Spacer(Modifier.height(10.dp))
                Panel(Modifier.fillMaxWidth().weight(1f)) {
                    MatchDetails(vm, pb, selected, follows) { watch(it) }
                }
            }
        }
    }
}

@Composable
internal fun MenuRow(
    label: String, count: Int, selected: Boolean, memKey: String, requester: FocusRequester?, onFocus: () -> Unit,
    onRight: (() -> Unit)? = null, onClick: () -> Unit,
) {
    Focus(
        Modifier.fillMaxWidth().height(28.5.dp), onClick = onClick, onFocus = onFocus, memKey = memKey, requester = requester,
        onKey = if (onRight == null) null else { ev ->
            if (ev.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && ev.key == androidx.compose.ui.input.key.Key.DirectionRight) { onRight(); true } else false
        }
    ) { f ->
        Row(
            Modifier.fillMaxSize().background(if (f) C.focusBg else if (selected) C.accentSoft else Color.Transparent)
                .then(if (f) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(4.dp)) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(2.5.dp).fillMaxHeight().background(if (selected) C.accent else Color.Transparent))
            Spacer(Modifier.width(12.dp))
            Txt(label, F.s(9.5.sp, if (selected || f) C.white else C.text, if (selected) FontWeight.SemiBold else FontWeight.Normal), Modifier.weight(1f))
            if (count > 0) Count(count, selected)
            Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
private fun StatusBadge(m: Match) {
    val now = System.currentTimeMillis()
    val (text, bg, fg) = when {
        m.isLive(now) -> Triple("AO VIVO", C.live, C.white)
        m.isOver(now) -> Triple("Encerrado", Color(0xFF2A2840), C.dim)
        else -> Triple(hhmm(m.start), C.cyanSoft, C.cyan)
    }
    Box(Modifier.width(55.dp).clip(RoundedCornerShape(4.dp)).background(bg).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Txt(text, F.s(7.sp, fg, FontWeight.Bold))
    }
}

@Composable
private fun MatchRow(vm: Vm, m: Match, followed: Boolean, requester: FocusRequester?, onFocus: () -> Unit, onLong: () -> Unit, onClick: () -> Unit) {
    Focus(Modifier.fillMaxWidth(), onClick = onClick, onFocus = onFocus, onLongClick = onLong, requester = requester, memKey = "match:${MatchLogic.key(m.home, m.away, m.start)}") { f ->
        val shape = RoundedCornerShape(8.dp)
        Row(
            Modifier.fillMaxWidth().height(41.dp).clip(shape).background(if (f) C.focusBg else Color(0x0FFFFFFF))
                .then(if (f) Modifier.border(1.5.dp, C.accent, shape) else Modifier).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(m)
            Spacer(Modifier.width(8.dp))
            Txt(m.home, F.s(9.sp, C.white, FontWeight.SemiBold), Modifier.weight(1f), align = TextAlign.End)
            Spacer(Modifier.width(6.dp))
            TeamShield(m.home, 21.dp)
            Txt("×", F.s(11.sp, C.dim, FontWeight.Bold), Modifier.width(24.dp), align = TextAlign.Center)
            TeamShield(m.away, 21.dp)
            Spacer(Modifier.width(6.dp))
            Txt(m.away, F.s(9.sp, C.white, FontWeight.SemiBold), Modifier.weight(1f))
            Column(Modifier.width(90.dp), horizontalAlignment = Alignment.End) {
                Txt((m.channels.firstOrNull()?.name ?: ""), F.s(7.sp, if (followed) C.star else C.dim))
                m.comp?.let { Txt(it, F.s(7.sp, C.faint)) }
            }
        }
    }
}

@Composable
private fun MatchPreview(vm: Vm, pb: Playback, c: Channel?, m: Match?, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val playing = pb.current?.takeIf { it.kind == Kind.LIVE }
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(C.fieldGreen, C.fieldDark))).border(1.dp, C.panelBorder, shape)) {
        if (playing != null && !pb.fullscreen) VideoSurface(pb, Modifier.fillMaxSize())
        else Pitch(Modifier.fillMaxSize().padding(14.dp))
        if (playing != null || m?.isLive() == true) LiveBadge(modifier = Modifier.padding(9.dp))
        val label = playing?.channel?.let { "${it.name}" + (vm.nowOn(it)?.let { p -> " · ${p.title}" } ?: "") }
            ?: c?.let { "${it.name} · OK para assistir" }
        if (label != null) {
            Txt(label, F.s(9.sp, C.white, FontWeight.Bold), Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC081A10)))).padding(10.dp))
        }
        if (pb.buffering && playing != null) Box(Modifier.align(Alignment.Center)) { Spinner() }
    }
}

/** Desenho do campo (linhas brancas) como no design. */
@Composable
internal fun Pitch(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
        val line = Color(0x55FFFFFF)
        drawRect(line, style = stroke)
        drawLine(line, androidx.compose.ui.geometry.Offset(size.width / 2, 0f), androidx.compose.ui.geometry.Offset(size.width / 2, size.height), 1.2.dp.toPx())
        drawCircle(line, size.height * 0.22f, style = stroke)
    }
}

@Composable
private fun MatchDetails(vm: Vm, pb: Playback, m: Match?, follows: List<String>, watch: (Channel) -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Txt("DETALHES DO JOGO", F.label)
        Spacer(Modifier.height(12.dp))
        if (m == null) {
            Txt(if (vm.matchState == LoadState.LOADING) "Procurando jogos…" else "Selecione um jogo para ver os detalhes.", F.s(9.sp, C.dim), maxLines = 3)
            return@Column
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                TeamShield(m.home, 34.dp); Spacer(Modifier.height(5.dp))
                Txt(m.home, F.s(9.sp, C.white, FontWeight.SemiBold), align = TextAlign.Center, maxLines = 2)
                if (follows.any { it.equals(m.home, true) }) Txt("Seguindo", F.s(7.sp, C.star, FontWeight.SemiBold))
            }
            Column(Modifier.width(70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Txt(if (m.isLive()) "AO VIVO" else if (m.isOver()) "Encerrado" else hhmm(m.start), F.s(if (m.isLive()) 9.sp else 16.sp, if (m.isLive()) C.live else C.white, FontWeight.Bold, F.outfit))
                Txt("${hhmm(m.start)} – ${hhmm(m.end)}", F.s(7.sp, C.dim))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                TeamShield(m.away, 34.dp); Spacer(Modifier.height(5.dp))
                Txt(m.away, F.s(9.sp, C.white, FontWeight.SemiBold), align = TextAlign.Center, maxLines = 2)
                if (follows.any { it.equals(m.away, true) }) Txt("Seguindo", F.s(7.sp, C.star, FontWeight.SemiBold))
            }
        }
        m.comp?.let { Spacer(Modifier.height(8.dp)); Txt(it, F.s(8.sp, C.text), align = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        if (m.desc.isNotBlank()) { Spacer(Modifier.height(6.dp)); Txt(m.desc, F.s(7.5.sp, C.dim), maxLines = 3) }
        Spacer(Modifier.height(12.dp))
        Txt("ONDE ASSISTIR", F.label)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            itemsIndexed(m.channels, key = { _, c -> c.id }) { i, c ->
                ChipPill(c.name, i == 0 || pb.current?.channel?.id == c.id) { watch(c) }
            }
        }
    }
}


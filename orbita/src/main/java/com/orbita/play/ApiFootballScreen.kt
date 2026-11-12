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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val apiMenu = listOf("Jogos do dia", "Ao vivo agora", "Na sua lista", "Times que sigo", "Canais de futebol")
private val apiDaysRange = (-1..5).toList()

/** Aba Futebol com a agenda e o placar da API (quando há token). */
@Composable
fun ApiFootballScreen(vm: Vm, pb: Playback, ui: Ui) {
    ScreenBackground()
    val day = if (ui.footballDay in apiDaysRange) ui.footballDay else 0
    LaunchedEffect(day) {
        vm.loadApiDay(day)
        // placar ao vivo: atualiza hoje a cada minuto enquanto a aba está aberta
        if (day == 0) while (true) { delay(60_000); vm.loadApiDay(0, force = true) }
    }
    val games = vm.apiDays[day]
    val error = vm.apiErrors[day]
    val index = remember(vm.channels) { vm.channelIndex() }
    val follows = vm.store.followedTeams.toList()
    fun followed(g: FootballApi.Game) = vm.store.follows(g.home) || vm.store.follows(g.away)
    fun channelsOf(g: FootballApi.Game) = FootballApi.matchChannels(g.broadcasters, index)

    val all = games.orEmpty()
    val shown = when (ui.footballMenu) {
        1 -> all.filter { FootballApi.isLive(it) }
        2 -> all.filter { channelsOf(it).isNotEmpty() }
        3 -> all.filter { followed(it) }
        else -> all
    }.sortedWith(compareBy({ !followed(it) }, { if (FootballApi.isLive(it)) 0 else if (FootballApi.isOver(it)) 2 else 1 }, { it.start }))

    var selected by remember { mutableStateOf<FootballApi.Game?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(note) { if (note != null) { delay(3000); note = null } }
    val firstReq = remember { FocusRequester() }
    val menuReq = remember { FocusRequester() }
    InitialFocus(if (ui.footballMenu == 4 || shown.isNotEmpty()) firstReq else menuReq, games != null)

    fun watch(c: Channel, list: List<Channel>) {
        val items = list.ifEmpty { listOf(c) }.map { liveItem(it) }
        if (pb.current?.channel?.id == c.id) pb.openFull(liveItem(c), items) else pb.play(liveItem(c), items)
    }

    Column(Modifier.fillMaxSize().padding(top = 50.dp)) {
        Row(Modifier.weight(1f).padding(start = 24.dp, end = 24.dp, bottom = 16.dp)) {
            Panel(Modifier.width(170.dp).fillMaxHeight()) {
                Column {
                    PanelHeader("Futebol")
                    apiMenu.forEachIndexed { i, label ->
                        val count = when (i) {
                            0 -> all.size
                            1 -> all.count { FootballApi.isLive(it) }
                            2 -> all.count { channelsOf(it).isNotEmpty() }
                            3 -> all.count { followed(it) }
                            else -> vm.footballChannels.size
                        }
                        MenuRow(label, count, ui.footballMenu == i, "fb:menu:$i", if (i == ui.footballMenu) menuReq else null,
                            onFocus = { ui.footballMenu = i }) { runCatching { firstReq.requestFocus() } }
                    }
                    Spacer(Modifier.weight(1f))
                    Focus(Modifier.padding(10.dp).fillMaxWidth(), onClick = { vm.loadApiDay(day, force = true) }, memKey = "fb:refresh") { f ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (f) C.accent else C.chip).padding(7.dp),
                            contentAlignment = Alignment.Center
                        ) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Ic.Refresh, 12.dp); Spacer(Modifier.width(6.dp)); Txt("Atualizar placar", F.s(8.5.sp, C.white, FontWeight.SemiBold)) } }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Panel(Modifier.width(470.dp).fillMaxHeight()) {
                Column {
                    if (ui.footballMenu == 4) {
                        PanelHeader("Canais de futebol", "${vm.footballChannels.size} canais")
                        LazyColumn(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            itemsIndexed(vm.footballChannels, key = { _, c -> c.id }) { i, c ->
                                ChannelRow(vm, c, pb.current?.channel?.id == c.id, vm.store.isFav(c.id), if (i == 0) firstReq else null,
                                    {}, { watch(c, vm.footballChannels) }, { vm.store.toggleFav(c.id) })
                            }
                        }
                        return@Column
                    }
                    val date = SimpleDateFormat("EEE dd/MM", Locale("pt", "BR")).format(Date(System.currentTimeMillis() + day * 86_400_000L)).replace(".", "")
                    val title = if (ui.footballMenu == 0) "Jogos de ${if (day == 0) "hoje" else dayLabel(day).lowercase()} · $date" else "${apiMenu[ui.footballMenu]} · $date"
                    PanelHeader(title, "${shown.size} jogos")
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(apiDaysRange, key = { it }) { d -> ChipPill(dayLabel(d), d == day, onFocus = { ui.footballDay = d }) { ui.footballDay = d } }
                    }
                    Spacer(Modifier.height(6.dp))
                    when {
                        games == null && error == null -> Loading("Carregando jogos…", Modifier.fillMaxSize())
                        games == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            EmptyState("Não foi possível carregar os jogos", error.orEmpty())
                            ActionButton("Tentar de novo", primary = true) { vm.loadApiDay(day, force = true) }
                        }
                        shown.isEmpty() -> EmptyState(
                            when (ui.footballMenu) { 1 -> "Nenhum jogo ao vivo agora"; 2 -> "Nenhum jogo nos canais da sua lista"; 3 -> "Nenhum jogo dos times que você segue"; else -> "Nenhum jogo neste dia" },
                            if (ui.footballMenu == 3) "Segure OK sobre um jogo para seguir os times." else "",
                            Modifier.fillMaxSize()
                        )
                        else -> LazyColumn(state = ui.matchList, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(shown, key = { _, g -> g.id }) { i, g ->
                                GameRow(g, followed(g), channelsOf(g), if (i == 0) firstReq else null,
                                    onFocus = { selected = g },
                                    onLong = { if (vm.store.follows(g.home) && !vm.store.follows(g.away)) vm.store.toggleTeam(g.away) else vm.store.toggleTeam(g.home) }
                                ) {
                                    selected = g
                                    val chs = channelsOf(g)
                                    if (chs.isNotEmpty()) watch(chs.first(), chs)
                                    else note = if (g.broadcasters.isEmpty()) "Sem informação de transmissão para este jogo." else "Nenhum canal da sua lista transmite este jogo (${g.broadcasters.take(3).joinToString()})."
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                GamePreview(vm, pb, selected, Modifier.fillMaxWidth().height(158.dp))
                Spacer(Modifier.height(10.dp))
                Panel(Modifier.fillMaxWidth().weight(1f)) {
                    GameDetails(pb, selected, selected?.let { channelsOf(it) }.orEmpty(), follows, note) { c, l -> watch(c, l) }
                }
            }
        }
    }
}

@Composable
private fun ApiBadge(g: FootballApi.Game) {
    val live = FootballApi.isLive(g)
    val over = FootballApi.isOver(g) || g.status == "postponed" || g.status.startsWith("cancel")
    val bg = when { live -> C.live; over -> Color(0xFF2A2840); else -> C.cyanSoft }
    val fg = when { live -> C.white; over -> C.dim; else -> C.cyan }
    Box(Modifier.width(62.dp).clip(RoundedCornerShape(4.dp)).background(bg).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Txt(FootballApi.statusLabel(g), F.s(7.sp, fg, FontWeight.Bold))
    }
}

private fun score(g: FootballApi.Game): String? {
    val started = FootballApi.isLive(g) || FootballApi.isOver(g)
    if (!started || g.golsA == null || g.golsB == null) return null
    val pen = if (g.penA != null && g.penB != null) " (${g.penA}-${g.penB})" else ""
    return "${g.golsA} × ${g.golsB}$pen"
}

@Composable
private fun GameRow(
    g: FootballApi.Game, followed: Boolean, channels: List<Channel>, requester: FocusRequester?,
    onFocus: () -> Unit, onLong: () -> Unit, onClick: () -> Unit,
) {
    Focus(Modifier.fillMaxWidth(), onClick = onClick, onFocus = onFocus, onLongClick = onLong, requester = requester, memKey = "game:${g.id}") { f ->
        val shape = RoundedCornerShape(8.dp)
        Row(
            Modifier.fillMaxWidth().height(41.dp).clip(shape).background(if (f) C.focusBg else Color(0x0FFFFFFF))
                .then(if (f) Modifier.border(1.5.dp, C.accent, shape) else Modifier).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ApiBadge(g)
            Spacer(Modifier.width(6.dp))
            Txt(g.home, F.s(8.5.sp, C.white, FontWeight.SemiBold), Modifier.weight(1f), align = TextAlign.End)
            Spacer(Modifier.width(5.dp))
            TeamShield(g.home, 19.dp, g.logoA)
            Txt(score(g) ?: "×", F.s(if (score(g) != null) 12.sp else 10.sp, if (score(g) != null) C.white else C.dim, FontWeight.Bold, F.outfit),
                Modifier.width(52.dp), align = TextAlign.Center)
            TeamShield(g.away, 19.dp, g.logoB)
            Spacer(Modifier.width(5.dp))
            Txt(g.away, F.s(8.5.sp, C.white, FontWeight.SemiBold), Modifier.weight(1f))
            Column(Modifier.width(92.dp), horizontalAlignment = Alignment.End) {
                val ch = channels.firstOrNull()?.name ?: g.broadcasters.firstOrNull() ?: ""
                Txt(ch, F.s(7.sp, if (channels.isNotEmpty()) C.cyan else if (followed) C.star else C.dim))
                g.comp?.let { Txt(it, F.s(6.5.sp, C.faint)) }
            }
        }
    }
}

@Composable
private fun GamePreview(vm: Vm, pb: Playback, g: FootballApi.Game?, modifier: Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val playing = pb.current?.takeIf { it.kind == Kind.LIVE }
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(C.fieldGreen, C.fieldDark))).border(1.dp, C.panelBorder, shape)) {
        if (playing != null && !pb.fullscreen) VideoSurface(pb, Modifier.fillMaxSize())
        else Pitch(Modifier.fillMaxSize().padding(14.dp))
        if (playing != null || (g != null && FootballApi.isLive(g))) LiveBadge(modifier = Modifier.padding(9.dp))
        if (g != null && playing == null && score(g) != null) {
            Box(Modifier.align(Alignment.TopEnd).padding(9.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xCC0B1A12)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Txt("${MatchLogic.initials(g.home)} ${score(g)} ${MatchLogic.initials(g.away)} · ${FootballApi.statusLabel(g)}", F.s(9.sp, C.white, FontWeight.Bold, F.outfit))
            }
        }
        val label = playing?.channel?.let { c -> c.name + (vm.nowOn(c)?.let { " · ${it.title}" } ?: "") }
            ?: g?.let { "${it.home} × ${it.away}" + (it.comp?.let { c -> " · $c" } ?: "") }
        if (label != null) {
            Txt(label, F.s(9.sp, C.white, FontWeight.Bold), Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC081A10)))).padding(10.dp))
        }
        if (pb.buffering && playing != null) Box(Modifier.align(Alignment.Center)) { Spinner() }
    }
}

@Composable
private fun GameDetails(pb: Playback, g: FootballApi.Game?, channels: List<Channel>, follows: List<String>, note: String?, watch: (Channel, List<Channel>) -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt("DETALHES DO JOGO", F.label, Modifier.weight(1f))
            g?.compLogo?.let { AsyncImage(it, null, Modifier.size(14.dp), contentScale = ContentScale.Fit) }
        }
        Spacer(Modifier.height(10.dp))
        if (g == null) { Txt("Selecione um jogo para ver os detalhes.", F.s(9.sp, C.dim), maxLines = 3); return@Column }
        g.comp?.let { Txt(it, F.s(8.sp, C.text), Modifier.fillMaxWidth(), align = TextAlign.Center); Spacer(Modifier.height(6.dp)) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                TeamShield(g.home, 34.dp, g.logoA); Spacer(Modifier.height(5.dp))
                Txt(g.home, F.s(9.sp, C.white, FontWeight.SemiBold), align = TextAlign.Center, maxLines = 2)
                if (follows.any { it.equals(g.home, true) }) Txt("Seguindo", F.s(7.sp, C.star, FontWeight.SemiBold))
            }
            Column(Modifier.width(80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val sc = score(g)
                Txt(sc ?: hhmm(g.start), F.s(if (sc != null) 20.sp else 16.sp, C.white, FontWeight.Bold, F.outfit), align = TextAlign.Center)
                Txt(FootballApi.statusLabel(g), F.s(7.5.sp, if (FootballApi.isLive(g)) C.live else C.dim, FontWeight.Bold))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                TeamShield(g.away, 34.dp, g.logoB); Spacer(Modifier.height(5.dp))
                Txt(g.away, F.s(9.sp, C.white, FontWeight.SemiBold), align = TextAlign.Center, maxLines = 2)
                if (follows.any { it.equals(g.away, true) }) Txt("Seguindo", F.s(7.sp, C.star, FontWeight.SemiBold))
            }
        }
        Spacer(Modifier.height(12.dp))
        Txt("ONDE ASSISTIR", F.label)
        Spacer(Modifier.height(6.dp))
        if (channels.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                itemsIndexed(channels, key = { _, c -> c.id }) { i, c ->
                    ChipPill(c.name, i == 0 || pb.current?.channel?.id == c.id) { watch(c, channels) }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        val others = g.broadcasters
        if (others.isNotEmpty()) Txt("Transmissão: " + others.joinToString(", "), F.s(7.5.sp, C.dim), maxLines = 3)
        else if (channels.isEmpty()) Txt("Sem informação de transmissão.", F.s(7.5.sp, C.dim))
        note?.let { Spacer(Modifier.height(8.dp)); Txt(it, F.s(8.sp, C.star, FontWeight.Medium), maxLines = 3) }
    }
}

package com.orbita.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private const val FAV = "__fav"
private const val RECENT = "__recent"
private const val ALL = "__all"

/** Canais da categoria escolhida (inclui Favoritos, Recentes e Todos). */
fun channelsOf(vm: Vm, cat: String): List<Channel> = when (cat) {
    FAV -> vm.store.favChannels.mapNotNull { vm.channelById[it] }
    RECENT -> Quality.unique(vm.store.recentChannels.mapNotNull { vm.channelById[it] })
    ALL -> vm.uniqueChannels
    else -> vm.uniqueByCat[cat].orEmpty()
}

@Composable
fun ChannelsScreen(vm: Vm, pb: Playback, ui: Ui) {
    ScreenBackground()
    when (vm.liveState) {
        LoadState.LOADING, LoadState.IDLE -> { Loading("Carregando canais…", Modifier.fillMaxSize()); return }
        LoadState.ERROR -> {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                EmptyState("Não foi possível carregar os canais", vm.liveError.orEmpty())
                val r = remember { FocusRequester() }
                ActionButton("Tentar de novo", primary = true, requester = r) { vm.loadLive() }
                InitialFocus(r)
            }
            return
        }
        LoadState.READY -> Unit
    }

    var catQuery by remember { mutableStateOf("") }
    var chQuery by remember { mutableStateOf("") }
    var focusedCh by remember { mutableStateOf<Channel?>(null) }
    var pendingCat by remember { mutableStateOf<String?>(null) }

    val cats = remember(vm.liveCats, catQuery) {
        val base = vm.liveCats
        if (catQuery.isBlank()) base else base.filter { MatchLogic.norm(it.name).contains(MatchLogic.norm(catQuery), true) }
    }
    val favCount = vm.store.favChannels.size
    val recentCount = vm.store.recentChannels.size
    val list = remember(ui.liveCat, chQuery, vm.channels, favCount, recentCount) {
        val l = channelsOf(vm, ui.liveCat)
        if (chQuery.isBlank()) l else {
            val q = MatchLogic.norm(chQuery)
            l.filter { MatchLogic.norm(it.name).contains(q, true) || it.num.toString() == chQuery.trim() }
        }
    }
    val catTitle = when (ui.liveCat) { FAV -> "Favoritos"; RECENT -> "Recentes"; ALL -> "Todos os canais"; else -> vm.catName(ui.liveCat) }

    LaunchedEffect(pendingCat) {
        val p = pendingCat ?: return@LaunchedEffect
        delay(220)
        if (ui.liveCat != p) { ui.liveCat = p; chQuery = "" }
    }
    LaunchedEffect(ui.liveCat) {
        if (ui.liveCatShown != null && ui.liveCatShown != ui.liveCat) ui.chList.scrollToItem(0)
        ui.liveCatShown = ui.liveCat
    }

    val playingId = (pb.current?.takeIf { it.kind == Kind.LIVE })?.channel?.id
    val initial = remember { FocusRequester() }
    val firstChannel = remember { FocusRequester() }
    val initialIdx = list.indexOfFirst { it.id == playingId }.let { if (it < 0) 0 else it }
    LaunchedEffect(Unit) { if (initialIdx > 0 && ui.chList.firstVisibleItemIndex == 0) ui.chList.scrollToItem((initialIdx - 3).coerceAtLeast(0)) }
    val catReq = remember { FocusRequester() }
    InitialFocus(if (list.isEmpty()) catReq else if (initialIdx > 0) initial else firstChannel)

    fun playItems() = list.map { liveItem(it) }
    fun open(c: Channel) {
        if (playingId == c.id) pb.openFull(liveItem(c), playItems())
        else pb.play(liveItem(c), playItems())
    }

    // Prévia automática ao parar sobre um canal (opcional em Configurações)
    LaunchedEffect(focusedCh?.id) {
        val c = focusedCh ?: return@LaunchedEffect
        vm.requestEpg(c)
        if (vm.store.previewAutoplay.value && playingId != c.id) { delay(1_200); pb.play(liveItem(c), playItems()) }
    }

    Column(Modifier.fillMaxSize().padding(top = 50.dp)) {
        Row(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            // Categorias
            Panel(Modifier.width(215.dp).fillMaxHeight()) {
                Column {
                    PanelHeader("Categorias", "${vm.liveCats.size}")
                    SearchField(catQuery, { catQuery = it }, "Buscar categoria", Modifier.padding(horizontal = 10.dp))
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(state = ui.catList, modifier = Modifier.weight(1f)) {
                        if (catQuery.isBlank()) {
                            item(key = FAV) { CatRow("Favoritos", favCount, ui.liveCat == FAV, "cat:$FAV", { pendingCat = FAV }, catReq, Ic.Star) { ui.liveCat = FAV; runCatching { firstChannel.requestFocus() } } }
                            item(key = RECENT) { CatRow("Recentes", recentCount, ui.liveCat == RECENT, "cat:$RECENT", { pendingCat = RECENT }, icon = Ic.History) { ui.liveCat = RECENT; runCatching { firstChannel.requestFocus() } } }
                            item(key = ALL) { CatRow("Todos os canais", vm.uniqueChannels.size, ui.liveCat == ALL, "cat:$ALL", { pendingCat = ALL }, icon = Ic.LiveTv) { ui.liveCat = ALL; runCatching { firstChannel.requestFocus() } } }
                        }
                        items(cats, key = { "c" + it.id }) { c ->
                            val icon = if (MatchLogic.isSportsName(c.name)) Ic.Soccer else null
                            CatRow(c.name, vm.uniqueByCat[c.id]?.size ?: 0, ui.liveCat == c.id, "cat:${c.id}", { pendingCat = c.id }, icon = icon) {
                                ui.liveCat = c.id; runCatching { firstChannel.requestFocus() }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            // Canais
            Panel(Modifier.width(345.dp).fillMaxHeight()) {
                Column {
                    PanelHeader(catTitle, "${list.size} canais")
                    SearchField(chQuery, { chQuery = it }, "Buscar canal", Modifier.padding(horizontal = 10.dp))
                    Spacer(Modifier.height(6.dp))
                    if (list.isEmpty()) {
                        EmptyState(
                            if (ui.liveCat == FAV) "Nenhum favorito ainda" else "Nenhum canal",
                            if (ui.liveCat == FAV) "Segure OK sobre um canal para favoritar." else "",
                            Modifier.fillMaxWidth()
                        )
                    }
                    LazyColumn(state = ui.chList, modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        itemsIndexed(list, key = { _, c -> c.id }) { i, c ->
                            val req = when { i == 0 -> firstChannel; i == initialIdx -> initial; else -> null }
                            ChannelRow(
                                vm, c, playing = c.id == playingId, fav = vm.store.isFav(c.id), requester = req,
                                onFocus = { focusedCh = c }, onClick = { open(c) }, onLong = { vm.store.toggleFav(c.id) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            // Prévia + guia
            Column(Modifier.weight(1f).fillMaxHeight()) {
                val shown = focusedCh ?: vm.channelById[playingId ?: ""] ?: list.firstOrNull()
                Preview(vm, pb, shown, Modifier.fillMaxWidth().height(196.dp))
                Spacer(Modifier.height(10.dp))
                Panel(Modifier.fillMaxWidth().weight(1f)) { EpgList(vm, shown) }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
internal fun CatRow(
    label: String, count: Int, selected: Boolean, memKey: String, onFocus: () -> Unit, requester: FocusRequester? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit,
) {
    Focus(Modifier.fillMaxWidth().height(36.dp), onClick = onClick, onFocus = onFocus, memKey = memKey, requester = requester) { f ->
        Row(
            Modifier.fillMaxSize()
                .background(if (f) C.focusBg else if (selected) C.accentSoft else Color.Transparent)
                .then(if (f) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(4.dp)) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(2.5.dp).fillMaxHeight().background(if (selected) C.accent else Color.Transparent))
            Spacer(Modifier.width(12.dp))
            if (icon != null) { Icon(icon, 14.dp, if (selected || f) C.white else C.dim); Spacer(Modifier.width(8.dp)) }
            Txt(label, F.s(11.5.sp, if (selected || f) C.white else C.text, if (selected) FontWeight.SemiBold else FontWeight.Medium), Modifier.weight(1f))
            Count(count, selected)
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
fun ChannelRow(
    vm: Vm, c: Channel, playing: Boolean, fav: Boolean, requester: FocusRequester?, onFocus: () -> Unit, onClick: () -> Unit, onLong: () -> Unit,
) {
    LaunchedEffect(c.id) { delay(150); vm.requestEpg(c) }
    Focus(
        Modifier.fillMaxWidth().padding(vertical = 0.5.dp), onClick = onClick, onLongClick = onLong, onFocus = onFocus,
        requester = requester, memKey = "ch:${c.id}"
    ) { f ->
        val shape = RoundedCornerShape(8.dp)
        Row(
            Modifier.fillMaxWidth().height(44.dp).clip(shape)
                .background(if (f) C.focusBg else Color.Transparent)
                .then(if (f) Modifier.border(1.5.dp, C.accent, shape) else Modifier)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt(c.num.toString(), F.s(10.sp, if (f || playing) C.cyan else C.dim, FontWeight.SemiBold), Modifier.width(36.dp))
            ChannelLogo(c, Modifier.size(42.dp, 30.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Txt(c.name, F.s(11.5.sp, C.white, FontWeight.SemiBold))
                val now = vm.nowOn(c)
                val sub = when {
                    now != null -> "${now.title} · ${hhmm(now.start)}"
                    playing -> "Assistindo agora"
                    else -> ""
                }
                if (sub.isNotEmpty()) Txt(sub, F.s(9.sp, if (playing) C.cyan else C.dim))
            }
            if (fav) Icon(Ic.Star, 13.dp, C.star)
        }
    }
}

@Composable
private fun Preview(vm: Vm, pb: Playback, c: Channel?, modifier: Modifier) {
    val playing = pb.current?.takeIf { it.kind == Kind.LIVE }
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(C.fieldGreen, C.fieldDark))).border(1.dp, C.panelBorder, shape)) {
        if (playing != null && !pb.fullscreen) VideoSurface(pb, Modifier.fillMaxSize())
        else if (c != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (c.logo != null) AsyncImage(c.logo, null, Modifier.size(90.dp, 60.dp))
                    else ChannelLogo(c, Modifier.size(80.dp, 50.dp))
                }
            }
        }
        val info = playing?.channel ?: c
        if (playing != null) LiveBadge(modifier = Modifier.padding(11.dp))
        if (pb.buffering && playing != null) Box(Modifier.align(Alignment.Center)) { Spinner() }
        pb.error?.takeIf { playing != null }?.let {
            Txt(it, F.s(9.sp, C.white, FontWeight.SemiBold), Modifier.align(Alignment.Center).background(Color(0xCC000000)).padding(8.dp), maxLines = 3)
        }
        if (info != null) {
            val now = vm.nowOn(info)
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6081A10))))
                    .padding(start = 13.dp, end = 13.dp, bottom = 11.dp, top = 24.dp)
            ) {
                Txt("${info.num} · ${info.name}", F.s(15.sp, C.white, FontWeight.Bold, F.outfit))
                Spacer(Modifier.height(3.dp))
                if (now != null) {
                    Txt("${now.title} · ${hhmm(now.start)} – ${hhmm(now.end)}", F.s(10.sp, C.text))
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(now.progress(), Modifier.fillMaxWidth().height(2.5.dp).clip(RoundedCornerShape(2.dp)))
                } else Txt(vm.catName(info.catId), F.s(8.5.sp, C.text))
            }
        }
    }
}

@Composable
private fun EpgList(vm: Vm, c: Channel?) {
    Column(Modifier.fillMaxSize()) {
        PanelHeader("Programação", "Hoje")
        val nowMs = System.currentTimeMillis()
        // só o que está passando e o que vem depois (um único "AGORA")
        val progs = c?.let { vm.epg[it.id] }.orEmpty().filter { it.end > nowMs }
        if (c == null || progs.isEmpty()) {
            EmptyState(
                if (vm.source is XtreamSource) "Sem programação disponível" else "Guia indisponível para listas M3U",
                if (vm.source is XtreamSource) "Este canal não tem guia (EPG) no seu provedor." else "",
                Modifier.fillMaxWidth()
            )
            return@Column
        }
        progs.take(7).forEachIndexed { i, p ->
            val now = i == 0 && p.start <= nowMs
            Row(
                Modifier.fillMaxWidth().height(if (now) 46.dp else 34.dp).background(if (now) C.cyanSoft.copy(alpha = 0.55f) else Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(if (now) C.cyan else Color.Transparent))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.width(78.dp)) {
                    Txt(hhmm(p.start), F.s(11.sp, if (now) C.cyan else C.dim, FontWeight.SemiBold))
                    if (now) Txt("até ${hhmm(p.end)}", F.s(8.sp, C.dim))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(p.title, F.s(11.5.sp, if (now) C.white else C.text, if (now) FontWeight.SemiBold else FontWeight.Normal), Modifier.weight(1f, fill = false))
                        if (now) {
                            Spacer(Modifier.width(7.dp))
                            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(C.cyan).padding(horizontal = 5.dp, vertical = 1.5.dp)) {
                                Txt("AGORA", F.s(7.sp, Color(0xFF062430), FontWeight.Bold))
                            }
                        }
                    }
                    if (now) {
                        Spacer(Modifier.height(5.dp))
                        ProgressBar(p.progress(nowMs), Modifier.fillMaxWidth(0.85f).height(2.5.dp).clip(RoundedCornerShape(2.dp)))
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

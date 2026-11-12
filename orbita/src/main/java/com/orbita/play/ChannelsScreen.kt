package com.orbita.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FAV = "__fav"
private const val RECENT = "__recent"
private const val ALL = "__all"

/** Canais da categoria escolhida (inclui Favoritos, Recentes e Todos). */
fun channelsOf(vm: Vm, cat: String): List<Channel> = when (cat) {
    FAV -> vm.store.favChannels.mapNotNull { vm.channelById[it] }
    RECENT -> Quality.unique(vm.store.recentChannels.mapNotNull { vm.channelById[it] }).filter { c -> vm.liveCats.firstOrNull { it.id == c.catId }?.let { vm.catAllowed("live", it) } ?: true }
    ALL -> vm.allowedChannels()
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

    val visibleCats = vm.liveCats.filter { vm.catVisible("live", it) }
    val cats = remember(visibleCats, catQuery) {
        if (catQuery.isBlank()) visibleCats else visibleCats.filter { MatchLogic.norm(it.name).contains(MatchLogic.norm(catQuery), true) }
    }
    val favCount = vm.store.favChannels.size
    val recentCount = vm.store.recentChannels.size
    val sortAZ = vm.store.sortAZ.value
    val allowed = vm.allowedChannels()
    val list = remember(ui.liveCat, chQuery, vm.channels, favCount, recentCount, sortAZ, allowed) {
        var l = channelsOf(vm, ui.liveCat)
        if (sortAZ) l = l.sortedBy { MatchLogic.norm(it.name).lowercase() }
        if (chQuery.isBlank()) l else {
            val q = MatchLogic.norm(chQuery)
            l.filter { MatchLogic.norm(it.name).contains(q, true) || it.num.toString() == chQuery.trim() }
        }
    }
    val catTitle = when (ui.liveCat) { FAV -> "Favoritos"; RECENT -> "Recentes"; ALL -> "Todos os canais"; else -> vm.catName(ui.liveCat) }

    LaunchedEffect(ui.liveCat, sortAZ) {
        if (ui.liveCatShown != null && ui.liveCatShown != ui.liveCat + sortAZ) ui.chList.scrollToItem(0)
        ui.liveCatShown = ui.liveCat + sortAZ
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    /** Vai para o primeiro canal da lista (▶ nas categorias, OK numa categoria). */
    fun toFirst() { scope.launch { ui.chList.scrollToItem(0); delay(30); runCatching { firstChannel.requestFocus() } } }
    fun choose(id: String) { ui.liveCat = id; chQuery = ""; toFirst() }

    // Prévia automática ao parar sobre um canal (opcional em Configurações)
    LaunchedEffect(focusedCh?.id) {
        val c = focusedCh ?: return@LaunchedEffect
        vm.requestEpg(c)
        if (vm.store.previewAutoplay.value && playingId != c.id) { delay(1_200); pb.play(liveItem(c), playItems()) }
    }

    Row(Modifier.fillMaxSize().padding(top = 50.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)) {
        // Categorias
        Panel(Modifier.width(196.dp).fillMaxHeight()) {
            Column {
                PanelHeader("Categorias", "${visibleCats.size}")
                SearchField(catQuery, { catQuery = it }, "Buscar categoria", Modifier.padding(horizontal = 10.dp))
                Spacer(Modifier.height(8.dp))
                LazyColumn(state = ui.catList, modifier = Modifier.weight(1f)) {
                    if (catQuery.isBlank()) {
                        item(key = FAV) { CatRow("Favoritos", favCount, ui.liveCat == FAV, "cat:$FAV", {}, catReq, Ic.Star, ::toFirst) { choose(FAV) } }
                        item(key = RECENT) { CatRow("Recentes", recentCount, ui.liveCat == RECENT, "cat:$RECENT", {}, icon = Ic.History, onRight = ::toFirst) { choose(RECENT) } }
                        item(key = ALL) { CatRow("Todos os canais", allowed.size, ui.liveCat == ALL, "cat:$ALL", {}, icon = Ic.LiveTv, onRight = ::toFirst) { choose(ALL) } }
                    }
                    items(cats, key = { "c" + it.id }) { c ->
                        val locked = vm.catLocked(c)
                        val icon = if (locked) Ic.Lock else if (MatchLogic.isSportsName(c.name)) Ic.Soccer else null
                        CatRow(c.name, if (locked) -1 else vm.uniqueByCat[c.id]?.size ?: 0, ui.liveCat == c.id, "cat:${c.id}",
                            {}, icon = icon, onRight = ::toFirst) {
                            if (locked) ui.unlockAdult(vm) { choose(c.id) } else choose(c.id)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        // Canais (com botão de favoritar ao lado)
        Panel(Modifier.width(318.dp).fillMaxHeight()) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt(catTitle.uppercase(), F.label, Modifier.weight(1f))
                    Txt("${list.size}", F.s(8.5.sp, C.dim, FontWeight.SemiBold))
                    Spacer(Modifier.width(8.dp))
                    Focus(onClick = { vm.store.setSortAZ(!sortAZ) }, memKey = "ch:sort") { f ->
                        Row(
                            Modifier.clip(RoundedCornerShape(6.dp)).background(if (f) C.accent else C.chip).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Ic.Sort, 11.dp)
                            Spacer(Modifier.width(4.dp))
                            Txt(if (sortAZ) "A–Z" else "Nº", F.s(8.sp, C.white, FontWeight.Bold))
                        }
                    }
                }
                SearchField(chQuery, { chQuery = it }, "Buscar canal", Modifier.padding(horizontal = 10.dp))
                Spacer(Modifier.height(6.dp))
                if (list.isEmpty()) {
                    EmptyState(
                        if (ui.liveCat == FAV) "Nenhum favorito ainda" else "Nenhum canal",
                        if (ui.liveCat == FAV) "Use a estrela ao lado do canal para favoritar." else "",
                        Modifier.fillMaxWidth()
                    )
                }
                LazyColumn(state = ui.chList, modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    itemsIndexed(list, key = { _, c -> c.id }) { i, c ->
                        val req = when { i == 0 -> firstChannel; i == initialIdx -> initial; else -> null }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                ChannelRow(
                                    vm, c, playing = c.id == playingId, fav = false, requester = req,
                                    onFocus = { focusedCh = c }, onClick = { open(c) }, onLong = { vm.store.toggleFav(c.id) }
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            FavButton(vm.store.isFav(c.id), "fav:${c.id}") { vm.store.toggleFav(c.id) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        // Prévia + guia com dias
        val shown = focusedCh ?: vm.channelById[playingId ?: ""] ?: list.firstOrNull()
        EpgArea(vm, pb, shown) { shown?.let { open(it) } }
    }
}

@Composable
private fun FavButton(fav: Boolean, memKey: String, onClick: () -> Unit) {
    Focus(onClick = onClick, memKey = memKey) { f ->
        val shape = RoundedCornerShape(8.dp)
        Box(
            Modifier.size(34.dp, 44.dp).clip(shape).background(if (f) C.focusBg else Color(0x14FFFFFF))
                .border(if (f) 1.5.dp else 1.dp, if (f) C.accent else Color(0x22FFFFFF), shape),
            contentAlignment = Alignment.Center
        ) { Icon(if (fav) Ic.Star else Ic.StarOutline, 15.dp, if (fav) C.star else if (f) C.white else C.dim) }
    }
}

/** Prévia em cima, guia do dia escolhido embaixo e a coluna de dias à direita. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.EpgArea(vm: Vm, pb: Playback, c: Channel?, onWatch: () -> Unit) {
    LaunchedEffect(c?.id) { val ch = c ?: return@LaunchedEffect; delay(450); vm.requestFullEpg(ch) }
    val progs = c?.let { vm.fullEpg[it.id] ?: vm.epg[it.id] }.orEmpty()
    val days = remember(progs) { (progs.map { dayIndex(it.start) } + 0).filter { it in -3..7 }.distinct().sorted() }
    var day by remember(c?.id) { mutableIntStateOf(0) }
    Column(Modifier.weight(1f).fillMaxHeight()) {
        Preview(vm, pb, c, Modifier.fillMaxWidth().height(188.dp))
        Spacer(Modifier.height(8.dp))
        Panel(Modifier.fillMaxWidth().weight(1f)) { DayProgrammes(vm, c, progs, day, onWatch) }
    }
    Spacer(Modifier.width(8.dp))
    // dias
    Column(Modifier.width(46.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        days.forEach { d ->
            val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, d) }
            val wd = java.text.SimpleDateFormat("EEE", java.util.Locale("pt", "BR")).format(cal.time).replace(".", "").take(3).replaceFirstChar { it.uppercase() }
            Focus(onFocus = { day = d }, onClick = { day = d }, memKey = "day:$d") { f ->
                val sel = d == day
                Column(
                    Modifier.width(46.dp).height(42.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (f) C.accent else if (sel) C.accentSoft else C.panel)
                        .border(1.dp, if (sel && !f) C.accent else C.panelBorder, RoundedCornerShape(8.dp)),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Txt(cal.get(java.util.Calendar.DAY_OF_MONTH).toString(), F.s(12.sp, C.white, FontWeight.Bold, F.outfit))
                    Txt(if (d == 0) "Hoje" else wd, F.s(6.5.sp, if (sel || f) C.white else C.dim, FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun DayProgrammes(vm: Vm, c: Channel?, all: List<Prog>, day: Int, onWatch: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        val label = when (day) { 0 -> "Hoje"; 1 -> "Amanhã"; -1 -> "Ontem"; else -> dayLabel(day) }
        PanelHeader("Programação", label)
        val nowMs = System.currentTimeMillis()
        val progs = all.filter { dayIndex(it.start) == day }
        if (c == null || progs.isEmpty()) {
            EmptyState(
                if (vm.source is XtreamSource) "Sem programação para este dia" else "Guia indisponível para listas M3U",
                "", Modifier.fillMaxWidth()
            )
            return@Column
        }
        val nowIdx = progs.indexOfFirst { it.isNow(nowMs) }
        val state = androidx.compose.foundation.lazy.rememberLazyListState((if (nowIdx > 0) nowIdx - 1 else 0))
        LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(progs, key = { it.start }) { p ->
                val now = p.isNow(nowMs)
                val past = p.end <= nowMs
                Focus(Modifier.fillMaxWidth(), onClick = { if (now) onWatch() }, memKey = "epg:${p.start}") { f ->
                    Row(
                        Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (f) C.focusBg else if (now) C.cyanSoft.copy(alpha = 0.55f) else Color(0x0DFFFFFF))
                            .then(if (f) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(8.dp)) else Modifier)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(
                            when { now -> Color(0xFF22C55E); past -> Color(0xFF4B4762); else -> Color(0xFFD8D6E6) }
                        ))
                        Spacer(Modifier.width(10.dp))
                        Txt("${hhmm(p.start)} – ${hhmm(p.end)}", F.s(9.sp, if (now) C.cyan else if (past) C.faint else C.dim, FontWeight.SemiBold), Modifier.width(84.dp))
                        Txt(p.title, F.s(10.5.sp, if (past) C.dim else C.white, if (now) FontWeight.SemiBold else FontWeight.Medium), Modifier.weight(1f))
                        if (now) {
                            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(C.cyan).padding(horizontal = 5.dp, vertical = 1.5.dp)) {
                                Txt("AGORA", F.s(6.5.sp, Color(0xFF062430), FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CatRow(
    label: String, count: Int, selected: Boolean, memKey: String, onFocus: () -> Unit, requester: FocusRequester? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onRight: (() -> Unit)? = null, onClick: () -> Unit,
) {
    Focus(
        Modifier.fillMaxWidth().height(36.dp), onClick = onClick, onFocus = onFocus, memKey = memKey, requester = requester,
        onKey = if (onRight == null) null else { ev ->
            if (ev.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && ev.key == androidx.compose.ui.input.key.Key.DirectionRight) { onRight(); true } else false
        }
    ) { f ->
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
            if (count >= 0) Count(count, selected)
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
                    val next = vm.epg[info.id]?.firstOrNull { it.start >= now.end }
                    if (next != null) { Spacer(Modifier.height(5.dp)); Txt("${hhmm(next.start)}  ${next.title}", F.s(9.sp, C.dim)) }
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

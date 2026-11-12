package com.orbita.play

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- Login

@Composable
fun LoginScreen(vm: Vm) {
    var xtream by remember { mutableStateOf(true) }
    var server by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var m3u by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val serverReq = remember { FocusRequester() }
    val userReq = remember { FocusRequester() }
    val passReq = remember { FocusRequester() }
    val m3uReq = remember { FocusRequester() }
    val enterReq = remember { FocusRequester() }
    val tabReq = remember { FocusRequester() }
    InitialFocus(tabReq)

    fun submit() {
        if (busy) return
        error = null
        val acc = if (xtream) {
            if (server.isBlank() || user.isBlank() || pass.isBlank()) { error = "Preencha servidor, usuário e senha."; return }
            Account("xtream", Net.normalizeServer(server), user.trim(), pass.trim(), "")
        } else {
            if (m3u.isBlank()) { error = "Informe o endereço da lista M3U."; return }
            val url = m3u.trim().let { if (it.startsWith("http", true)) it else "http://$it" }
            val x = M3u.xtreamFromUrl(url)
            if (x != null) Account("xtream", Net.normalizeServer(x.first), x.second, x.third, url) else Account("m3u", "", "", "", url)
        }
        busy = true
        scope.launch {
            runCatching { vm.login(acc) }.onFailure { error = it.message ?: "Não foi possível conectar." }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF050409))) {
        Orbits()
        Row(Modifier.fillMaxSize().padding(horizontal = 50.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Logo(size = 22f)
                Spacer(Modifier.height(14.dp))
                Txt("Toda a sua TV,", F.s(17.sp, C.white, FontWeight.Medium, F.outfit))
                Txt("em uma só órbita.", F.s(17.sp, C.white, FontWeight.Medium, F.outfit))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    listOf("Canais ao vivo", "Filmes e séries", "Guia EPG", "Futebol").forEach { Txt("•  $it", F.s(8.5.sp, C.dim)) }
                }
            }
            Column(
                Modifier.width(330.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0E0B1A)).border(1.dp, Color(0xFF221A3A), RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Txt("Conectar lista", F.s(14.sp, C.white, FontWeight.Bold, F.outfit))
                Txt("Use os dados enviados pelo seu provedor.", F.s(8.sp, C.dim))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(Color(0xFF08060F)).padding(3.dp)) {
                    LoginTab("Xtream Codes", xtream, Modifier.weight(1f), tabReq) { xtream = true }
                    LoginTab("Lista M3U", !xtream, Modifier.weight(1f)) { xtream = false }
                }
                Spacer(Modifier.height(12.dp))
                if (xtream) {
                    FormField("Servidor", server, { server = it }, "http://servidor.exemplo:8080", keyboard = KeyboardType.Uri, requester = serverReq) { runCatching { userReq.requestFocus() } }
                    Spacer(Modifier.height(8.dp))
                    FormField("Usuário", user, { user = it }, "seu usuário", requester = userReq) { runCatching { passReq.requestFocus() } }
                    Spacer(Modifier.height(8.dp))
                    FormField("Senha", pass, { pass = it }, "••••••••", password = true, requester = passReq) { runCatching { enterReq.requestFocus() } }
                } else {
                    FormField("Endereço da lista (URL)", m3u, { m3u = it }, "http://servidor/get.php?username=…", keyboard = KeyboardType.Uri, requester = m3uReq) { runCatching { enterReq.requestFocus() } }
                    Spacer(Modifier.height(6.dp))
                    Txt("Listas get.php do Xtream são convertidas automaticamente para login com guia (EPG).", F.s(7.sp, C.faint), maxLines = 2)
                }
                Spacer(Modifier.height(14.dp))
                Focus(Modifier.fillMaxWidth(), onClick = { submit() }, requester = enterReq) { f ->
                    Box(
                        Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(7.dp)).background(if (f) Color(0xFFA5E6FF) else Color(0xFF6CD4FF))
                            .then(if (f) Modifier.border(2.dp, C.white, RoundedCornerShape(7.dp)) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (busy) Txt("Conectando…", F.s(9.5.sp, Color(0xFF06223A), FontWeight.Bold))
                        else Txt("Entrar  →", F.s(9.5.sp, Color(0xFF06223A), FontWeight.Bold))
                    }
                }
                error?.let { Spacer(Modifier.height(8.dp)); Txt(it, F.s(8.sp, C.live, FontWeight.Medium), maxLines = 3) }
                Spacer(Modifier.height(10.dp))
                Row {
                    Txt("Sem conta? Fale com seu provedor.", F.s(7.5.sp, C.dim), Modifier.weight(1f))
                    Txt("Ajuda: OK abre o teclado", F.s(7.5.sp, Color(0xFF6CD4FF)))
                }
            }
        }
        if (busy) Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) { Loading("Conectando ao servidor…") }
    }
}

@Composable
private fun LoginTab(text: String, selected: Boolean, modifier: Modifier, requester: FocusRequester? = null, onClick: () -> Unit) {
    Focus(modifier, onClick = onClick, onFocus = onClick, requester = requester) { f ->
        Box(
            Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(5.dp))
                .background(if (selected) Color(0xFF2A1D5E) else Color.Transparent)
                .then(if (f) Modifier.border(1.5.dp, Color(0xFF6CD4FF), RoundedCornerShape(5.dp)) else Modifier),
            contentAlignment = Alignment.Center
        ) { Txt(text, F.s(8.5.sp, if (selected) C.white else C.dim, FontWeight.SemiBold)) }
    }
}

/** Elipses de órbita do fundo da tela de login. */
@Composable
private fun Orbits() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width * 0.28f, size.height * 0.55f)
        val line = Color(0x337C5CFF)
        for (i in 0 until 4) {
            val w = size.width * (0.55f + i * 0.22f); val h = size.height * (0.32f + i * 0.14f)
            rotate(-18f, c) { drawOval(line, Offset(c.x - w / 2, c.y - h / 2), Size(w, h), style = Stroke(1.dp.toPx())) }
        }
        listOf(0.12f to 0.2f, 0.42f to 0.85f, 0.7f to 0.1f, 0.9f to 0.75f, 0.06f to 0.7f).forEach { (x, y) ->
            drawCircle(Color(0x88FFFFFF), 1.5.dp.toPx(), Offset(size.width * x, size.height * y))
        }
    }
}

// ---------------------------------------------------------------- Buscar

private class Results(val channels: List<Channel>, val movies: List<Movie>, val series: List<Series>)

@Composable
fun SearchScreen(vm: Vm, pb: Playback, ui: Ui) {
    ScreenBackground()
    var res by remember { mutableStateOf<Results?>(null) }
    LaunchedEffect(ui.query, vm.channels, vm.movies, vm.series) {
        val q = MatchLogic.norm(ui.query).lowercase()
        if (q.length < 2) { res = null; return@LaunchedEffect }
        delay(250)
        res = withContext(Dispatchers.Default) {
            fun hit(n: String) = MatchLogic.norm(n).lowercase().contains(q)
            Results(
                vm.uniqueChannels.asSequence().filter { hit(it.name) || it.num.toString() == q }.take(40).toList(),
                vm.movies.asSequence().filter { hit(it.name) }.take(60).toList(),
                vm.series.asSequence().filter { hit(it.name) }.take(60).toList(),
            )
        }
    }
    val fieldReq = remember { FocusRequester() }
    InitialFocus(fieldReq)
    Column(Modifier.fillMaxSize().padding(top = 56.dp)) {
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt("Buscar", F.s(18.sp, C.white, FontWeight.Bold, F.outfit))
            Spacer(Modifier.width(16.dp))
            SearchField(ui.query, { ui.query = it }, "Digite o nome do canal, filme ou série (OK abre o teclado)", Modifier.width(500.dp), fieldReq)
        }
        Spacer(Modifier.height(10.dp))
        val r = res
        when {
            ui.query.length < 2 -> EmptyState("O que você quer assistir?", "Busca em todos os canais, filmes e séries da sua lista.", Modifier.fillMaxSize())
            r == null -> Loading("Buscando…", Modifier.fillMaxSize())
            r.channels.isEmpty() && r.movies.isEmpty() && r.series.isEmpty() -> EmptyState("Nada encontrado para \"${ui.query}\"", "", Modifier.fillMaxSize())
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
                if (r.channels.isNotEmpty()) item(key = "ch") {
                    SectionTitle("Canais (${r.channels.size})")
                    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(r.channels, key = { it.id }) { c ->
                            ChannelCard(vm, c) { pb.openFull(liveItem(c), r.channels.map { liveItem(it) }) }
                        }
                    }
                }
                if (r.movies.isNotEmpty()) item(key = "mv") {
                    SectionTitle("Filmes (${r.movies.size})")
                    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        items(r.movies, key = { it.id }) { m ->
                            LaunchedEffect(m.id) { delay(200); vm.requestMeta(m) }
                            Poster(m.name, vm.metaOf(m)?.posterUrl() ?: m.poster, rating = m.rating, memKey = "s:mv:${m.id}") { ui.push(Route.MovieDetail(m, FocusMem.last)) }
                        }
                    }
                }
                if (r.series.isNotEmpty()) item(key = "se") {
                    SectionTitle("Séries (${r.series.size})")
                    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        items(r.series, key = { it.id }) { s ->
                            LaunchedEffect(s.id) { delay(200); vm.requestMeta(s) }
                            Poster(s.name, vm.metaOf(s)?.posterUrl() ?: s.cover, rating = s.rating, memKey = "s:se:${s.id}") { ui.push(Route.SeriesDetail(s, FocusMem.last)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) = Txt(t, F.s(11.sp, C.white, FontWeight.SemiBold), Modifier.padding(start = 24.dp, top = 12.dp, bottom = 7.dp))

@Composable
private fun ChannelCard(vm: Vm, c: Channel, onClick: () -> Unit) {
    Focus(Modifier.width(130.dp), onClick = onClick, onLongClick = { vm.store.toggleFav(c.id) }, memKey = "card:${c.id}") { f ->
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (f) C.focusBg else C.item)
                .then(if (f) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(8.dp)) else Modifier).padding(8.dp)
        ) {
            ChannelLogo(c, Modifier.fillMaxWidth().height(50.dp))
            Spacer(Modifier.height(5.dp))
            Txt("${c.num} · ${c.name}", F.s(8.5.sp, C.white, FontWeight.SemiBold))
            Txt(vm.catName(c.catId), F.s(7.sp, C.dim))
        }
    }
}

// ---------------------------------------------------------------- Biblioteca

private val libMenu = listOf("Canais favoritos", "Filmes na minha lista", "Séries na minha lista", "Continuar assistindo", "Canais recentes")

@Composable
fun LibraryScreen(vm: Vm, pb: Playback, ui: Ui) {
    ScreenBackground()
    val first = remember { FocusRequester() }
    val menuReq = remember { FocusRequester() }
    InitialFocus(menuReq)
    Column(Modifier.fillMaxSize().padding(top = 50.dp)) {
        Row(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            Panel(Modifier.width(200.dp).fillMaxHeight()) {
                Column {
                    PanelHeader("Biblioteca")
                    libMenu.forEachIndexed { i, l ->
                        val n = when (i) {
                            0 -> vm.store.favChannels.size
                            1 -> vm.store.myList.count { it.startsWith("m:") }
                            2 -> vm.store.myList.count { it.startsWith("s:") }
                            3 -> vm.store.continueWatching().size
                            else -> vm.store.recentChannels.size
                        }
                        Focus(Modifier.fillMaxWidth().height(28.5.dp), onClick = { runCatching { first.requestFocus() } }, onFocus = { ui.libraryMenu = i },
                            requester = if (i == ui.libraryMenu) menuReq else null, memKey = "lib:$i") { f ->
                            val sel = ui.libraryMenu == i
                            Row(
                                Modifier.fillMaxSize().background(if (f) C.focusBg else if (sel) C.accentSoft else Color.Transparent)
                                    .then(if (f) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(4.dp)) else Modifier),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.width(2.5.dp).fillMaxHeight().background(if (sel) C.accent else Color.Transparent))
                                Spacer(Modifier.width(12.dp))
                                Txt(l, F.s(9.5.sp, if (sel || f) C.white else C.text), Modifier.weight(1f))
                                Count(n, sel)
                                Spacer(Modifier.width(10.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize()) {
                    PanelHeader(libMenu[ui.libraryMenu])
                    when (ui.libraryMenu) {
                        0, 4 -> {
                            val list = (if (ui.libraryMenu == 0) vm.store.favChannels else vm.store.recentChannels).mapNotNull { vm.channelById[it] }
                            if (list.isEmpty()) EmptyState(if (ui.libraryMenu == 0) "Nenhum canal favorito" else "Nenhum canal assistido ainda", if (ui.libraryMenu == 0) "Em Canais, segure OK sobre um canal para favoritar." else "", Modifier.fillMaxSize())
                            else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
                                itemsIndexed(list, key = { _, c -> c.id }) { i, c ->
                                    ChannelRow(vm, c, pb.current?.channel?.id == c.id, vm.store.isFav(c.id), if (i == 0) first else null, {}, {
                                        pb.openFull(liveItem(c), list.map { liveItem(it) })
                                    }, { vm.store.toggleFav(c.id) })
                                }
                            }
                        }
                        1, 2 -> {
                            val movies = ui.libraryMenu == 1
                            val entries = vm.store.myList.filter { it.startsWith(if (movies) "m:" else "s:") }.mapNotNull { k ->
                                val id = k.substring(2)
                                if (movies) vm.movieById[id]?.let { m -> VodEntry(m.id, m.name, m.poster, m.rating, { ui.push(Route.MovieDetail(m, FocusMem.last)) }, { vm.store.toggleList(k) }) }
                                else vm.seriesById[id]?.let { s -> VodEntry(s.id, s.name, s.cover, s.rating, { ui.push(Route.SeriesDetail(s, FocusMem.last)) }, { vm.store.toggleList(k) }) }
                            }
                            if (entries.isEmpty()) EmptyState("Sua lista está vazia", "Use \"+ Minha lista\" ou segure OK sobre um título.", Modifier.fillMaxSize())
                            else LazyVerticalGrid(
                                GridCells.Fixed(7), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(entries, key = { _, e -> e.key }) { i, e ->
                                    Poster(e.title, e.image, rating = e.rating, requester = if (i == 0) first else null, memKey = "lib:${e.key}", onLongClick = e.longPress) { e.open() }
                                }
                            }
                        }
                        else -> {
                            val cont = vm.store.continueWatching()
                            if (cont.isEmpty()) EmptyState("Nada em andamento", "Filmes e episódios que você começar aparecem aqui.", Modifier.fillMaxSize())
                            else LazyVerticalGrid(
                                GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(cont, key = { _, p -> p.key }) { i, p ->
                                    WideCard(
                                        p.title, p.image, p.fraction,
                                        if (p.kind == Kind.EPISODE) "${p.subtitle} · faltam ${p.remainingMin} min" else "Faltam ${p.remainingMin} min",
                                        requester = if (i == 0) first else null, memKey = "lib:${p.key}"
                                    ) {
                                        val s = p.seriesId?.let { vm.seriesById[it] }
                                        if (s != null) ui.push(Route.SeriesDetail(s, FocusMem.last))
                                        else pb.openFull(PlayItem(Kind.MOVIE, p.key, p.title, p.subtitle, vm.movieById[p.key.removePrefix("mv:")]?.url ?: p.url, p.image), startMs = p.pos)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        HintBar(listOf("OK" to "Abrir", "★" to "Remover (segurar OK)", "VOLTAR" to "Menu"), Modifier.height(30.dp).fillMaxWidth().padding(top = 8.dp))
    }
}

// ---------------------------------------------------------------- Configurações

@Composable
fun SettingsScreen(vm: Vm, pb: Playback, ui: Ui) {
    ScreenBackground()
    val first = remember { FocusRequester() }
    InitialFocus(first)
    var confirmLogout by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { if (msg != null) { delay(2500); msg = null } }
    val a = vm.account
    val st = vm.status
    Row(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 30.dp)) {
        Column(Modifier.width(320.dp)) {
            Logo(size = 16f)
            Spacer(Modifier.height(18.dp))
            Txt("Configurações", F.s(18.sp, C.white, FontWeight.Bold, F.outfit))
            Spacer(Modifier.height(14.dp))
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Txt("CONTA", F.label)
                    InfoLine("Tipo", if (a?.type == "xtream") "Xtream Codes" else "Lista M3U")
                    if (a?.type == "xtream") {
                        InfoLine("Servidor", a.server)
                        InfoLine("Usuário", a.user)
                    } else InfoLine("Lista", a?.m3uUrl.orEmpty())
                    st?.status?.let { InfoLine("Status", if (it.equals("Active", true)) "Ativa" else it) }
                    st?.expires?.let { InfoLine("Validade", SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(it))) }
                    st?.maxConnections?.let { InfoLine("Conexões", "${st.activeConnections ?: "0"} de $it em uso") }
                    InfoLine("Conteúdo", "${fmtCount(vm.channels.size)} canais · ${fmtCount(vm.movies.size)} filmes · ${fmtCount(vm.series.size)} séries")
                }
            }
            Spacer(Modifier.height(10.dp))
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
            Txt("Orbita Play $ver · O app não fornece conteúdo: use apenas listas que você tem direito de usar.", F.s(7.5.sp, C.faint), maxLines = 3)
        }
        Spacer(Modifier.width(30.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Spacer(Modifier.height(50.dp))
            SettingRow("Formato dos canais ao vivo", if (vm.store.liveFormat.value == "m3u8") "HLS (.m3u8)" else "MPEG-TS (.ts)", first) {
                vm.store.setLiveFormat(if (vm.store.liveFormat.value == "m3u8") "ts" else "m3u8")
                vm.reloadAll(); msg = "Formato alterado. Recarregando canais…"
            }
            SettingRow("Próximo episódio automático", if (vm.store.autoNext.value) "Ligado" else "Desligado") { vm.store.setAutoNext(!vm.store.autoNext.value) }
            SettingRow("Prévia automática ao navegar nos canais", if (vm.store.previewAutoplay.value) "Ligada" else "Desligada") { vm.store.setPreviewAutoplay(!vm.store.previewAutoplay.value) }
            SettingRow("Recarregar listas", "Canais, filmes e séries") { vm.reloadAll(); msg = "Recarregando listas…" }
            SettingRow("Limpar histórico", "Recentes e continuar assistindo") { vm.store.clearHistory(); msg = "Histórico apagado." }
            SettingRow(if (confirmLogout) "Confirmar: sair da conta?" else "Sair da conta", if (confirmLogout) "Pressione OK de novo" else "Trocar de lista ou provedor", danger = true) {
                if (confirmLogout) { pb.stop(); ui.stack.clear(); ui.tab = Tab.CANAIS; vm.logout() } else confirmLogout = true
            }
            msg?.let { Txt(it, F.s(9.sp, C.cyan, FontWeight.Medium), Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(10.dp))
            Txt("Controle remoto: OK seleciona · segurar OK favorita · VOLTAR volta · MENU no player abre áudio/legenda · 0–9 digita o número do canal", F.s(7.5.sp, C.faint), maxLines = 3)
        }
    }
}

@Composable
private fun InfoLine(k: String, v: String) {
    Row {
        Txt(k, F.s(8.5.sp, C.dim), Modifier.width(70.dp))
        Txt(v, F.s(8.5.sp, C.white, FontWeight.Medium), maxLines = 2)
    }
}

@Composable
private fun SettingRow(label: String, value: String, requester: FocusRequester? = null, danger: Boolean = false, onClick: () -> Unit) {
    Focus(Modifier.fillMaxWidth(), onClick = onClick, requester = requester) { f ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (f) C.focusBg else C.panel)
                .border(if (f) 1.5.dp else 1.dp, if (f) C.accent else C.panelBorder, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt(label, F.s(10.sp, if (danger) C.live else C.white, FontWeight.SemiBold), Modifier.weight(1f))
            Txt(value, F.s(8.5.sp, C.cyan))
        }
    }
}


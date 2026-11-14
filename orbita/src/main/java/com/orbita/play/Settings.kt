package com.orbita.play

import kotlinx.coroutines.launch
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class Section(val key: String, val title: String, val icon: ImageVector, val desc: String)
private class Group(val title: String, val items: List<Section>)

private val groups = listOf(
    Group("CONTA", listOf(Section("info", "Informações gerais", Ic.Info, "Sua conta, assinatura e os dados deste aparelho."))),
    Group("REPRODUÇÃO", listOf(
        Section("format", "Formato de fluxo", Ic.LiveTv, "Como os canais ao vivo são abertos."),
        Section("play", "Reprodução", Ic.Play, "Comportamento do player."),
    )),
    Group("PRIVACIDADE", listOf(
        Section("parental", "Controle parental", Ic.Lock, "Bloqueie ou oculte o conteúdo adulto."),
        Section("pin", "Alterar senha", Ic.Key, "Senha do controle parental."),
        Section("hide", "Ocultar categorias", Ic.EyeOff, "Escolha o que aparece nas listas."),
    )),
    Group("APARÊNCIA", listOf(
        Section("wall", "Papel de parede", Ic.Grid, "Fundo das telas do app."),
        Section("layout", "Layout da tela inicial", Ic.Home, "Escolha o visual da tela inicial."),
    )),
    Group("SISTEMA", listOf(
        Section("update", "Atualizações", Ic.New, "Versão do app e novas atualizações."),
        Section("storage", "Armazenamento", Ic.Refresh, "Recarregar listas e limpar dados guardados."),
        Section("logout", "Sair da conta", Ic.Logout, "Remover a lista deste aparelho."),
    )),
)

/** Pedido de confirmação mostrado em janela por cima das configurações. */
class Ask(val title: String, val message: String, val confirm: String, val icon: ImageVector, val danger: Boolean = false, val action: () -> Unit)

/** Configurações: menu por grupos à esquerda e o conteúdo da seção em quadros à direita. */
@Composable
fun SettingsScreen(vm: Vm, pb: Playback, ui: Ui) {
    var key by remember { mutableStateOf("info") }
    var msg by remember { mutableStateOf<String?>(null) }
    var ask by remember { mutableStateOf<Ask?>(null) }
    LaunchedEffect(msg) { if (msg != null) { delay(2500); msg = null } }
    val first = remember { FocusRequester() }
    InitialFocus(first)
    val all = groups.flatMap { it.items }
    val cur = all.first { it.key == key }
    val confirm: (Ask) -> Unit = { ask = it }

    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Column(Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, top = 22.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Logo(size = 12f)
                Box(Modifier.padding(horizontal = 14.dp).width(1.dp).height(18.dp).background(C.panelBorder))
                Txt("Configurações", F.s(18.sp, C.white, FontWeight.Bold, F.outfit))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.weight(1f)) {
                // menu por grupos
                Column(
                    Modifier.width(250.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(C.panel)
                        .border(1.dp, C.panelBorder, RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    groups.forEach { g ->
                        Txt(g.title, F.s(7.5.sp, C.faint, FontWeight.Bold, spacing = 1.4.sp), Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp))
                        g.items.forEach { s ->
                            Focus(Modifier.fillMaxWidth(), onClick = { key = s.key }, requester = if (s.key == "info") first else null, memKey = "set:${s.key}") { f ->
                                val sel = key == s.key
                                val danger = s.key == "logout"
                                Row(
                                    Modifier.fillMaxWidth().height(33.dp).clip(RoundedCornerShape(9.dp))
                                        .background(if (f) C.accent else if (sel) C.accentSoft else Color.Transparent)
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(if (sel && !f) C.accent else Color.Transparent))
                                    Spacer(Modifier.width(7.dp))
                                    Icon(s.icon, 15.dp, if (f) C.white else if (danger) C.live else if (sel) C.white else C.dim)
                                    Spacer(Modifier.width(10.dp))
                                    Txt(s.title, F.s(11.sp, if (f) C.white else if (danger) C.live else if (sel) C.white else C.text, FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                // conteúdo
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(C.accentSoft), contentAlignment = Alignment.Center) {
                            Icon(cur.icon, 17.dp, C.accentText)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Txt(cur.title, F.s(15.sp, C.white, FontWeight.Bold, F.outfit))
                            Txt(cur.desc, F.s(8.5.sp, C.dim))
                        }
                        msg?.let {
                            Row(
                                Modifier.clip(RoundedCornerShape(15.dp)).background(Color(0xFF163A2A)).padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) { Icon(Ic.Check, 11.dp, Color(0xFF7EE2A8)); Spacer(Modifier.width(5.dp)); Txt(it, F.s(8.5.sp, Color(0xFF7EE2A8), FontWeight.SemiBold)) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth().weight(1f)) {
                        when (key) {
                            "info" -> GeneralInfo(vm)
                            "format" -> StreamFormat(vm, confirm) { msg = it }
                            "parental" -> Parental(vm, ui) { msg = it }
                            "pin" -> ChangePin(vm, ui) { msg = it }
                            "hide" -> HideCategories(vm)
                            "play" -> PlaybackPrefs(vm)
                            "storage" -> Storage(vm, ui, confirm) { msg = it }
                            "update" -> UpdatesCard()
                            "wall" -> Card { WallpaperPicker(vm) }
                            "layout" -> HomeLayoutPicker(vm)
                            else -> Logout(vm, pb, ui, confirm)
                        }
                    }
                }
            }
        }
        ask?.let { q ->
            ConfirmDialog(q.title, q.message, q.confirm, onConfirm = { ask = null; q.action() }, onCancel = { ask = null }, icon = q.icon, danger = q.danger)
        }
    }
}

/** Quadro com título opcional (agrupa as opções de cada seção). */
@Composable
private fun Card(title: String? = null, modifier: Modifier = Modifier.fillMaxWidth(), hint: String? = null, content: @Composable () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(C.panel).border(1.dp, C.panelBorder, RoundedCornerShape(12.dp)).padding(14.dp)
    ) {
        if (title != null) { Txt(title, F.s(7.5.sp, C.faint, FontWeight.SemiBold, spacing = 1.2.sp)); Spacer(Modifier.height(9.dp)) }
        content()
        if (hint != null) { Spacer(Modifier.height(9.dp)); Txt(hint, F.s(8.sp, C.faint), maxLines = 2) }
    }
}

@Composable
private fun Option(
    label: String, value: String? = null, checked: Boolean? = null, radio: Boolean? = null, danger: Boolean = false, memKey: String,
    sub: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit,
) {
    Focus(Modifier.fillMaxWidth(), onClick = onClick, memKey = memKey) { f ->
        Row(
            Modifier.fillMaxWidth().height(if (sub != null) 50.dp else 42.dp).clip(RoundedCornerShape(10.dp)).background(if (f) C.focusBg else C.item)
                .border(if (f) 1.5.dp else 1.dp, if (f) C.accent else C.panelBorder, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (radio != null) {
                Box(Modifier.size(17.dp).clip(CircleShape).border(2.dp, if (radio) C.accent else C.dim, CircleShape), contentAlignment = Alignment.Center) {
                    if (radio) Box(Modifier.size(9.dp).clip(CircleShape).background(C.accent))
                }
                Spacer(Modifier.width(12.dp))
            }
            if (icon != null) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(if (danger) C.live.copy(alpha = 0.15f) else C.accentSoft), contentAlignment = Alignment.Center) {
                    Icon(icon, 14.dp, if (danger) C.live else C.accentText)
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Txt(label, F.s(11.sp, if (danger) C.live else C.white, FontWeight.SemiBold))
                sub?.let { Spacer(Modifier.height(2.dp)); Txt(it, F.s(8.5.sp, C.dim)) }
            }
            value?.let { Txt(it, F.s(9.sp, C.dim, FontWeight.Medium), Modifier.padding(end = if (checked != null) 12.dp else 0.dp)) }
            checked?.let { Switch(it) }
        }
    }
}

@Composable
private fun Line(k: String, v: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(k, F.s(9.sp, C.dim), Modifier.width(78.dp))
        Txt(v, F.s(9.5.sp, C.white, FontWeight.Medium), maxLines = 1)
    }
}

@Composable
private fun BigValue(label: String, value: String, spacing: Float = 0f) {
    Column(Modifier.padding(vertical = 3.dp)) {
        Txt(label, F.s(7.sp, C.faint, FontWeight.SemiBold, spacing = 1.sp))
        Txt(value, F.s(15.sp, C.white, FontWeight.Bold, F.outfit, spacing.sp))
    }
}

@Composable
private fun InfoTile(label: String, value: String, icon: ImageVector, modifier: Modifier, mono: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xE60C0A16)).border(1.dp, C.panelBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(C.accentSoft), contentAlignment = Alignment.Center) { Icon(icon, 12.dp, C.accentText) }
            Spacer(Modifier.width(8.dp))
            Txt(label, F.s(7.5.sp, C.faint, FontWeight.SemiBold, spacing = 1.2.sp))
        }
        Spacer(Modifier.height(8.dp))
        Txt(value, F.s(if (mono) 15.sp else 15.sp, C.white, FontWeight.Bold, F.outfit, if (mono) 1.5.sp else 0.sp), maxLines = 1)
    }
}

/** Informações gerais: usuário, MAC e chave em destaque; assinatura e app embaixo. */
@Composable
private fun GeneralInfo(vm: Vm) {
    val a = vm.account
    val st = vm.status
    val ctx = LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
    val active = st?.status == null || st.status.equals("Active", true)
    val user = if (a?.type == "xtream") a.user else "Lista M3U"
    val playlist = vm.store.playlists.firstOrNull { p -> a?.let { p.same(it) } == true }?.name
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("USUÁRIO", user, Ic.Key, Modifier.weight(1f))
            InfoTile("ENDEREÇO MAC", Activation.mac(ctx), Ic.Info, Modifier.weight(1.25f), mono = true)
            InfoTile("CHAVE DO APARELHO", Activation.key(ctx), Ic.Lock, Modifier.weight(1f), mono = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card("ASSINATURA", Modifier.weight(1f).height(124.dp)) {
                val c = if (active) Color(0xFF7EE2A8) else C.live
                Row(Modifier.clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(5.dp))
                    Txt(if (active) "Ativa" else st?.status.orEmpty(), F.s(8.5.sp, c, FontWeight.SemiBold))
                }
                Spacer(Modifier.height(6.dp))
                Row {
                    Box(Modifier.weight(1f)) { BigValue("VALIDADE", st?.expires?.let { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(it)) } ?: "—") }
                    Box(Modifier.weight(1f)) { BigValue("CONEXÕES", st?.maxConnections?.let { "${st.activeConnections ?: "0"} de $it" } ?: "—") }
                }
            }
            Card("APLICATIVO", Modifier.weight(1f).height(124.dp)) {
                Line("Versão", "Orbita Play $ver")
                Line("Playlist", playlist ?: "—")
                if (a?.type == "xtream") Line("Servidor", a.server.removePrefix("http://").removePrefix("https://"))
                if (Activation.enabled()) Line("Site", Activation.site.removePrefix("https://").removePrefix("http://"))
            }
        }
        Txt("O app não fornece conteúdo: use apenas listas que você tem direito de usar.", F.s(8.sp, C.faint), maxLines = 2)
    }
}

@Composable
private fun StreamFormat(vm: Vm, confirm: (Ask) -> Unit, msg: (String) -> Unit) {
    val f = vm.store.liveFormat.value
    fun set(v: String) {
        if (v == f) return
        confirm(Ask("Trocar o formato?", "Os canais vão ser recarregados com o novo formato.", "Trocar", Ic.Refresh) {
            vm.store.setLiveFormat(v); vm.reloadAll(); msg("Formato alterado")
        })
    }
    Card("CANAIS AO VIVO", hint = "Use HLS se algum canal não abrir ou ficar travando no formato TS.") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Option("MPEG-TS (.ts)", sub = "Padrão, mais rápido", radio = f != "m3u8", memKey = "fmt:ts") { set("ts") }
            Option("HLS (.m3u8)", sub = "Mais compatível", radio = f == "m3u8", memKey = "fmt:hls") { set("m3u8") }
        }
    }
}

@Composable
private fun Parental(vm: Vm, ui: Ui, msg: (String) -> Unit) {
    val mode = vm.store.adultMode.value
    fun set(m: String) {
        if (m == mode) return
        // sair do bloqueio pede a senha
        if (mode == "lock" || mode == "hide") {
            ui.pinPrompt = PinPrompt("Confirme a senha", "Necessária para liberar o conteúdo adulto.") { pin ->
                if (pin == vm.store.pin()) { vm.store.setAdultMode(m); vm.adultUnlocked = false; msg("Alterado."); null } else "Senha incorreta"
            }
        } else { vm.store.setAdultMode(m); vm.adultUnlocked = false; msg("Alterado.") }
    }
    Card("CANAIS, FILMES E SÉRIES ADULTOS", hint = "As categorias adultas são reconhecidas pelo nome (Adultos, XXX, +18…). Senha padrão: 0000.") { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Option("Bloquear com senha", radio = mode == "lock", memKey = "adult:lock") { set("lock") }
        Option("Ocultar", radio = mode == "hide", memKey = "adult:hide") { set("hide") }
        Option("Mostrar", radio = mode == "show", memKey = "adult:show") { set("show") }
        if (mode == "lock" && vm.adultUnlocked) {
            Spacer(Modifier.height(4.dp))
            Option("Bloquear de novo agora", memKey = "adult:relock") { vm.adultUnlocked = false; msg("Conteúdo adulto bloqueado.") }
        }
    } }
}

@Composable
private fun ChangePin(vm: Vm, ui: Ui, msg: (String) -> Unit) {
    Card("SENHA DO CONTROLE PARENTAL", hint = "A senha protege o conteúdo adulto e as mudanças no controle parental.") {
    Option("Alterar senha", icon = Ic.Key, sub = "4 números", memKey = "pin:change") {
        ui.pinPrompt = PinPrompt("Senha atual", "Digite a senha atual (padrão 0000).") { cur ->
            if (cur != vm.store.pin()) "Senha incorreta" else {
                ui.pinPrompt = PinPrompt("Nova senha", "Digite a nova senha de 4 números.") { new ->
                    ui.pinPrompt = PinPrompt("Confirme a nova senha", "") { again ->
                        if (again == new) { vm.store.setPin(new); msg("Senha alterada."); null } else "As senhas não conferem"
                    }
                    null
                }
                null
            }
        }
    }
    }
}

@Composable
private fun HideCategories(vm: Vm) {
    var tab by remember { mutableIntStateOf(0) }
    val kinds = listOf("live" to "TV", "vod" to "FILMES", "series" to "SÉRIES")
    Card(modifier = Modifier.fillMaxSize()) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        kinds.forEachIndexed { i, (_, label) -> ChipPill(label, tab == i, onFocus = { tab = i }) { tab = i } }
    }
    Spacer(Modifier.height(10.dp))
    val kind = kinds[tab].first
    val cats = when (tab) { 0 -> vm.liveCats; 1 -> vm.movieCats; else -> vm.seriesCats }
    if (cats.isEmpty()) { Txt("Nenhuma categoria carregada.", F.s(9.sp, C.dim)); return@Card }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items(cats, key = { kind + it.id }) { c ->
            val visible = !vm.store.isHidden("$kind:${c.id}")
            Option(c.name, checked = visible, memKey = "hide:$kind:${c.id}") { vm.store.toggleHidden("$kind:${c.id}") }
        }
    }
    }
}

@Composable
private fun PlaybackPrefs(vm: Vm) {
    Card("PLAYER") { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Option("Próximo episódio automático", checked = vm.store.autoNext.value, memKey = "pl:next") { vm.store.setAutoNext(!vm.store.autoNext.value) }
        Option("Prévia automática nos canais", checked = vm.store.previewAutoplay.value, memKey = "pl:preview") { vm.store.setPreviewAutoplay(!vm.store.previewAutoplay.value) }
    } }
}

@Composable
private fun Storage(vm: Vm, ui: Ui, confirm: (Ask) -> Unit, msg: (String) -> Unit) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card("LISTAS") {
            Option("Recarregar listas", icon = Ic.Refresh, sub = "Canais, filmes e séries", memKey = "st:reload") {
                confirm(Ask("Recarregar as listas?", "Canais, filmes e séries são baixados de novo do seu provedor.", "Recarregar", Ic.Refresh) { vm.reloadAll(); ui.reloading = 1 })
            }
        }
        Card("DADOS GUARDADOS NESTE APARELHO") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Option("Limpar histórico", icon = Ic.History, sub = "Recentes e continuar assistindo", memKey = "st:history") {
                    confirm(Ask("Limpar o histórico?", "Os canais recentes e o \"Continuar assistindo\" serão apagados.", "Limpar", Ic.Refresh, danger = true) { vm.store.clearHistory(); msg("Histórico apagado") })
                }
                Option("Limpar imagens guardadas", icon = Ic.Grid, sub = "Capas e logos", memKey = "st:images") {
                    confirm(Ask("Limpar as imagens?", "Capas e logos serão baixadas de novo quando aparecerem.", "Limpar", Ic.Refresh, danger = true) {
                        runCatching { coil.Coil.imageLoader(ctx).let { it.memoryCache?.clear(); it.diskCache?.clear() } }
                        msg("Imagens apagadas")
                    })
                }
            }
        }
    }
}

@Composable
private fun Logout(vm: Vm, pb: Playback, ui: Ui, confirm: (Ask) -> Unit) {
    Card("SAIR DA CONTA", hint = "Favoritos, histórico e preferências deste aparelho também serão apagados. O MAC e a chave continuam os mesmos.") {
        Txt("Remove a lista deste aparelho e volta para a tela de login.", F.s(9.5.sp, C.text), maxLines = 2)
        Spacer(Modifier.height(10.dp))
        Option("Sair da conta", sub = "Remove a lista deste aparelho", icon = Ic.Logout, danger = true, memKey = "logout") {
            confirm(Ask("Sair da conta?", "Você vai precisar entrar de novo com os dados do seu provedor.", "Sair", Ic.Logout, danger = true) {
                pb.stop(); ui.stack.clear(); ui.tab = Tab.CANAIS; ui.home = true; vm.logout()
            })
        }
    }
}

@Composable
private fun HomeLayoutPicker(vm: Vm) {
    val cur = vm.store.homeLayout.value
    Card("TELA INICIAL") { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Option("Padrão", sub = "Blocos grandes e Continuar assistindo", radio = cur !in listOf("lite", "cards", "round", "square"), memKey = "layout:default") { vm.store.setHomeLayout("default") }
        Option("Lite", sub = "Blocos quadrados com ícones de linha", radio = cur == "lite", memKey = "layout:lite") { vm.store.setHomeLayout("lite") }
        Option("Vitrine", sub = "Cartões de vidro grandes, relógio e dados do aparelho", radio = cur == "cards", memKey = "layout:cards") { vm.store.setHomeLayout("cards") }
        Option("Redondo", sub = "Ícones grandes em círculos de vidro, o escolhido em degradê", radio = cur == "round", memKey = "layout:round") { vm.store.setHomeLayout("round") }
        Option("Quadrado", sub = "Ícones grandes em quadrados arredondados de vidro", radio = cur == "square", memKey = "layout:square") { vm.store.setHomeLayout("square") }
    } }
}

/** Configurações → Atualizações: versão instalada, situação e o botão de verificar/atualizar. */
@Composable
private fun UpdatesCard() {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val up = Updater.available
    val p = Updater.progress
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.panel).border(1.dp, C.panelBorder, RoundedCornerShape(14.dp)).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.mipmap.ic_launcher), null, Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Txt("Orbita Play", F.s(16.sp, C.white, FontWeight.Bold, F.outfit))
                Txt("Versão ${Updater.currentName(ctx)}", F.s(10.sp, C.dim))
            }
            val (t, c) = when {
                Updater.checking -> "Verificando…" to C.accentText
                up != null -> "Nova versão ${up.name}" to Color(0xFFFFC56B)
                Updater.lastResult != null -> "Atualizado" to Color(0xFF7EE2A8)
                else -> "Não verificado" to C.dim
            }
            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(7.dp))
                Txt(t, F.s(9.5.sp, c, FontWeight.SemiBold))
            }
        }
        if (up != null) {
            Card("NOVIDADES") { Txt(up.notes.ifBlank { "Melhorias e correções." }, F.s(10.sp, C.text), maxLines = 4) }
        }
        if (p >= 0f) {
            Column {
                ProgressBar(p, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), C.accent)
                Spacer(Modifier.height(6.dp)); Txt("Baixando… ${(p * 100).toInt()}%", F.s(9.sp, C.dim))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (up != null) ActionButton("Atualizar agora", primary = true, icon = Ic.Refresh) { scope.launch { Updater.install(ctx) } }
            ActionButton(if (Updater.checking) "Verificando…" else "Verificar atualizações", primary = up == null, icon = Ic.New) { scope.launch { Updater.check(ctx) } }
        }
        Updater.error?.let { Txt(it, F.s(9.sp, C.live, FontWeight.Medium), maxLines = 3) }
    }
}

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
        Section("layout", "Layout da tela inicial", Ic.Home, "Padrão ou Lite."),
    )),
    Group("SISTEMA", listOf(
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
                    Modifier.width(220.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(C.panel)
                        .border(1.dp, C.panelBorder, RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    groups.forEach { g ->
                        Txt(g.title, F.s(7.sp, C.faint, FontWeight.SemiBold, spacing = 1.2.sp), Modifier.padding(start = 10.dp, top = 7.dp, bottom = 3.dp))
                        g.items.forEach { s ->
                            Focus(Modifier.fillMaxWidth(), onClick = { key = s.key }, requester = if (s.key == "info") first else null, memKey = "set:${s.key}") { f ->
                                val sel = key == s.key
                                val danger = s.key == "logout"
                                Row(
                                    Modifier.fillMaxWidth().height(29.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (f) C.accent else if (sel) C.accentSoft else Color.Transparent)
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.width(2.5.dp).height(14.dp).background(if (sel && !f) C.accent else Color.Transparent))
                                    Spacer(Modifier.width(7.dp))
                                    Icon(s.icon, 13.dp, if (f) C.white else if (danger) C.live else if (sel) C.white else C.dim)
                                    Spacer(Modifier.width(9.dp))
                                    Txt(s.title, F.s(9.5.sp, if (f) C.white else if (danger) C.live else if (sel) C.white else C.text, FontWeight.SemiBold))
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
                            "storage" -> Storage(vm, confirm) { msg = it }
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
private fun Option(label: String, value: String? = null, checked: Boolean? = null, radio: Boolean? = null, danger: Boolean = false, memKey: String, onClick: () -> Unit) {
    Focus(Modifier.fillMaxWidth(), onClick = onClick, memKey = memKey) { f ->
        Row(
            Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(9.dp)).background(if (f) C.focusBg else C.item)
                .border(if (f) 1.5.dp else 1.dp, if (f) C.accent else C.panelBorder, RoundedCornerShape(9.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (radio != null) {
                Box(Modifier.size(16.dp).clip(CircleShape).border(2.dp, if (radio) C.accent else C.dim, CircleShape), contentAlignment = Alignment.Center) {
                    if (radio) Box(Modifier.size(8.dp).clip(CircleShape).background(C.accent))
                }
                Spacer(Modifier.width(10.dp))
            }
            Txt(label, F.s(10.5.sp, if (danger) C.live else C.white, FontWeight.SemiBold), Modifier.weight(1f))
            value?.let { Txt(it, F.s(8.5.sp, C.dim, FontWeight.Medium), Modifier.padding(end = if (checked != null) 10.dp else 0.dp)) }
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
private fun GeneralInfo(vm: Vm) {
    val a = vm.account
    val st = vm.status
    val ctx = LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
    val active = st?.status == null || st.status.equals("Active", true)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card("CONTA", Modifier.weight(1f).height(118.dp)) {
                Line("Tipo", if (a?.type == "xtream") "Xtream Codes" else "Lista M3U")
                if (a?.type == "xtream") { Line("Servidor", a.server.removePrefix("http://").removePrefix("https://")); Line("Usuário", a.user) }
                else Line("Lista", a?.m3uUrl.orEmpty())
            }
            Card("ASSINATURA", Modifier.weight(1f).height(118.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val c = if (active) Color(0xFF7EE2A8) else C.live
                    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(c.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(c)); Spacer(Modifier.width(5.dp))
                        Txt(if (active) "Ativa" else st?.status.orEmpty(), F.s(8.5.sp, c, FontWeight.SemiBold))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    Box(Modifier.weight(1f)) { BigValue("VALIDADE", st?.expires?.let { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(it)) } ?: "—") }
                    Box(Modifier.weight(1f)) { BigValue("CONEXÕES", st?.maxConnections?.let { "${st.activeConnections ?: "0"} de $it" } ?: "—") }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card("ESTE APARELHO", Modifier.weight(1f).height(118.dp)) {
                Row {
                    Box(Modifier.weight(1.3f)) { BigValue("MAC", Activation.mac(ctx)) }
                    Box(Modifier.weight(1f)) { BigValue("CHAVE", Activation.key(ctx), 2f) }
                }
                Spacer(Modifier.height(2.dp))
                if (Activation.enabled()) Txt("Use o MAC e a chave em ${Activation.site.removePrefix("https://").removePrefix("http://")}", F.s(8.sp, C.dim), maxLines = 1)
                Txt("Orbita Play $ver", F.s(8.sp, C.faint))
            }
            Card("CONTEÚDO", Modifier.weight(1f).height(118.dp)) {
                Row {
                    listOf(vm.uniqueChannels.size to "canais", vm.movies.size to "filmes", vm.series.size to "séries").forEach { (n, l) ->
                        Column(Modifier.weight(1f)) {
                            Txt(fmtCount(n), F.s(17.sp, C.white, FontWeight.Bold, F.outfit))
                            Txt(l, F.s(8.5.sp, C.dim))
                        }
                    }
                }
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
            Option("MPEG-TS (.ts)", "Padrão, mais rápido", radio = f != "m3u8", memKey = "fmt:ts") { set("ts") }
            Option("HLS (.m3u8)", "Mais compatível", radio = f == "m3u8", memKey = "fmt:hls") { set("m3u8") }
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
    Option("Alterar senha", "4 números", memKey = "pin:change") {
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
private fun Storage(vm: Vm, confirm: (Ask) -> Unit, msg: (String) -> Unit) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card("LISTAS") {
            Option("Recarregar listas", "Canais, filmes e séries", memKey = "st:reload") {
                confirm(Ask("Recarregar as listas?", "Canais, filmes e séries são baixados de novo do seu provedor.", "Recarregar", Ic.Refresh) { vm.reloadAll(); msg("Recarregando listas…") })
            }
        }
        Card("DADOS GUARDADOS NESTE APARELHO") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Option("Limpar histórico", "Recentes e continuar assistindo", memKey = "st:history") {
                    confirm(Ask("Limpar o histórico?", "Os canais recentes e o \"Continuar assistindo\" serão apagados.", "Limpar", Ic.Refresh, danger = true) { vm.store.clearHistory(); msg("Histórico apagado") })
                }
                Option("Limpar imagens guardadas", "Capas e logos", memKey = "st:images") {
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
        Option("Sair da conta", danger = true, memKey = "logout") {
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
        Option("Padrão", "Blocos grandes e Continuar assistindo", radio = cur != "lite", memKey = "layout:default") { vm.store.setHomeLayout("default") }
        Option("Lite", "Logo no centro, ícones de linha e capas ao fundo", radio = cur == "lite", memKey = "layout:lite") { vm.store.setHomeLayout("lite") }
    } }
}

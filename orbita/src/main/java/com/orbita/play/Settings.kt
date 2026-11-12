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

private class Section(val title: String, val icon: ImageVector)

private val sections = listOf(
    Section("Informações gerais", Ic.Info),
    Section("Formato de fluxo", Ic.LiveTv),
    Section("Controle parental", Ic.Lock),
    Section("Alterar senha", Ic.Key),
    Section("Ocultar categorias", Ic.EyeOff),
    Section("Reprodução", Ic.Play),
    Section("Armazenamento", Ic.Refresh),
    Section("Sair da conta", Ic.Logout),
)

/** Configurações em duas colunas: menu à esquerda, opções da seção à direita. */
@Composable
fun SettingsScreen(vm: Vm, pb: Playback, ui: Ui) {
    ScreenBackground()
    var section by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) { if (msg != null) { delay(2500); msg = null } }
    val first = remember { FocusRequester() }
    InitialFocus(first)

    Column(Modifier.fillMaxSize().padding(start = 36.dp, end = 36.dp, top = 26.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(size = 13f)
            Spacer(Modifier.width(18.dp))
            Txt("Configurações", F.s(20.sp, C.white, FontWeight.Bold, F.outfit))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.weight(1f)) {
            Column(Modifier.width(250.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sections.forEachIndexed { i, s ->
                    Focus(Modifier.fillMaxWidth(), onFocus = { section = i }, requester = if (i == 0) first else null, memKey = "set:$i") { f ->
                        val sel = section == i
                        Row(
                            Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(9.dp))
                                .background(if (f) C.accent else if (sel) C.accentSoft else C.panel)
                                .border(1.dp, if (sel && !f) C.accent.copy(alpha = 0.6f) else C.panelBorder, RoundedCornerShape(9.dp))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(s.icon, 15.dp, if (f || sel) C.white else C.dim)
                            Spacer(Modifier.width(10.dp))
                            Txt(s.title, F.s(10.5.sp, if (i == sections.lastIndex) C.live.copy(alpha = if (f) 1f else 0.9f) else C.white, FontWeight.SemiBold))
                        }
                    }
                }
            }
            Spacer(Modifier.width(18.dp))
            Panel(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    Txt(sections[section].title, F.s(14.sp, C.white, FontWeight.Bold, F.outfit))
                    Spacer(Modifier.height(12.dp))
                    when (section) {
                        0 -> GeneralInfo(vm)
                        1 -> StreamFormat(vm) { msg = it }
                        2 -> Parental(vm, ui) { msg = it }
                        3 -> ChangePin(vm, ui) { msg = it }
                        4 -> HideCategories(vm)
                        5 -> PlaybackPrefs(vm)
                        6 -> Storage(vm) { msg = it }
                        else -> Logout(vm, pb, ui)
                    }
                    msg?.let { Spacer(Modifier.height(10.dp)); Txt(it, F.s(9.5.sp, C.cyan, FontWeight.Medium)) }
                }
            }
        }
    }
}

@Composable
private fun Option(label: String, value: String? = null, checked: Boolean? = null, radio: Boolean? = null, danger: Boolean = false, memKey: String, onClick: () -> Unit) {
    Focus(Modifier.fillMaxWidth(), onClick = onClick, memKey = memKey) { f ->
        Row(
            Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(9.dp)).background(if (f) C.focusBg else C.item)
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
            value?.let { Txt(it, F.s(9.sp, C.cyan, FontWeight.Medium)) }
            checked?.let { Switch(it) }
        }
    }
}

@Composable
private fun Line(k: String, v: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Txt(k, F.s(10.sp, C.dim), Modifier.width(110.dp))
        Txt(v, F.s(10.sp, C.white, FontWeight.Medium), maxLines = 2)
    }
}

@Composable
private fun GeneralInfo(vm: Vm) {
    val a = vm.account
    val st = vm.status
    val ctx = LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
    Line("Tipo", if (a?.type == "xtream") "Xtream Codes" else "Lista M3U")
    if (a?.type == "xtream") { Line("Servidor", a.server); Line("Usuário", a.user) } else Line("Lista", a?.m3uUrl.orEmpty())
    st?.status?.let { Line("Status", if (it.equals("Active", true)) "Ativa" else it) }
    st?.expires?.let { Line("Validade", SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(it))) }
    st?.maxConnections?.let { Line("Conexões", "${st.activeConnections ?: "0"} de $it em uso") }
    Line("Conteúdo", "${fmtCount(vm.uniqueChannels.size)} canais · ${fmtCount(vm.movies.size)} filmes · ${fmtCount(vm.series.size)} séries")
    Line("Versão", "Orbita Play $ver")
    Spacer(Modifier.height(10.dp))
    Txt("O app não fornece conteúdo: use apenas listas que você tem direito de usar.", F.s(8.5.sp, C.faint), maxLines = 2)
}

@Composable
private fun StreamFormat(vm: Vm, msg: (String) -> Unit) {
    val f = vm.store.liveFormat.value
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Option("MPEG-TS (.ts)", radio = f != "m3u8", memKey = "fmt:ts") { if (f == "m3u8") { vm.store.setLiveFormat("ts"); vm.reloadAll(); msg("Formato alterado. Recarregando canais…") } }
        Option("HLS (.m3u8)", radio = f == "m3u8", memKey = "fmt:hls") { if (f != "m3u8") { vm.store.setLiveFormat("m3u8"); vm.reloadAll(); msg("Formato alterado. Recarregando canais…") } }
    }
    Spacer(Modifier.height(10.dp))
    Txt("Use HLS se algum canal não abrir ou ficar travando no formato TS.", F.s(8.5.sp, C.faint), maxLines = 2)
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Txt("CANAIS, FILMES E SÉRIES ADULTOS", F.label)
        Spacer(Modifier.height(2.dp))
        Option("Bloquear com senha", radio = mode == "lock", memKey = "adult:lock") { set("lock") }
        Option("Ocultar", radio = mode == "hide", memKey = "adult:hide") { set("hide") }
        Option("Mostrar", radio = mode == "show", memKey = "adult:show") { set("show") }
        if (mode == "lock" && vm.adultUnlocked) {
            Spacer(Modifier.height(4.dp))
            Option("Bloquear de novo agora", memKey = "adult:relock") { vm.adultUnlocked = false; msg("Conteúdo adulto bloqueado.") }
        }
    }
    Spacer(Modifier.height(10.dp))
    Txt("As categorias adultas são reconhecidas pelo nome (Adultos, XXX, +18…). Senha padrão: 0000.", F.s(8.5.sp, C.faint), maxLines = 2)
}

@Composable
private fun ChangePin(vm: Vm, ui: Ui, msg: (String) -> Unit) {
    Option("Alterar senha", memKey = "pin:change") {
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
    Spacer(Modifier.height(10.dp))
    Txt("A senha protege o conteúdo adulto e as mudanças no controle parental.", F.s(8.5.sp, C.faint), maxLines = 2)
}

@Composable
private fun HideCategories(vm: Vm) {
    var tab by remember { mutableIntStateOf(0) }
    val kinds = listOf("live" to "TV", "vod" to "FILMES", "series" to "SÉRIES")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        kinds.forEachIndexed { i, (_, label) -> ChipPill(label, tab == i, onFocus = { tab = i }) { tab = i } }
    }
    Spacer(Modifier.height(10.dp))
    val kind = kinds[tab].first
    val cats = when (tab) { 0 -> vm.liveCats; 1 -> vm.movieCats; else -> vm.seriesCats }
    if (cats.isEmpty()) { Txt("Nenhuma categoria carregada.", F.s(9.sp, C.dim)); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items(cats, key = { kind + it.id }) { c ->
            val visible = !vm.store.isHidden("$kind:${c.id}")
            Option(c.name, checked = visible, memKey = "hide:$kind:${c.id}") { vm.store.toggleHidden("$kind:${c.id}") }
        }
    }
}

@Composable
private fun PlaybackPrefs(vm: Vm) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Option("Próximo episódio automático", checked = vm.store.autoNext.value, memKey = "pl:next") { vm.store.setAutoNext(!vm.store.autoNext.value) }
        Option("Prévia automática nos canais", checked = vm.store.previewAutoplay.value, memKey = "pl:preview") { vm.store.setPreviewAutoplay(!vm.store.previewAutoplay.value) }
    }
}

@Composable
private fun Storage(vm: Vm, msg: (String) -> Unit) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Option("Recarregar listas", "Canais, filmes e séries", memKey = "st:reload") { vm.reloadAll(); msg("Recarregando listas…") }
        Option("Limpar histórico", "Recentes e continuar assistindo", memKey = "st:history") { vm.store.clearHistory(); msg("Histórico apagado.") }
        Option("Limpar imagens guardadas", "Capas e logos", memKey = "st:images") {
            runCatching { coil.Coil.imageLoader(ctx).let { it.memoryCache?.clear(); it.diskCache?.clear() } }
            msg("Imagens apagadas.")
        }
    }
}

@Composable
private fun Logout(vm: Vm, pb: Playback, ui: Ui) {
    var confirm by remember { mutableStateOf(false) }
    Txt("Você vai precisar entrar de novo com os dados do seu provedor.", F.s(9.5.sp, C.dim), maxLines = 2)
    Spacer(Modifier.height(12.dp))
    Option(if (confirm) "Confirmar: sair da conta" else "Sair da conta", danger = true, memKey = "logout") {
        if (confirm) { pb.stop(); ui.stack.clear(); ui.tab = Tab.CANAIS; vm.logout() } else confirm = true
    }
}


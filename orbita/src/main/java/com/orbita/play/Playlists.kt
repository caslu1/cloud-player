package com.orbita.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Trash by lazy {
    ImageVector.Builder("trash", 24.dp, 24.dp, 24f, 24f).addPath(
        addPathNodes("M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM8,9h8v10H8V9zM15.5,4l-1,-1h-5l-1,1H5v2h14V4z"),
        fill = SolidColor(Color.White)
    ).build()
}

/**
 * Playlists (tela Vitrine): listas salvas no aparelho. OK numa lista passa a usá-la,
 * a lixeira apaga (com confirmação) e o + abre o login para adicionar outra.
 */
@Composable
fun PlaylistsScreen(vm: Vm, ui: Ui) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var askDelete by remember { mutableStateOf<Playlist?>(null) }
    val first = remember { FocusRequester() }
    InitialFocus(first)

    Box(Modifier.fillMaxSize()) {
        ScreenBackground()
        Box(Modifier.fillMaxSize().background(Color(0x99050409)))
        Column(Modifier.fillMaxSize().padding(start = 130.dp, end = 130.dp, top = 44.dp, bottom = 60.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("Playlists", F.s(24.sp, C.white, FontWeight.Bold, F.outfit))
                Spacer(Modifier.weight(1f))
                Focus(Modifier.size(54.dp, 34.dp), onClick = { ui.push(Route.AddPlaylist(FocusMem.last)) }, memKey = "pl:add", requester = if (vm.store.playlists.isEmpty()) first else null) { f ->
                    Box(
                        Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)).background(if (f) C.white else Color(0xCC15121F))
                            .border(1.dp, if (f) C.white else Color(0x33FFFFFF), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Ic.Add, 20.dp, if (f) Color(0xFF0B0A12) else C.white, desc = "Adicionar playlist") }
                }
                Spacer(Modifier.width(52.dp))
            }
            Spacer(Modifier.height(16.dp))
            if (vm.store.playlists.isEmpty()) {
                Txt("Nenhuma playlist salva. Toque em + para adicionar.", F.s(11.sp, C.dim))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(vm.store.playlists, key = { i, p -> "${p.acc.type}|${p.acc.server}|${p.acc.user}|${p.acc.m3uUrl}|$i" }) { i, pl ->
                    val current = vm.account?.let { pl.same(it) } == true
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Focus(Modifier.weight(1f).height(48.dp), memKey = "pl:$i", requester = if (i == 0) first else null, onClick = {
                            if (current) { ui.stack.clear(); ui.home = true; return@Focus }
                            if (busy) return@Focus
                            busy = true; error = null
                            scope.launch {
                                runCatching { vm.switchTo(pl.acc) }
                                    .onSuccess { ui.stack.clear(); ui.home = true }
                                    .onFailure { error = it.message ?: "Não foi possível abrir essa playlist." }
                                busy = false
                            }
                        }) { f ->
                            Row(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)).background(if (f) Color(0xFFE4E2EC) else Color(0xCC15121F))
                                    .border(1.dp, if (f) C.white else Color(0x26FFFFFF), RoundedCornerShape(7.dp)).padding(horizontal = 22.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val fg = if (f) Color(0xFF0B0A12) else C.white
                                Txt("${i + 1}", F.s(13.sp, fg, FontWeight.Medium), Modifier.width(36.dp))
                                Txt(pl.name, F.s(13.sp, fg, FontWeight.Medium), Modifier.weight(1f), maxLines = 1)
                                if (current) Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (f) C.accent else C.accentSoft).padding(horizontal = 9.dp, vertical = 3.dp)) {
                                    Txt("Em uso", F.s(8.sp, C.white, FontWeight.SemiBold))
                                }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Focus(Modifier.size(38.dp), memKey = "pl:del:$i", onClick = { askDelete = pl }) { f ->
                            Box(
                                Modifier.fillMaxSize().clip(CircleShape).background(if (f) C.live else Color(0xCC15121F))
                                    .border(1.dp, if (f) C.live else Color(0x33FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Icon(Trash, 17.dp, C.white, desc = "Apagar") }
                        }
                    }
                }
            }
            error?.let { Spacer(Modifier.height(10.dp)); Txt(it, F.s(9.sp, C.live, FontWeight.Medium), maxLines = 2) }
        }

        // rodapé: versão, situação, site, chave e MAC
        val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
        val st = vm.status
        val status = if (st?.status == null || st.status.equals("Active", true)) "Ativado" else st.status
        val mac = remember { Activation.mac(ctx) }
        val key = remember { Activation.key(ctx) }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 30.dp, vertical = 16.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Txt("Versão: $ver", F.s(8.5.sp, C.text, FontWeight.Medium))
                Txt("Status: $status", F.s(8.5.sp, C.text, FontWeight.Medium))
            }
            if (Activation.site.isNotBlank()) Txt("Página da web: ${Activation.site}", F.s(8.5.sp, C.text, FontWeight.Medium))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Txt("Chave do dispositivo: $key", F.s(8.5.sp, C.text, FontWeight.Medium))
                Txt("Endereço MAC: $mac", F.s(8.5.sp, C.text, FontWeight.Medium))
            }
        }

        if (busy) Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) { Loading("Abrindo a playlist…") }
        askDelete?.let { pl ->
            ConfirmDialog("Apagar \"${pl.name}\"?", "A playlist sai deste aparelho. Você pode adicioná-la de novo depois.", "Apagar",
                onConfirm = { askDelete = null; vm.removePlaylist(pl) }, onCancel = { askDelete = null }, icon = Trash, danger = true)
        }
    }
}

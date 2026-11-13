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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Rodapé padrão de todos os temas da tela inicial: versão e status | MAC, chave e usuário. */
@Composable
fun DeviceFooter(vm: Vm, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "1.0" }
    val st = vm.status
    val status = buildString {
        append(if (st?.status == null || st.status.equals("Active", true)) "Ativa" else st.status)
        st?.expires?.let { append(" · vence " + SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(it))) }
    }
    val mac = remember { Activation.mac(ctx) }
    val key = remember { Activation.key(ctx) }
    val user = vm.account?.takeIf { it.type == "xtream" }?.user
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
        Column {
            Txt("Versão: $ver", F.s(7.5.sp, C.dim))
            Txt("Status: $status", F.s(7.5.sp, C.dim))
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Txt("Endereço MAC: $mac", F.s(7.5.sp, C.dim))
            Txt("Chave do dispositivo: $key", F.s(7.5.sp, C.dim))
            if (user != null) Txt("Usuário: $user", F.s(7.5.sp, C.dim))
        }
    }
}

/** Janela do "Recarregar": carregando… e depois "Atualização concluída". */
@Composable
fun ReloadDialog(vm: Vm, ui: Ui) {
    val s = ui.reloading
    LaunchedEffect(s) {
        if (s == 1) {
            delay(400)
            withTimeoutOrNull(180_000) {
                snapshotFlow {
                    listOf(vm.liveState, vm.movieState, vm.seriesState).all { it == LoadState.READY || it == LoadState.ERROR }
                }.first { it }
            }
            ui.reloading = 2
        } else if (s == 2) { delay(1800); ui.reloading = 0 }
    }
    if (s == 0) return
    Box(Modifier.fillMaxSize().background(Color(0xB3000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(280.dp).clip(RoundedCornerShape(16.dp)).background(C.panelSolid).border(1.dp, C.panelBorder, RoundedCornerShape(16.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (s == 1) {
                Spinner(30.dp)
                Spacer(Modifier.height(14.dp))
                Txt("Carregando…", F.s(14.sp, C.white, FontWeight.Bold, F.outfit))
                Spacer(Modifier.height(4.dp))
                Txt("Atualizando canais, filmes e séries", F.s(9.sp, C.dim))
            } else {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x297EE2A8)), contentAlignment = Alignment.Center) { Icon(Ic.Check, 22.dp, Color(0xFF7EE2A8)) }
                Spacer(Modifier.height(12.dp))
                Txt("Atualização concluída", F.s(14.sp, C.white, FontWeight.Bold, F.outfit))
                Spacer(Modifier.height(4.dp))
                Txt("Sua lista está em dia.", F.s(9.sp, C.dim))
            }
        }
    }
}

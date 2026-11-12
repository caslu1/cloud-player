package com.orbita.play

import android.view.KeyEvent as AKey
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Pedido de senha mostrado por cima de qualquer tela. [submit] devolve uma mensagem de erro ou null (fecha). */
class PinPrompt(val title: String, val subtitle: String = "", val submit: (String) -> String?)

/** Teclado numérico de 4 dígitos (também aceita os números do controle remoto). */
@Composable
fun PinDialog(p: PinPrompt, onClose: () -> Unit) {
    var pin by remember(p) { mutableStateOf("") }
    var error by remember(p) { mutableStateOf<String?>(null) }
    val first = remember { FocusRequester() }
    LaunchedEffect(p) { delay(60); runCatching { first.requestFocus() } }
    BackHandler { onClose() }

    fun type(d: String) {
        if (pin.length >= 4) return
        error = null
        pin += d
        if (pin.length == 4) {
            val e = p.submit(pin)
            if (e == null) onClose() else { error = e; pin = "" }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000))
            .onPreviewKeyEvent { ev ->
                val c = ev.nativeKeyEvent.keyCode
                if (ev.type == KeyEventType.KeyDown && c in AKey.KEYCODE_0..AKey.KEYCODE_9) { type((c - AKey.KEYCODE_0).toString()); true }
                else if (ev.type == KeyEventType.KeyDown && c == AKey.KEYCODE_DEL) { pin = pin.dropLast(1); true }
                else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.width(300.dp).clip(RoundedCornerShape(16.dp)).background(C.panelSolid).border(1.dp, C.panelBorder, RoundedCornerShape(16.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(C.accentSoft), contentAlignment = Alignment.Center) { Icon(Ic.Lock, 18.dp, C.accentText) }
            Spacer(Modifier.height(10.dp))
            Txt(p.title, F.s(13.sp, C.white, FontWeight.Bold, F.outfit))
            if (p.subtitle.isNotEmpty()) { Spacer(Modifier.height(3.dp)); Txt(p.subtitle, F.s(8.5.sp, C.dim), maxLines = 2) }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier.size(38.dp, 44.dp).clip(RoundedCornerShape(8.dp)).background(C.field)
                            .border(1.5.dp, if (i == pin.length) C.accent else C.panelBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) { if (i < pin.length) Box(Modifier.size(10.dp).clip(CircleShape).background(C.white)) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Txt(error ?: " ", F.s(8.5.sp, C.live, FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "×")
            keys.chunked(3).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    row.forEach { k ->
                        Focus(
                            requester = if (k == "1") first else null,
                            onClick = { when (k) { "⌫" -> pin = pin.dropLast(1); "×" -> onClose(); else -> type(k) } }
                        ) { f ->
                            Box(
                                Modifier.size(64.dp, 40.dp).clip(RoundedCornerShape(8.dp)).background(if (f) C.accent else C.item),
                                contentAlignment = Alignment.Center
                            ) {
                                when (k) {
                                    "⌫" -> Icon(Ic.Backspace, 16.dp)
                                    "×" -> Txt("Cancelar", F.s(8.5.sp, C.white, FontWeight.SemiBold))
                                    else -> Txt(k, F.s(14.sp, C.white, FontWeight.Bold, F.outfit))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Interruptor liga/desliga. */
@Composable
fun Switch(on: Boolean) {
    Box(
        Modifier.size(34.dp, 19.dp).clip(RoundedCornerShape(10.dp)).background(if (on) C.accent else Color(0xFF34304A)).padding(2.5.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) { Box(Modifier.size(14.dp).clip(CircleShape).background(C.white)) }
}

/** Janela de confirmação (sair do app, sair da conta, limpar dados…). */
@Composable
fun ConfirmDialog(
    title: String, message: String, confirm: String, onConfirm: () -> Unit, onCancel: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Ic.Logout, danger: Boolean = false,
) {
    val cancelReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(60); runCatching { cancelReq.requestFocus() } }
    BackHandler { onCancel() }
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(340.dp).clip(RoundedCornerShape(16.dp)).background(C.panelSolid).border(1.dp, C.panelBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val tint = if (danger) C.live else C.accentText
            Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Icon(icon, 20.dp, tint) }
            Spacer(Modifier.height(12.dp))
            Txt(title, F.s(14.sp, C.white, FontWeight.Bold, F.outfit))
            if (message.isNotEmpty()) { Spacer(Modifier.height(5.dp)); Txt(message, F.s(9.sp, C.dim), maxLines = 3, align = androidx.compose.ui.text.style.TextAlign.Center) }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Cancelar", requester = cancelReq) { onCancel() }
                ActionButton(confirm, primary = true, icon = icon) { onConfirm() }
            }
        }
    }
}

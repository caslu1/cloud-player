package com.orbita.play

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.insertTextAtCursor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pedido de digitação: o teclado do app aparece embaixo e escreve direto no campo. */
class KbRequest(
    val label: String, initial: String, val password: Boolean,
    val onChange: (String) -> Unit, val onDone: () -> Unit, val back: FocusRequester?,
) {
    var text by mutableStateOf(initial)
}

/** Teclado próprio do app (igual em todas as telas que pedem texto). */
object Kb {
    var req by mutableStateOf<KbRequest?>(null)

    fun open(label: String, value: String, password: Boolean = false, back: FocusRequester? = null, onDone: () -> Unit = {}, onChange: (String) -> Unit) {
        req = KbRequest(label, value, password, onChange, onDone, back)
    }

    fun close(done: Boolean) {
        val r = req ?: return
        req = null
        runCatching { r.back?.requestFocus() }
        if (done) r.onDone()
    }
}

/** Campo que abre o teclado do app no OK (os testes escrevem nele pela acessibilidade). */
fun Modifier.kbSemantics(value: String, onChange: (String) -> Unit): Modifier = semantics {
    setText { onChange(it.text); true }
    insertTextAtCursor { onChange(value + it.text); true }
}

private class K(val label: String, val w: Float = 1f, val action: String? = null)

private val lettersRows = listOf(
    "qwertyuiop/\\".map { K(it.toString()) } + K("⌫", 1f, "del"),
    "asdfghjkl.:".map { K(it.toString()) } + K("Pronto", 2f, "done"),
    listOf(K("⇧", 1f, "shift")) + "zxcvbnm,?".map { K(it.toString()) } + K("Limpar", 2f, "clear") + K("⇧", 1f, "shift"),
    listOf(K("123", 1.4f, "mode"), K("http://", 1.6f), K("espaço", 6f, "space"), K(".com", 1.6f), K("@", 1.4f)),
)

private val symbolRows = listOf(
    "1234567890-_".map { K(it.toString()) } + K("⌫", 1f, "del"),
    "!#\$%&*()=+;".map { K(it.toString()) } + K("Pronto", 2f, "done"),
    listOf(K("[", 1f), K("]", 1f)) + "{}<>'\"~|".map { K(it.toString()) } + K("Limpar", 2f, "clear") + K("^", 1f),
    listOf(K("ABC", 1.4f, "mode"), K("https://", 1.6f), K("espaço", 6f, "space"), K(".br", 1.6f), K(":", 1.4f)),
)

@Composable
fun BoxScope.TvKeyboard() {
    val r = Kb.req ?: return
    var symbols by remember(r) { mutableStateOf(false) }
    var shift by remember(r) { mutableStateOf(false) }
    var row by remember(r) { mutableIntStateOf(0) }
    var col by remember(r) { mutableIntStateOf(0) }
    val rows = if (symbols) symbolRows else lettersRows
    val focus = remember(r) { FocusRequester() }
    LaunchedEffect(r) { kotlinx.coroutines.delay(30); runCatching { focus.requestFocus() } }
    BackHandler { Kb.close(false) }

    fun set(t: String) { r.text = t; r.onChange(t) }
    fun press(k: K) {
        when (k.action) {
            "del" -> set(r.text.dropLast(1))
            "clear" -> set("")
            "done" -> Kb.close(true)
            "shift" -> shift = !shift
            "mode" -> { symbols = !symbols; col = col.coerceAtMost((if (symbols) symbolRows else lettersRows)[row].size - 1) }
            "space" -> set(r.text + " ")
            else -> {
                val ch = if (shift && k.label.length == 1) k.label.uppercase() else k.label
                set(r.text + ch)
                if (shift) shift = false
            }
        }
    }
    // ao subir/descer, vai para a tecla mais perto na outra linha
    fun moveRow(d: Int) {
        val nr = (row + d).coerceIn(0, rows.size - 1)
        if (nr == row) return
        val center = rows[row].take(col).sumOf { it.w.toDouble() } + rows[row][col].w / 2
        var acc = 0.0; var best = 0
        rows[nr].forEachIndexed { i, k -> if (acc <= center) best = i; acc += k.w }
        row = nr; col = best
    }

    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xF2060510))
            .focusRequester(focus).focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> { col = (col - 1).coerceAtLeast(0); true }
                    Key.DirectionRight -> { col = (col + 1).coerceAtMost(rows[row].size - 1); true }
                    Key.DirectionUp -> { moveRow(-1); true }
                    Key.DirectionDown -> { moveRow(1); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { press(rows[row][col.coerceAtMost(rows[row].size - 1)]); true }
                    else -> false
                }
            }
            .padding(top = 10.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // o que está sendo digitado (o campo pode estar escondido atrás do teclado)
        Row(Modifier.width(560.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(r.label, F.s(8.sp, C.dim, FontWeight.SemiBold, spacing = 1.sp))
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF15131F))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(7.dp)).padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val shown = if (r.password) "•".repeat(r.text.length) else r.text
                Txt("$shown▏", F.s(11.sp, C.white, FontWeight.Medium))
            }
        }
        rows.forEachIndexed { ri, keys ->
            Row(Modifier.width(560.dp).padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                keys.forEachIndexed { ci, k ->
                    val f = ri == row && ci == col
                    val shape = RoundedCornerShape(5.dp)
                    val label = if (shift && symbols.not() && k.label.length == 1) k.label.uppercase() else k.label
                    Box(
                        Modifier.weight(k.w).height(28.dp).clip(shape)
                            .background(if (f) C.white else if (k.action == "shift" && shift) Color(0xFF3A3650) else Color(0xFF16141F))
                            .border(1.dp, Color(0x1FFFFFFF), shape)
                            .clickable { row = ri; col = ci; press(k) },
                        contentAlignment = Alignment.Center
                    ) {
                        Txt(if (k.action == "space") "␣" else label, F.s(if (label.length > 2) 9.sp else 11.sp, if (f) Color(0xFF0B0A12) else C.white, FontWeight.SemiBold))
                    }
                }
            }
        }
    }
}

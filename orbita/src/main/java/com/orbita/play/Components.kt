package com.orbita.play

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Txt(
    text: String, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = 1,
    align: TextAlign? = null,
) = BasicText(
    text, modifier, style = if (align != null) style.copy(textAlign = align) else style,
    maxLines = maxLines, overflow = TextOverflow.Ellipsis
)

fun isSelectKey(k: Key) = k == Key.DirectionCenter || k == Key.Enter || k == Key.NumPadEnter

/**
 * Item focável pelo controle remoto. OK = clique; segurar OK = [onLongClick].
 * O clique só acontece se o botão foi pressionado aqui (evita "clique fantasma" ao trocar de tela).
 */
@Composable
fun Focus(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onFocus: () -> Unit = {},
    requester: FocusRequester? = null,
    onKey: ((KeyEvent) -> Boolean)? = null,
    memKey: String? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val req = requester ?: remember { FocusRequester() }
    if (memKey != null) LaunchedEffect(memKey) {
        if (FocusMem.restore == memKey) {
            kotlinx.coroutines.delay(16)
            if (runCatching { req.requestFocus() }.isSuccess) { FocusMem.restore = null; FocusMem.restoredAt = System.currentTimeMillis() }
        }
    }
    val state = remember { BooleanArray(2) } // [0] = OK pressionado aqui, [1] = longo já disparado
    val click by rememberUpdatedState(onClick)
    val long by rememberUpdatedState(onLongClick)
    val focusCb by rememberUpdatedState(onFocus)
    val keyCb by rememberUpdatedState(onKey)
    Box(
        modifier
            .focusRequester(req)
            .onFocusChanged {
                if (it.isFocused != focused) {
                    focused = it.isFocused
                    if (it.isFocused) { FocusMem.last = memKey; focusCb() } else { state[0] = false; state[1] = false }
                }
            }
            .onPreviewKeyEvent { ev ->
                if (keyCb?.invoke(ev) == true) return@onPreviewKeyEvent true
                if (!isSelectKey(ev.key)) return@onPreviewKeyEvent false
                when (ev.type) {
                    KeyEventType.KeyDown -> {
                        val rep = ev.nativeKeyEvent.repeatCount
                        if (rep == 0) { state[0] = true; state[1] = false }
                        else if (rep >= 1 && state[0] && !state[1] && long != null) { state[1] = true; long?.invoke() }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (state[0] && !state[1]) click()
                        state[0] = false; state[1] = false
                        true
                    }
                    else -> false
                }
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { click() }
    ) { content(focused) }
}

/** Lembra qual item tinha o foco, para devolvê-lo ao voltar de outra tela ou do player. */
object FocusMem {
    var last: String? = null
    var restore: String? = null
    var restoredAt = 0L
}

/** Foco inicial de uma tela: usa [req] se nenhum item lembrado pegou o foco de volta. */
@Composable
fun InitialFocus(req: FocusRequester, key: Any = Unit) {
    LaunchedEffect(key) {
        var waited = 0
        kotlinx.coroutines.delay(60)
        while (FocusMem.restore != null && waited < 500) { kotlinx.coroutines.delay(50); waited += 50 }
        FocusMem.restore = null
        if (System.currentTimeMillis() - FocusMem.restoredAt < 1500) return@LaunchedEffect
        // navegando pelas abas do topo: não puxa o foco para o conteúdo
        if (FocusMem.last?.startsWith("tab:") == true) return@LaunchedEffect
        runCatching { req.requestFocus() }
    }
}

/** Botão em formato de pílula (abas do topo, temporadas, dias). */
@Composable
fun Pill(
    text: String, selected: Boolean, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, requester: FocusRequester? = null,
    size: Float = 10f, padH: Dp = 11.dp, padV: Dp = 5.dp, onClick: () -> Unit = {},
) {
    Focus(modifier, onClick = onClick, onFocus = onFocus, requester = requester) { f ->
        val bg = when { selected -> C.accent; f -> C.focusBg; else -> Color.Transparent }
        Box(
            Modifier.scale(if (f) 1.06f else 1f).clip(RoundedCornerShape(6.dp)).background(bg)
                .then(if (f) Modifier.border(1.5.dp, if (selected) C.white else C.accent, RoundedCornerShape(6.dp)) else Modifier)
                .padding(horizontal = padH, vertical = padV)
        ) {
            Txt(text, F.s(size.sp, if (selected || f) C.white else C.dim, FontWeight.SemiBold))
        }
    }
}

@Composable
fun ChipPill(
    text: String, selected: Boolean, modifier: Modifier = Modifier, onFocus: () -> Unit = {}, requester: FocusRequester? = null,
    onClick: () -> Unit = {},
) {
    Focus(modifier, onClick = onClick, onFocus = onFocus, requester = requester) { f ->
        val bg = when { selected -> C.accent; f -> C.focusBg; else -> C.chip }
        Box(
            Modifier.clip(RoundedCornerShape(6.dp)).background(bg)
                .then(if (f) Modifier.border(1.5.dp, C.white, RoundedCornerShape(6.dp)) else Modifier)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) { Txt(text, F.s(8.5.sp, if (selected || f) C.white else C.dim, FontWeight.SemiBold)) }
    }
}

/** Botão de ação (Assistir, Minha lista, Trailer...). */
@Composable
fun ActionButton(
    text: String, modifier: Modifier = Modifier, primary: Boolean = false, icon: String? = null, requester: FocusRequester? = null,
    onFocus: () -> Unit = {}, onClick: () -> Unit,
) {
    Focus(modifier, onClick = onClick, requester = requester, onFocus = onFocus) { f ->
        val shape = RoundedCornerShape(7.dp)
        Row(
            Modifier.scale(if (f) 1.05f else 1f)
                .then(if (f) Modifier.border(2.dp, C.white, shape) else Modifier.border(1.dp, if (primary) C.accent else C.panelBorder, shape))
                .clip(shape)
                .background(if (primary) C.accent else if (f) C.focusBg else Color(0xCC1E1C34))
                .padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) { Txt(icon, F.s(11.sp, C.white, FontWeight.Bold)); Spacer(Modifier.width(6.dp)) }
            Txt(text, F.s(10.5.sp, C.white, FontWeight.Bold))
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp)).background(C.panel).border(1.dp, C.panelBorder, RoundedCornerShape(10.dp)),
        content = content
    )
}

@Composable
fun PanelHeader(left: String, right: String = "", modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(left.uppercase(), F.label, Modifier.weight(1f))
        if (right.isNotEmpty()) Txt(right.uppercase(), F.label)
    }
}

/** Contador em pílula (12, 64, 1.284). */
@Composable
fun Count(n: Int, highlight: Boolean) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(if (highlight) C.accent else Color(0x22FFFFFF)).padding(horizontal = 6.dp, vertical = 2.dp)
    ) { Txt(fmtCount(n), F.s(8.sp, if (highlight) C.white else C.faint, FontWeight.SemiBold)) }
}

fun fmtCount(n: Int): String = if (n >= 1000) String.format(Locale("pt", "BR"), "%,d", n).replace(',', '.') else n.toString()

/** Campo de busca: aparece como item focável; OK abre o teclado. Assim o teclado não sobe só por passar o foco. */
@Composable
fun SearchField(
    value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier,
    requester: FocusRequester? = null, onFocus: () -> Unit = {},
) {
    var editing by remember { mutableStateOf(false) }
    val fieldReq = remember { FocusRequester() }
    val shape = RoundedCornerShape(7.dp)
    if (editing) {
        var had by remember { mutableStateOf(false) }
        BasicTextField(
            value, onChange,
            modifier.fillMaxWidth().height(30.dp).clip(shape).background(C.field).border(1.5.dp, C.accent, shape)
                .focusRequester(fieldReq)
                .onFocusChanged { if (it.isFocused) had = true else if (had) editing = false }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            singleLine = true,
            textStyle = F.s(9.5.sp, C.white),
            cursorBrush = SolidColor(C.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { editing = false; runCatching { requester?.requestFocus() } }, onDone = { editing = false }),
        )
        LaunchedEffect(Unit) { runCatching { fieldReq.requestFocus() } }
    } else {
        Focus(modifier, onClick = { editing = true }, requester = requester, onFocus = onFocus) { f ->
            Row(
                Modifier.fillMaxWidth().height(30.dp).clip(shape).background(C.field)
                    .border(if (f) 1.5.dp else 1.dp, if (f) C.accent else C.panelBorder, shape).padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Txt("🔍", F.s(8.sp, C.dim)); Spacer(Modifier.width(5.dp))
                Txt(value.ifEmpty { placeholder }, F.s(10.5.sp, if (value.isEmpty()) C.faint else C.white), Modifier.weight(1f))
                if (value.isNotEmpty() && f) Txt("OK editar", F.s(7.sp, C.faint))
            }
        }
    }
}

/** Campo de formulário (login). */
@Composable
fun FormField(
    label: String, value: String, onChange: (String) -> Unit, placeholder: String, password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text, requester: FocusRequester? = null, onNext: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Column {
        Txt(label, F.s(7.5.sp, C.dim, FontWeight.Medium))
        Spacer(Modifier.height(3.dp))
        val shape = RoundedCornerShape(6.dp)
        BasicTextField(
            value, onChange,
            Modifier.fillMaxWidth().height(28.dp).clip(shape).background(C.field)
                .border(if (focused) 1.5.dp else 1.dp, if (focused) C.cyan else C.panelBorder, shape)
                .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 9.dp, vertical = 7.dp),
            singleLine = true,
            textStyle = F.s(9.sp, C.white),
            cursorBrush = SolidColor(C.cyan),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() }),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Txt(placeholder, F.s(9.sp, C.faint))
                    inner()
                }
            }
        )
    }
}

/** Logo do canal: imagem do provedor ou caixa colorida com a sigla. */
@Composable
fun ChannelLogo(c: Channel, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(modifier.clip(shape).background(C.hash(c.name).copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Txt(abbrev(c.name), F.s(6.5.sp, C.white, FontWeight.Bold), Modifier.padding(2.dp))
        if (c.logo != null) {
            Box(Modifier.fillMaxSize().background(Color(0xFF1A1830)).padding(3.dp)) {
                AsyncImage(c.logo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

fun abbrev(name: String): String {
    val clean = name.replace(Regex("""(?i)\b(fhd|uhd|hd|sd|4k|h265|hevc)\b|[\[\]\(\)\{\}|]"""), " ").trim()
    val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return "TV"
    val first = words[0].take(5).uppercase()
    val num = words.drop(1).firstOrNull { it.all(Char::isDigit) }
    return if (num != null) "$first $num".take(7) else first
}

/** Capa gerada quando o filme/série não tem imagem (gradiente + título, como no design). */
@Composable
fun GeneratedArt(title: String, modifier: Modifier = Modifier, big: Boolean = false) {
    val c = C.hash(title)
    Box(
        modifier.background(Brush.linearGradient(listOf(c.copy(alpha = 0.95f), Color(0xFF15122E)))),
        contentAlignment = Alignment.BottomStart
    ) {
        Txt(title, F.s(if (big) 12.sp else 9.5.sp, C.white, FontWeight.Bold, F.outfit), Modifier.padding(8.dp), maxLines = 3)
    }
}

/** Pôster vertical (Lançamentos etc). */
@Composable
fun Poster(
    title: String, image: String?, modifier: Modifier = Modifier, width: Dp = 84.dp, rating: Float = 0f, onFocus: () -> Unit = {},
    requester: FocusRequester? = null, onLongClick: (() -> Unit)? = null, memKey: String? = null, onClick: () -> Unit,
) {
    Focus(modifier.width(width), onClick = onClick, onFocus = onFocus, requester = requester, onLongClick = onLongClick, memKey = memKey) { f ->
        val shape = RoundedCornerShape(7.dp)
        Box(
            Modifier.scale(if (f) 1.08f else 1f).width(width).height(width * 1.5f).clip(shape)
                .then(if (f) Modifier.border(2.dp, C.white, shape) else Modifier)
        ) {
            GeneratedArt(title, Modifier.fillMaxSize())
            if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (rating > 0f) {
                Box(Modifier.align(Alignment.TopEnd).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xCC000000)).padding(horizontal = 3.dp, vertical = 1.dp)) {
                    Txt("★ " + String.format(Locale.US, "%.1f", rating), F.s(6.5.sp, C.star, FontWeight.Bold))
                }
            }
            if (f) {
                Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000)))).padding(5.dp)) {
                    Txt(title, F.s(7.5.sp, C.white, FontWeight.SemiBold), maxLines = 2)
                }
            }
        }
    }
}

/** Cartão largo com barra de progresso (Continuar assistindo). */
@Composable
fun WideCard(
    title: String, image: String?, progress: Float, caption: String, modifier: Modifier = Modifier,
    onFocus: () -> Unit = {}, requester: FocusRequester? = null, onLongClick: (() -> Unit)? = null, memKey: String? = null, onClick: () -> Unit,
) {
    Focus(modifier.width(165.dp), onClick = onClick, onFocus = onFocus, requester = requester, onLongClick = onLongClick, memKey = memKey) { f ->
        Column {
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier.scale(if (f) 1.05f else 1f).fillMaxWidth().height(84.dp).clip(shape)
                    .then(if (f) Modifier.border(2.dp, C.white, shape) else Modifier)
            ) {
                val c = C.hash(title)
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0E1530), c))))
                if (image != null) AsyncImage(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))))
                Txt(title, F.s(11.sp, C.white, FontWeight.Bold, F.outfit), Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 11.dp, end = 8.dp))
                ProgressBar(progress, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.5.dp), C.accent, Color(0x55FFFFFF))
            }
            Spacer(Modifier.height(5.dp))
            Txt(caption, F.s(7.5.sp, C.dim))
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier, color: Color = C.cyan, track: Color = Color(0x33FFFFFF)) {
    Box(modifier.background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
    }
}

@Composable
fun LiveBadge(text: String = "● AO VIVO", modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(5.dp)).background(C.live).padding(horizontal = 6.dp, vertical = 3.dp)) {
        Txt(text, F.s(7.5.sp, C.white, FontWeight.Bold, spacing = 0.5.sp))
    }
}

@Composable
fun MetaChip(text: String, color: Color = C.white, bg: Color = Color(0x33FFFFFF)) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.5.dp)) {
        Txt(text, F.s(7.5.sp, color, FontWeight.Bold))
    }
}

/** Escudo do time (formato de escudo, cor estável, iniciais). */
private val shieldShape: Shape = GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height * 0.62f)
    lineTo(size.width / 2f, size.height); lineTo(0f, size.height * 0.62f); close()
}

@Composable
fun TeamShield(team: String, size: Dp = 24.dp, logo: String? = null) {
    if (logo != null) {
        Box(Modifier.width(size).height(size * 1.12f), contentAlignment = Alignment.Center) {
            AsyncImage(logo, null, Modifier.size(size), contentScale = ContentScale.Fit)
        }
        return
    }
    val c = C.hash(MatchLogic.norm(team).lowercase())
    Box(Modifier.width(size).height(size * 1.12f).clip(shieldShape).background(c), contentAlignment = Alignment.Center) {
        Txt(MatchLogic.initials(team), F.s((size.value * 0.3f).sp, C.white, FontWeight.Bold, F.outfit), Modifier.padding(bottom = size * 0.15f))
    }
}

/** Barra inferior de dicas do controle: "OK Tela cheia  ▲▼ Trocar canal ..." */
@Composable
fun HintBar(hints: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        for ((k, v) in hints) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 5.dp, vertical = 1.5.dp)) {
                    Txt(k, F.s(8.5.sp, C.white, FontWeight.Bold))
                }
                Spacer(Modifier.width(5.dp))
                Txt(v, F.s(9.sp, C.dim))
            }
        }
    }
}

@Composable
fun Logo(modifier: Modifier = Modifier, size: Float = 13f) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.logo_mark), null, Modifier.size((size * 1.75f).dp))
        Spacer(Modifier.width(8.dp))
        Txt("ORBITA", F.s(size.sp, C.white, FontWeight.Bold, F.outfit, 0.5.sp))
        Spacer(Modifier.width(5.dp))
        Txt("PLAY", F.s(size.sp, C.accentText, FontWeight.Bold, F.outfit, 0.5.sp))
    }
}

@Composable
fun Loading(text: String, modifier: Modifier = Modifier, progress: Float? = null) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Spinner()
        Spacer(Modifier.height(10.dp))
        Txt(text, F.s(9.sp, C.dim), align = TextAlign.Center, maxLines = 2)
        if (progress != null) {
            Spacer(Modifier.height(8.dp))
            ProgressBar(progress, Modifier.width(140.dp).height(3.dp).clip(RoundedCornerShape(2.dp)), C.accent)
        }
    }
}

@Composable
fun Spinner(diameter: Dp = 22.dp) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val a by t.animateFloat(
        0f, 360f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing)),
        label = "a"
    )
    androidx.compose.foundation.Canvas(Modifier.size(diameter)) {
        drawArc(C.accent, a, 270f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = diameter.toPx() / 8f))
    }
}

@Composable
fun EmptyState(title: String, subtitle: String = "", modifier: Modifier = Modifier) {
    Column(modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Txt(title, F.s(11.sp, C.text, FontWeight.SemiBold), align = TextAlign.Center, maxLines = 2)
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            Txt(subtitle, F.s(8.5.sp, C.dim), align = TextAlign.Center, maxLines = 4)
        }
    }
}

private val hm = SimpleDateFormat("HH:mm", Locale("pt", "BR"))
fun hhmm(t: Long): String = synchronized(hm) { hm.format(Date(t)) }

fun fmtDuration(sec: Int): String {
    if (sec <= 0) return ""
    val h = sec / 3600; val m = (sec % 3600) / 60
    return if (h > 0) "${h}h ${"%02d".format(m)}min" else "$m min"
}

fun fmtTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
}

package com.orbita.play

import android.content.Context
import android.view.KeyEvent as AKey
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C as MC
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/** Um único ExoPlayer para a prévia e a tela cheia. */
class Playback(ctx: Context, private val vm: Vm) {
    val exo: ExoPlayer
    var current by mutableStateOf<PlayItem?>(null); private set
    /** Lista para ▲▼ (canais da categoria) ou próximos episódios. */
    var list: List<PlayItem> = emptyList(); private set
    var fullscreen by mutableStateOf(false)
    var surfaceEpoch by mutableIntStateOf(0)
    var error by mutableStateOf<String?>(null); private set
    var buffering by mutableStateOf(false); private set
    var isPlaying by mutableStateOf(false); private set
    var resize by mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    private var retries = 0

    init {
        val http = DefaultHttpDataSource.Factory().setUserAgent(Net.UA).setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000).setReadTimeoutMs(25_000)
        val rf = DefaultRenderersFactory(ctx).setEnableDecoderFallback(true)
        val lc = DefaultLoadControl.Builder().setBufferDurationsMs(15_000, 50_000, 1_500, 3_000).build()
        exo = ExoPlayer.Builder(ctx, rf)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(ctx, http)))
            .setLoadControl(lc)
            .setHandleAudioBecomingNoisy(true)
            .build()
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setPreferredAudioLanguage("pt").setPreferredTextLanguage("pt").build()
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) { retries = 0; error = null }
                if (state == Player.STATE_ENDED) onEnded()
            }

            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }

            override fun onPlayerError(e: PlaybackException) {
                val cur = current ?: return
                if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    exo.seekToDefaultPosition(); exo.prepare(); return
                }
                if (cur.kind == Kind.LIVE && retries < 30) {
                    retries++
                    error = "Reconectando… ($retries)"
                    vm.scope.launch { delay(if (retries < 3) 1_000L else 3_000L); if (current === cur) { exo.prepare(); exo.play() } }
                } else if (cur.kind != Kind.LIVE && retries < 3) {
                    retries++
                    val pos = exo.currentPosition
                    vm.scope.launch { delay(1_500); if (current === cur) { exo.seekTo(pos); exo.prepare(); exo.play() } }
                } else {
                    error = errorText(e)
                }
            }
        })
    }

    private fun errorText(e: PlaybackException) = when (e.errorCode) {
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "O servidor recusou o vídeo. Pode ser limite de conexões da sua assinatura."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Sem conexão com o servidor. Verifique a internet."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "Formato de vídeo não suportado por este aparelho."
        else -> "Não foi possível reproduzir (${e.errorCodeName})."
    }

    fun play(item: PlayItem, items: List<PlayItem> = emptyList(), startMs: Long = 0L) {
        saveProgress()
        current = item
        list = items
        error = null; retries = 0
        val b = MediaItem.Builder().setUri(item.url).setMediaId(item.key)
        val u = item.url.lowercase()
        if (u.contains(".m3u8")) b.setMimeType(MimeTypes.APPLICATION_M3U8) else if (u.contains(".mpd")) b.setMimeType(MimeTypes.APPLICATION_MPD)
        exo.setMediaItem(b.build(), if (startMs > 0) startMs else MC.TIME_UNSET)
        exo.prepare()
        exo.playWhenReady = true
        vm.play(item)
    }

    fun retry() { val c = current ?: return; play(c, list, if (c.kind == Kind.LIVE) 0 else exo.currentPosition) }

    fun stop() {
        saveProgress()
        exo.stop(); exo.clearMediaItems()
        current = null; list = emptyList(); fullscreen = false
    }

    fun neighbor(delta: Int): PlayItem? {
        val cur = current ?: return null
        if (list.isEmpty()) return null
        val i = list.indexOfFirst { it.key == cur.key }
        if (i < 0) return null
        val n = i + delta
        return if (cur.kind == Kind.LIVE) list[((n % list.size) + list.size) % list.size] else list.getOrNull(n)
    }

    private fun onEnded() {
        val cur = current ?: return
        if (cur.kind == Kind.LIVE) { exo.prepare(); return }
        saveProgress(ended = true)
        if (cur.kind == Kind.EPISODE && vm.store.autoNext.value) {
            neighbor(1)?.let { play(it, list); return }
        }
        fullscreen = false
    }

    fun saveProgress(ended: Boolean = false) {
        val cur = current ?: return
        if (cur.kind == Kind.LIVE) return
        val dur = exo.duration.takeIf { it > 0 } ?: return
        val pos = if (ended) dur else exo.currentPosition
        vm.store.saveProgress(
            Progress(
                cur.key, cur.kind, cur.title, cur.subtitle, cur.image, pos, dur, System.currentTimeMillis(),
                cur.series?.id, cur.episode?.season ?: 0, cur.episode?.num ?: 0, cur.url
            )
        )
    }

    // Faixas de áudio e legenda
    class Track(val group: Tracks.Group, val index: Int, val name: String, val selected: Boolean)

    fun tracks(type: Int): List<Track> {
        val out = ArrayList<Track>()
        for (g in exo.currentTracks.groups) {
            if (g.type != type) continue
            for (i in 0 until g.length) {
                if (!g.isTrackSupported(i)) continue
                out += Track(g, i, trackName(g.getTrackFormat(i), out.size + 1), g.isTrackSelected(i))
            }
        }
        return out
    }

    private fun trackName(f: Format, n: Int): String {
        val lang = f.language?.takeIf { it.isNotBlank() && it != "und" }?.let { Locale.forLanguageTag(it).getDisplayLanguage(Locale("pt", "BR")) }
            ?.replaceFirstChar { it.uppercase() }
        val label = f.label?.takeIf { it.isNotBlank() }
        val ch = if (f.channelCount > 2) " ${f.channelCount - 1}.1" else ""
        return listOfNotNull(label ?: lang ?: "Faixa $n").first() + ch
    }

    fun select(t: Track?, type: Int) {
        val b = exo.trackSelectionParameters.buildUpon()
        if (t == null) b.setTrackTypeDisabled(type, true)
        else b.setTrackTypeDisabled(type, false).setOverrideForType(TrackSelectionOverride(t.group.mediaTrackGroup, t.index))
        exo.trackSelectionParameters = b.build()
    }

    fun release() { saveProgress(); exo.release() }
}

/** Superfície de vídeo. Recriada quando [Playback.surfaceEpoch] muda, para retomar a imagem depois da tela cheia. */
@Composable
fun VideoSurface(pb: Playback, modifier: Modifier = Modifier) {
    key(pb.surfaceEpoch) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    player = pb.exo
                }
            },
            update = { v -> v.resizeMode = pb.resize },
            onRelease = { v -> v.player = null },
            modifier = modifier,
        )
    }
}

private enum class PPanel { NONE, CHANNELS, OPTIONS }

/** Player em tela cheia com controles do controle remoto. */
@Composable
fun FullPlayer(vm: Vm, pb: Playback) {
    val item = pb.current ?: return
    val live = item.kind == Kind.LIVE
    var overlay by remember { mutableStateOf(true) }
    var overlayTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var panel by remember { mutableStateOf(PPanel.NONE) }
    var digits by remember { mutableStateOf("") }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var seekTarget by remember { mutableLongStateOf(-1L) }
    val root = remember { FocusRequester() }

    fun showOverlay() { overlay = true; overlayTick = System.currentTimeMillis() }
    fun close() {
        pb.saveProgress()
        pb.fullscreen = false
        pb.surfaceEpoch++
        if (!live) pb.stop()
    }

    LaunchedEffect(Unit) { runCatching { root.requestFocus() } }
    LaunchedEffect(panel) { if (panel == PPanel.NONE) { delay(50); runCatching { root.requestFocus() } } }
    LaunchedEffect(overlayTick, overlay) {
        if (overlay) { delay(if (live) 4_500 else 4_000); if (pb.isPlaying || live) overlay = false }
    }
    LaunchedEffect(item.key) { showOverlay(); if (live) item.channel?.let { vm.requestEpg(it) } }
    LaunchedEffect(Unit) {
        var n = 0
        while (true) {
            pos = pb.exo.currentPosition; dur = pb.exo.duration.coerceAtLeast(0)
            if (++n % 20 == 0) pb.saveProgress()
            delay(500)
        }
    }
    LaunchedEffect(digits) {
        if (digits.isEmpty()) return@LaunchedEffect
        delay(1_800)
        val num = digits.toIntOrNull()
        digits = ""
        val target = vm.channels.firstOrNull { it.num == num } ?: return@LaunchedEffect
        val items = pb.list.ifEmpty { listOf(liveItem(target)) }
        pb.play(items.firstOrNull { it.channel?.id == target.id } ?: liveItem(target), items)
    }
    DisposableEffect(Unit) { onDispose { } }

    BackHandler {
        when {
            panel != PPanel.NONE -> panel = PPanel.NONE
            digits.isNotEmpty() -> digits = ""
            overlay && !live -> overlay = false
            else -> close()
        }
    }

    fun seekBy(ms: Long) {
        val base = if (seekTarget >= 0) seekTarget else pb.exo.currentPosition
        val t = (base + ms).coerceIn(0, (pb.exo.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
        seekTarget = t
        pb.exo.seekTo(t)
        showOverlay()
    }
    LaunchedEffect(seekTarget) { if (seekTarget >= 0) { delay(800); seekTarget = -1 } }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .focusRequester(root)
            .onPreviewKeyEvent { ev ->
                if (panel != PPanel.NONE) return@onPreviewKeyEvent false
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent isSelectKey(ev.key)
                val code = ev.nativeKeyEvent.keyCode
                val rep = ev.nativeKeyEvent.repeatCount
                if (code in AKey.KEYCODE_0..AKey.KEYCODE_9 && live) {
                    if (digits.length < 5) digits += (code - AKey.KEYCODE_0).toString(); return@onPreviewKeyEvent true
                }
                when (code) {
                    AKey.KEYCODE_MENU, AKey.KEYCODE_SETTINGS -> { panel = PPanel.OPTIONS; true }
                    AKey.KEYCODE_INFO, AKey.KEYCODE_GUIDE -> { if (overlay) overlay = false else showOverlay(); true }
                    AKey.KEYCODE_CHANNEL_UP -> { pb.neighbor(1)?.let { pb.play(it, pb.list) }; true }
                    AKey.KEYCODE_CHANNEL_DOWN -> { pb.neighbor(-1)?.let { pb.play(it, pb.list) }; true }
                    AKey.KEYCODE_MEDIA_PLAY_PAUSE, AKey.KEYCODE_MEDIA_PLAY, AKey.KEYCODE_MEDIA_PAUSE -> {
                        if (pb.exo.isPlaying) pb.exo.pause() else pb.exo.play(); showOverlay(); true
                    }
                    AKey.KEYCODE_MEDIA_FAST_FORWARD -> { if (!live) seekBy(30_000); true }
                    AKey.KEYCODE_MEDIA_REWIND -> { if (!live) seekBy(-30_000); true }
                    AKey.KEYCODE_MEDIA_NEXT -> { pb.neighbor(1)?.let { pb.play(it, pb.list) }; true }
                    AKey.KEYCODE_MEDIA_PREVIOUS -> { pb.neighbor(-1)?.let { pb.play(it, pb.list) }; true }
                    AKey.KEYCODE_DPAD_CENTER, AKey.KEYCODE_ENTER, AKey.KEYCODE_NUMPAD_ENTER -> {
                        if (rep > 0) return@onPreviewKeyEvent true
                        if (pb.error != null && !live) pb.retry()
                        else if (live) panel = PPanel.CHANNELS
                        else { if (pb.exo.isPlaying) pb.exo.pause() else pb.exo.play(); showOverlay() }
                        true
                    }
                    AKey.KEYCODE_DPAD_UP -> { if (live) pb.neighbor(-1)?.let { pb.play(it, pb.list) } else showOverlay(); true }
                    AKey.KEYCODE_DPAD_DOWN -> { if (live) pb.neighbor(1)?.let { pb.play(it, pb.list) } else if (overlay) panel = PPanel.OPTIONS else showOverlay(); true }
                    AKey.KEYCODE_DPAD_LEFT -> { if (live) panel = PPanel.CHANNELS else seekBy(-10_000L * (1 + rep / 4)); true }
                    AKey.KEYCODE_DPAD_RIGHT -> { if (live) showOverlay() else seekBy(10_000L * (1 + rep / 4)); true }
                    else -> false
                }
            }
            .focusable()
    ) {
        VideoSurface(pb, Modifier.fillMaxSize())

        if (pb.buffering && pb.error == null) Spinner(Modifier.align(Alignment.Center), 36.dp)
        pb.error?.let { msg ->
            Column(
                Modifier.align(Alignment.Center).clip(RoundedCornerShape(10.dp)).background(Color(0xE6131229)).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Txt(msg, F.s(10.sp, C.white, FontWeight.SemiBold), maxLines = 3)
                Spacer(Modifier.height(6.dp))
                Txt(if (live) "▲▼ trocar de canal · VOLTAR sair" else "OK tentar de novo · VOLTAR sair", F.s(8.sp, C.dim))
            }
        }

        if (digits.isNotEmpty()) {
            Box(Modifier.align(Alignment.TopEnd).padding(24.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xE6131229)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Txt(digits, F.s(22.sp, C.white, FontWeight.Bold, F.outfit))
            }
        }

        if (overlay && panel == PPanel.NONE) {
            if (live) LiveOverlay(vm, item, Modifier.align(Alignment.BottomCenter))
            else VodOverlay(item, if (seekTarget >= 0) seekTarget else pos, dur, pb.isPlaying, Modifier.align(Alignment.BottomCenter))
            Txt(hhmm(System.currentTimeMillis()), F.s(12.sp, C.white, FontWeight.Bold), Modifier.align(Alignment.TopEnd).padding(20.dp))
        }

        when (panel) {
            PPanel.CHANNELS -> ChannelPanel(vm, pb) { panel = PPanel.NONE }
            PPanel.OPTIONS -> OptionsPanel(pb, Modifier.align(Alignment.CenterEnd)) { panel = PPanel.NONE }
            PPanel.NONE -> Unit
        }
    }
}

@Composable
fun Spinner(modifier: Modifier, size: androidx.compose.ui.unit.Dp) { Box(modifier) { Spinner(size) } }

fun liveItem(c: Channel) = PlayItem(Kind.LIVE, "live:${c.id}", c.name, "", c.url, c.logo, channel = c)

@Composable
private fun LiveOverlay(vm: Vm, item: PlayItem, modifier: Modifier) {
    val c = item.channel ?: return
    val progs = vm.epg[c.id].orEmpty()
    val now = progs.firstOrNull { it.isNow() }
    val next = progs.firstOrNull { it.start >= (now?.end ?: System.currentTimeMillis()) }
    Box(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF00B0A1C)))).padding(horizontal = 28.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(c, Modifier.size(54.dp, 40.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveBadge()
                    Spacer(Modifier.width(8.dp))
                    Txt("${c.num} · ${c.name}", F.s(15.sp, C.white, FontWeight.Bold, F.outfit))
                }
                Spacer(Modifier.height(4.dp))
                if (now != null) {
                    Txt("${now.title}  ·  ${hhmm(now.start)} – ${hhmm(now.end)}", F.s(9.5.sp, C.text))
                    Spacer(Modifier.height(5.dp))
                    ProgressBar(now.progress(), Modifier.fillMaxWidth(0.7f).height(3.dp).clip(RoundedCornerShape(2.dp)))
                    if (next != null) { Spacer(Modifier.height(5.dp)); Txt("A seguir: ${hhmm(next.start)} ${next.title}", F.s(8.5.sp, C.dim)) }
                } else Txt(vm.catName(c.catId), F.s(9.sp, C.dim))
                Spacer(Modifier.height(7.dp))
                Txt("OK lista de canais   ▲▼ trocar canal   0-9 número   MENU áudio/legenda", F.s(7.5.sp, C.faint))
            }
        }
    }
}

@Composable
private fun VodOverlay(item: PlayItem, pos: Long, dur: Long, playing: Boolean, modifier: Modifier) {
    Box(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF00B0A1C)))).padding(horizontal = 28.dp, vertical = 22.dp)) {
        Column {
            Txt(item.title, F.s(16.sp, C.white, FontWeight.Bold, F.outfit))
            if (item.subtitle.isNotEmpty()) Txt(item.subtitle, F.s(9.sp, C.dim))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(if (playing) "❚❚" else "▶", F.s(12.sp, C.white, FontWeight.Bold), Modifier.width(22.dp))
                Txt(fmtTime(pos), F.s(8.5.sp, C.text), Modifier.width(52.dp))
                ProgressBar(if (dur > 0) pos.toFloat() / dur else 0f, Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)), C.accent)
                Spacer(Modifier.width(10.dp))
                Txt(fmtTime(dur), F.s(8.5.sp, C.text))
            }
            Spacer(Modifier.height(6.dp))
            Txt("OK pausar   ◀▶ voltar/avançar 10s (segure p/ acelerar)   ▼ ou MENU áudio/legenda", F.s(7.5.sp, C.faint))
        }
    }
}

@Composable
private fun ChannelPanel(vm: Vm, pb: Playback, onClose: () -> Unit) {
    val items = pb.list.ifEmpty { pb.current?.let { listOf(it) } ?: emptyList() }
    val curIdx = items.indexOfFirst { it.key == pb.current?.key }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (curIdx - 3).coerceAtLeast(0))
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(30); runCatching { first.requestFocus() } }
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(300.dp).fillMaxHeight().background(Color(0xF20E0D22)).border(1.dp, C.panelBorder)) {
            Column {
                PanelHeader("Canais", "${items.size}")
                LazyColumn(state = state, modifier = Modifier.padding(horizontal = 6.dp)) {
                    itemsIndexed(items, key = { _, it -> it.key }) { i, it ->
                        val c = it.channel
                        LaunchedEffect(c?.id) { c?.let { ch -> vm.requestEpg(ch, 4) } }
                        Focus(
                            Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            requester = if (i == curIdx) first else null,
                            onClick = { if (it.key != pb.current?.key) pb.play(it, pb.list); onClose() },
                            onKey = { ev ->
                                if (ev.type == KeyEventType.KeyDown && (ev.key == Key.DirectionLeft || ev.key == Key.DirectionRight)) { onClose(); true } else false
                            }
                        ) { f ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (f) C.focusBg else Color.Transparent)
                                    .then(if (f) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(6.dp)) else Modifier)
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Txt(c?.num?.toString() ?: "", F.s(8.5.sp, if (it.key == pb.current?.key) C.cyan else C.dim, FontWeight.SemiBold), Modifier.width(30.dp))
                                if (c != null) ChannelLogo(c, Modifier.size(30.dp, 22.dp))
                                Spacer(Modifier.width(7.dp))
                                Column(Modifier.weight(1f)) {
                                    Txt(it.title, F.s(9.sp, C.white, FontWeight.SemiBold))
                                    val now = c?.let { ch -> vm.nowOn(ch) }
                                    if (now != null) Txt(now.title, F.s(7.5.sp, C.dim))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionsPanel(pb: Playback, modifier: Modifier, onClose: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(30); runCatching { first.requestFocus() } }
    val item = pb.current
    @Suppress("UNUSED_VARIABLE") val t = tick
    val audio = pb.tracks(MC.TRACK_TYPE_AUDIO)
    val subs = pb.tracks(MC.TRACK_TYPE_TEXT)
    val aCur = audio.indexOfFirst { it.selected }
    val sCur = subs.indexOfFirst { it.selected }
    val modes = listOf(AspectRatioFrameLayout.RESIZE_MODE_FIT to "Original", AspectRatioFrameLayout.RESIZE_MODE_FILL to "Esticar", AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom")
    val mIdx = modes.indexOfFirst { it.first == pb.resize }.coerceAtLeast(0)
    Column(
        modifier.padding(24.dp).width(250.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xF2131229)).border(1.dp, C.panelBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Txt("OPÇÕES", F.label, Modifier.padding(4.dp))
        OptionRow("Áudio", if (aCur >= 0) audio[aCur].name else if (audio.isEmpty()) "Padrão" else "Automático", first) {
            if (audio.isNotEmpty()) { pb.select(audio[(aCur + 1) % audio.size], MC.TRACK_TYPE_AUDIO); tick++ }
        }
        OptionRow("Legendas", if (sCur >= 0) subs[sCur].name else if (subs.isEmpty()) "Nenhuma" else "Desligadas") {
            if (subs.isNotEmpty()) {
                val next = sCur + 1
                pb.select(if (next >= subs.size) null else subs[next], MC.TRACK_TYPE_TEXT); tick++
            }
        }
        OptionRow("Tela", modes[mIdx].second) { pb.resize = modes[(mIdx + 1) % modes.size].first; tick++ }
        if (item?.kind == Kind.EPISODE) {
            pb.neighbor(1)?.let { n -> OptionRow("Próximo episódio", n.title) { pb.play(n, pb.list); onClose() } }
        }
        if (item?.kind == Kind.LIVE) OptionRow("Reconectar", "") { pb.retry(); onClose() }
        Txt("OK altera · VOLTAR fecha", F.s(7.sp, C.faint), Modifier.padding(4.dp))
    }
    LaunchedEffect(Unit) {
        // faixas chegam depois do início da reprodução
        repeat(6) { delay(1_000); tick++ }
    }
}

@Composable
private fun OptionRow(label: String, value: String, requester: FocusRequester? = null, onClick: () -> Unit) {
    Focus(Modifier.fillMaxWidth(), onClick = onClick, requester = requester) { f ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (f) C.focusBg else Color.Transparent)
                .then(if (f) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(6.dp)) else Modifier)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt(label, F.s(9.sp, C.white, FontWeight.SemiBold), Modifier.weight(1f))
            Txt(value, F.s(8.5.sp, C.cyan, FontWeight.Medium))
        }
    }
}

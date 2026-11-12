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
        var i = list.indexOfFirst { it.key == cur.key }
        // trocou de qualidade (canal fora da lista): usa a posição do canal "irmão"
        if (i < 0 && cur.channel != null) {
            val k = FootballApi.channelKey(cur.channel.name)
            i = list.indexOfFirst { it.channel?.let { c -> FootballApi.channelKey(c.name) } == k }
        }
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
    val qualityReq = remember { FocusRequester() }

    fun showOverlay() { overlay = true; overlayTick = System.currentTimeMillis() }
    fun close() {
        pb.saveProgress()
        pb.fullscreen = false
        pb.surfaceEpoch++
        if (!live) pb.stop()
    }

    LaunchedEffect(Unit) { runCatching { root.requestFocus() } }
    LaunchedEffect(panel) { if (panel == PPanel.NONE) { delay(50); runCatching { root.requestFocus() } } }
    // ao vivo: com as informações abertas o foco vai para a qualidade; ao fechar, volta para o player
    LaunchedEffect(overlay, panel) {
        if (!live || panel != PPanel.NONE) return@LaunchedEffect
        delay(30)
        runCatching { if (overlay) qualityReq.requestFocus() else root.requestFocus() }
    }
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
            overlay -> overlay = false
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
                // ao vivo com informações abertas: ◀▶ e OK ficam com os botões de qualidade
                if (live && overlay && (isSelectKey(ev.key) || ev.key == Key.DirectionLeft || ev.key == Key.DirectionRight)) {
                    if (ev.type == KeyEventType.KeyDown) showOverlay()
                    return@onPreviewKeyEvent false
                }
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
                        else if (live) showOverlay()
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
                Modifier.align(Alignment.Center).clip(RoundedCornerShape(10.dp)).background(Color(0xE60C0A16)).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Txt(msg, F.s(10.sp, C.white, FontWeight.SemiBold), maxLines = 3)
                Spacer(Modifier.height(6.dp))
                Txt(if (live) "▲▼ trocar de canal · VOLTAR sair" else "OK tentar de novo · VOLTAR sair", F.s(8.sp, C.dim))
            }
        }

        if (digits.isNotEmpty()) {
            Box(Modifier.align(Alignment.TopEnd).padding(24.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xE60C0A16)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Txt(digits, F.s(22.sp, C.white, FontWeight.Bold, F.outfit))
            }
        }

        if (overlay && panel == PPanel.NONE) {
            if (live) LiveOverlay(vm, pb, item, qualityReq, Modifier.fillMaxSize())
            else {
                VodOverlay(item, if (seekTarget >= 0) seekTarget else pos, dur, pb.isPlaying, Modifier.align(Alignment.BottomCenter))
                Txt(hhmm(System.currentTimeMillis()), F.s(12.sp, C.white, FontWeight.Bold), Modifier.align(Alignment.TopEnd).padding(20.dp))
            }
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

/** Qualidade pelo nome do canal: "AgroMais FHD" → FHD. */
object Quality {
    val order = listOf("UHD", "FHD", "HD", "SD")
    private val uhd = Regex("""(?i)\b(uhd|4k|2160p?)\b""")
    private val fhd = Regex("""(?i)\b(fhd|full\s?hd|1080p?)\b""")
    private val hd = Regex("""(?i)\b(hd|720p?)\b""")
    private val sd = Regex("""(?i)\b(sd|480p?)\b""")
    /**
     * Uma linha por canal: "Globo SP FHD/FHD²/HD/SD" viram um só (a qualidade se escolhe no player).
     * Fica a versão preferida (FHD, depois HD, 4K, sem marcação, SD; a principal antes da "²").
     */
    fun unique(list: List<Channel>): List<Channel> {
        val groups = LinkedHashMap<String, MutableList<Channel>>()
        for (c in list) groups.getOrPut(FootballApi.channelKey(c.name).ifEmpty { c.id }) { ArrayList() } += c
        return groups.values.map { g -> if (g.size == 1) g[0] else g.minByOrNull { rank(it.name) }!! }
    }

    private fun rank(name: String): Int {
        val q = when (of(name)) { "FHD" -> 0; "HD" -> 10; "UHD" -> 20; null -> 30; else -> 40 }
        val alt = if (name.contains('²') || name.contains(Regex("""(?i)\b(alt|backup|2)\s*$"""))) 5 else 0
        return q + alt
    }

    fun of(name: String): String? = when {
        uhd.containsMatchIn(name) -> "UHD"
        fhd.containsMatchIn(name) -> "FHD"
        hd.containsMatchIn(name) -> "HD"
        sd.containsMatchIn(name) -> "SD"
        else -> null
    }
}

private val liveBlue = Color(0xFF3D6BFF)

/** Informações do canal ao vivo (layout da tela de referência): canal, programa, progresso, a seguir e qualidade. */
@Composable
private fun LiveOverlay(vm: Vm, pb: Playback, item: PlayItem, qualityReq: FocusRequester, modifier: Modifier) {
    val c = item.channel ?: return
    val progs = vm.epg[c.id].orEmpty()
    val nowMs = System.currentTimeMillis()
    val now = progs.firstOrNull { it.isNow(nowMs) }
    val next = progs.firstOrNull { it.start >= (now?.end ?: nowMs) }
    val list = pb.list
    val pos = list.indexOfFirst { it.key == item.key }.let { if (it < 0) list.indexOfFirst { p -> p.channel?.let { ch -> FootballApi.channelKey(ch.name) } == FootballApi.channelKey(c.name) } else it }
    val variants = remember(c.id, vm.channels) {
        vm.channelIndex()[FootballApi.channelKey(c.name)].orEmpty().mapNotNull { ch -> Quality.of(ch.name)?.let { it to ch } }.distinctBy { it.first }.toMap()
    }
    val q = Quality.of(c.name)

    Box(modifier) {
        // topo: posição na lista e relógio
        Row(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent))).padding(horizontal = 32.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            val where = listOfNotNull("Canais", vm.catName(c.catId).ifBlank { null }).joinToString(" • ") + if (pos >= 0 && list.size > 1) " · ${pos + 1} de ${list.size}" else ""
            Txt(where, F.s(9.5.sp, C.white.copy(alpha = 0.9f), FontWeight.SemiBold))
            Spacer(Modifier.width(12.dp))
            Txt(hhmm(nowMs), F.s(19.sp, C.white, FontWeight.Bold, F.outfit))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.35f to Color(0xB3080A16), 1f to Color(0xF2080A16)))
                .padding(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1A1D33)), contentAlignment = Alignment.Center) {
                    ChannelLogo(c, Modifier.size(46.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(c.num.toString(), F.s(12.sp, C.white.copy(alpha = 0.9f), FontWeight.Medium))
                        Spacer(Modifier.width(7.dp))
                        LiveBadge()
                        if (q != null) {
                            Spacer(Modifier.width(5.dp))
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                                Txt(q, F.s(7.5.sp, C.white, FontWeight.Bold))
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Txt(c.name, F.s(22.sp, C.white, FontWeight.Bold, F.outfit))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (now != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt("${hhmm(now.start)} – ${hhmm(now.end)}", F.s(10.sp, liveBlue.copy(alpha = 0.95f), FontWeight.SemiBold))
                    Spacer(Modifier.width(10.dp))
                    Txt(now.title, F.s(13.sp, C.white, FontWeight.Bold))
                }
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressBar(now.progress(nowMs), Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)), liveBlue, Color(0x55FFFFFF))
                    Spacer(Modifier.width(14.dp))
                    Txt("faltam ${((now.end - nowMs) / 60_000).coerceAtLeast(0)} min", F.s(9.5.sp, C.white, FontWeight.SemiBold))
                }
                if (next != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt("A seguir", F.s(9.5.sp, C.dim))
                        Spacer(Modifier.width(10.dp))
                        Txt(hhmm(next.start), F.s(9.5.sp, C.text, FontWeight.Medium))
                        Spacer(Modifier.width(10.dp))
                        Txt(next.title, F.s(9.5.sp, C.text, FontWeight.Medium))
                    }
                }
            } else {
                Txt(vm.catName(c.catId).ifBlank { "Ao vivo" }, F.s(10.sp, C.dim))
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("Qualidade", F.s(8.5.sp, C.dim), Modifier.padding(end = 10.dp))
                val focusTarget = q?.takeIf { variants.containsKey(it) } ?: Quality.order.firstOrNull { variants.containsKey(it) } ?: Quality.order[2]
                Quality.order.forEach { label ->
                    val target = variants[label]
                    QualityPill(label, selected = label == q, available = target != null, requester = if (label == focusTarget) qualityReq else null) {
                        if (target != null && target.id != c.id) pb.play(liveItem(target), pb.list)
                    }
                    Spacer(Modifier.width(7.dp))
                }
                Spacer(Modifier.width(6.dp))
                Txt(if (q != null) "$q · Assistindo agora" else "Assistindo agora", F.s(9.5.sp, C.white, FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun QualityPill(label: String, selected: Boolean, available: Boolean, requester: FocusRequester?, onClick: () -> Unit) {
    Focus(onClick = onClick, requester = requester) { f ->
        val shape = RoundedCornerShape(8.dp)
        Column(
            Modifier.width(52.dp).height(36.dp).clip(shape)
                .background(if (selected) C.white else if (f) Color(0x55FFFFFF) else Color(0x22FFFFFF))
                .border(if (f) 2.dp else 1.dp, if (f) C.white else Color(0x40FFFFFF), shape),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Txt(label, F.s(10.sp, if (selected) Color(0xFF14172B) else if (available) C.white else C.white.copy(alpha = 0.4f), FontWeight.Bold))
            Spacer(Modifier.height(2.dp))
            Box(Modifier.size(3.5.dp).clip(androidx.compose.foundation.shape.CircleShape).background(
                when { selected -> liveBlue; available -> C.white.copy(alpha = 0.6f); else -> Color.Transparent }
            ))
        }
    }
}

@Composable
private fun VodOverlay(item: PlayItem, pos: Long, dur: Long, playing: Boolean, modifier: Modifier) {
    Box(modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF0050409)))).padding(horizontal = 28.dp, vertical = 22.dp)) {
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

/**
 * Lista do player (◀): categorias à esquerda e canais à direita, com o programa atual e o progresso.
 * Trocar de categoria aqui também muda a lista usada por ▲▼ no player.
 */
@Composable
private fun ChannelPanel(vm: Vm, pb: Playback, onClose: () -> Unit) {
    val cur = pb.current?.channel
    val cats = remember(vm.liveCats) { listOf(Cat("__fav", "★  Favoritos"), Cat("__recent", "🕘  Recentes"), Cat("__all", "Todos os canais")) + vm.liveCats }
    // categoria inicial: a da lista que está tocando (se for uma só), senão a do canal
    val startCat = remember {
        val ids = pb.list.mapNotNull { it.channel?.catId }.distinct()
        when {
            ids.size == 1 -> ids[0]
            pb.list.size > 1 && pb.list.size == vm.channels.size -> "__all"
            pb.list.isNotEmpty() && pb.list.all { vm.store.isFav(it.channel?.id ?: "") } -> "__fav"
            else -> cur?.catId ?: "__all"
        }
    }
    var cat by remember { mutableStateOf(startCat) }
    var pending by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pending) { val p = pending ?: return@LaunchedEffect; delay(200); cat = p }
    val list = remember(cat, vm.channels) { channelsOf(vm, cat) }
    val curIdx = list.indexOfFirst { it.id == cur?.id }
    val chState = rememberLazyListState(initialFirstVisibleItemIndex = (curIdx - 4).coerceAtLeast(0))
    val catIdx = cats.indexOfFirst { it.id == cat }.coerceAtLeast(0)
    val catState = rememberLazyListState(initialFirstVisibleItemIndex = (catIdx - 4).coerceAtLeast(0))
    val chReq = remember { FocusRequester() }
    val catReq = remember { FocusRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) { delay(40); runCatching { chReq.requestFocus() } }
    LaunchedEffect(cat) { if (cat != startCat) chState.scrollToItem(0) }

    fun toChannels() { scope.launch { delay(30); runCatching { chReq.requestFocus() } } }
    fun toCats() { scope.launch { catState.scrollToItem((catIdx - 4).coerceAtLeast(0)); delay(30); runCatching { catReq.requestFocus() } } }

    Box(
        Modifier.fillMaxHeight().width(640.dp)
            .background(Brush.horizontalGradient(0f to Color(0xF5080A18), 0.8f to Color(0xEB050409), 1f to Color.Transparent))
    ) {
        Row(Modifier.fillMaxSize().padding(start = 22.dp, top = 22.dp, bottom = 18.dp, end = 50.dp)) {
            Column(Modifier.width(190.dp).fillMaxHeight()) {
                Txt("CATEGORIAS", F.label, Modifier.padding(start = 10.dp, bottom = 8.dp))
                LazyColumn(state = catState, modifier = Modifier.weight(1f)) {
                    itemsIndexed(cats, key = { _, c -> c.id }) { i, c ->
                        val sel = c.id == cat
                        Focus(
                            Modifier.fillMaxWidth().height(34.dp),
                            requester = if (i == catIdx) catReq else null,
                            onFocus = { pending = c.id },
                            onClick = { cat = c.id; toChannels() },
                            onKey = { ev ->
                                if (ev.type == KeyEventType.KeyDown && ev.key == Key.DirectionRight) { cat = pending ?: cat; toChannels(); true }
                                else ev.type == KeyEventType.KeyDown && ev.key == Key.DirectionLeft
                            }
                        ) { f ->
                            Row(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                                    .background(if (f) C.focusBg else if (sel) C.accentSoft else Color.Transparent)
                                    .then(if (f) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(6.dp)) else Modifier)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Txt(c.name, F.s(11.sp, if (sel || f) C.white else C.text, if (sel) FontWeight.SemiBold else FontWeight.Medium), Modifier.weight(1f))
                                if (sel) Box(Modifier.size(5.dp).clip(androidx.compose.foundation.shape.CircleShape).background(C.accent))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.padding(start = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Txt(cats.firstOrNull { it.id == cat }?.name?.trimStart('★', ' ')?.removePrefix("🕘")?.trim()?.uppercase() ?: "CANAIS", F.label, Modifier.weight(1f))
                    Txt("${list.size} CANAIS", F.label)
                }
                if (list.isEmpty()) {
                    EmptyState("Nenhum canal nesta categoria", "◀ para escolher outra categoria", Modifier.fillMaxWidth())
                    Focus(Modifier.size(1.dp), requester = chReq, onKey = { ev -> if (ev.type == KeyEventType.KeyDown && ev.key == Key.DirectionLeft) { toCats(); true } else false }) { }
                }
                LazyColumn(state = chState, modifier = Modifier.weight(1f)) {
                    itemsIndexed(list, key = { _, c -> c.id }) { i, c ->
                        LaunchedEffect(c.id) { delay(120); vm.requestEpg(c, 6) }
                        val playing = c.id == cur?.id
                        Focus(
                            Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            requester = if (i == (if (curIdx >= 0) curIdx else 0)) chReq else null,
                            onClick = {
                                if (!playing) pb.play(liveItem(c), list.map { liveItem(it) })
                                onClose()
                            },
                            onLongClick = { vm.store.toggleFav(c.id) },
                            onKey = { ev ->
                                if (ev.type != KeyEventType.KeyDown) false
                                else if (ev.key == Key.DirectionLeft) { toCats(); true }
                                else if (ev.key == Key.DirectionRight) { onClose(); true }
                                else false
                            }
                        ) { f ->
                            Row(
                                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(8.dp)).background(if (f) C.focusBg else Color.Transparent)
                                    .then(if (f) Modifier.border(1.5.dp, C.accent, RoundedCornerShape(8.dp)) else Modifier)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Txt(c.num.toString(), F.s(10.sp, if (playing || f) C.cyan else C.dim, FontWeight.SemiBold), Modifier.width(36.dp))
                                ChannelLogo(c, Modifier.size(42.dp, 30.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Txt(c.name, F.s(11.5.sp, C.white, FontWeight.SemiBold), Modifier.weight(1f, fill = false))
                                        if (playing) { Spacer(Modifier.width(6.dp)); Txt("▶ assistindo", F.s(7.5.sp, C.cyan, FontWeight.Bold)) }
                                        if (vm.store.isFav(c.id)) { Spacer(Modifier.width(5.dp)); Txt("★", F.s(9.sp, C.star)) }
                                    }
                                    val now = vm.nowOn(c)
                                    if (now != null) {
                                        Txt("${hhmm(now.start)} · ${now.title}", F.s(9.sp, C.dim))
                                        Spacer(Modifier.height(3.dp))
                                        ProgressBar(now.progress(), Modifier.fillMaxWidth(0.9f).height(2.dp).clip(RoundedCornerShape(1.dp)))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Txt("OK assistir   ◀ categorias   ▶ ou VOLTAR fechar   segurar OK favoritar", F.s(8.sp, C.faint), Modifier.padding(start = 10.dp))
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
        modifier.padding(24.dp).width(250.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xF20C0A16)).border(1.dp, C.panelBorder, RoundedCornerShape(10.dp))
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

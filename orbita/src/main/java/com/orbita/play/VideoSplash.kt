package com.orbita.play

import android.net.Uri
import androidx.activity.compose.BackHandler
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/** Vídeo de abertura (res/raw/splash.mp4): sempre toca até o fim antes de abrir o app (só segue antes se der erro). */
@Composable
fun VideoSplash(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://${ctx.packageName}/${R.raw.splash}")))
            prepare(); playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(s: Int) { if (s == Player.STATE_ENDED) onDone() }
            override fun onPlayerError(error: PlaybackException) { onDone() }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    // garantia: se o vídeo travar, o app abre depois de 15 s
    LaunchedEffect(Unit) { delay(15_000); onDone() }
    BackHandler { } // o vídeo não é pulado pelo Voltar
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(Modifier.fillMaxSize().background(C.bg).focusRequester(focus).focusable().onKeyEvent { true }) {
        AndroidView(
            factory = { c ->
                PlayerView(c).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(0xFF050409.toInt())
                    setBackgroundColor(0xFF050409.toInt())
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

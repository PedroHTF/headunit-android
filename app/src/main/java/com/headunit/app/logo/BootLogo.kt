package com.headunit.app.logo

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.headunit.app.R
import com.headunit.app.settings.LogoKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private val BACKGROUND = Color.Black

/**
 * Tela de logo da inicialização. Toque para pular.
 * @param onFinished chamado quando o tempo acaba (ou o vídeo termina).
 */
@Composable
fun BootSplash(
    kind: LogoKind,
    file: File,
    version: Int,
    seconds: Int,
    onFinished: () -> Unit,
) {
    val finish by rememberUpdatedState(onFinished)
    Box(
        Modifier.fillMaxSize().background(BACKGROUND)
            .clickable(remember { MutableInteractionSource() }, indication = null) { finish() },
    ) {
        when (kind) {
            LogoKind.DEFAULT -> DefaultLogo(Modifier.fillMaxSize())
            LogoKind.IMAGE -> ImageLogo(file, version)
            LogoKind.VIDEO -> VideoLogo(file, onEnded = { finish() })
        }
    }
    LaunchedEffect(kind, version) {
        delay(if (kind == LogoKind.VIDEO) MAX_VIDEO_MS else seconds * 1000L)
        finish()
    }
}

private const val MAX_VIDEO_MS = 15_000L

/**
 * Logo padrão: imagem "HR-V" cromada com reflexo em terracota e asterisco
 * (gerada por tools/make_logo.py). Entra com um fade suave.
 */
@Composable
fun DefaultLogo(modifier: Modifier = Modifier, animate: Boolean = true) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    val p = progress.value
    Box(modifier.background(BACKGROUND), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.boot_logo),
            contentDescription = "Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
                .graphicsLayer { alpha = p; scaleX = 1.04f - 0.04f * p; scaleY = 1.04f - 0.04f * p },
        )
    }
}

@Composable
private fun ImageLogo(file: File, version: Int) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file, version) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull()
        }
    }
    bitmap?.let { Image(it, contentDescription = "Logo", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
}

@Composable
private fun VideoLogo(file: File, onEnded: () -> Unit) {
    val context = LocalContext.current
    val ended by rememberUpdatedState(onEnded)
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f // logo em vídeo começa sem som
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) ended()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    AndroidView(
        factory = { PlayerView(it).apply { useController = false; this.player = player } },
        modifier = Modifier.fillMaxSize(),
    )
}

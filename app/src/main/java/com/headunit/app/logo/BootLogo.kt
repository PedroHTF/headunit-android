package com.headunit.app.logo

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.headunit.app.settings.LogoKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BACKGROUND = Color(0xFF0A0A0A)
private val TERRACOTTA = Color(0xFFD97757)
private val IVORY = Color(0xFFF4EDE4)

/**
 * Tela de logo da inicialização. Toque para pular.
 * @param onFinished chamado quando o tempo acaba (ou o vídeo termina).
 */
@Composable
fun BootSplash(
    kind: LogoKind,
    file: File,
    version: Int,
    text: String,
    seconds: Int,
    onFinished: () -> Unit,
) {
    val finish by rememberUpdatedState(onFinished)
    Box(
        Modifier.fillMaxSize().background(BACKGROUND)
            .clickable(remember { MutableInteractionSource() }, indication = null) { finish() },
    ) {
        when (kind) {
            LogoKind.DEFAULT -> DefaultLogo(text, Modifier.fillMaxSize())
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
 * Logo padrão: um símbolo de raios em terracota (desenho original, inspirado no estilo
 * "faísca" de traço orgânico) e o texto abaixo. Os raios crescem um a um ao abrir.
 */
@Composable
fun DefaultLogo(text: String, modifier: Modifier = Modifier, animate: Boolean = true) {
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
    val p = progress.value

    BoxWithConstraints(modifier.background(BACKGROUND)) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height * 0.40f)
            val r = size.height * 0.20f
            rotate(degrees = (1f - p) * -40f, pivot = c) {
                RAYS.forEachIndexed { i, (angle, length) ->
                    val local = (p * 1.6f - i * 0.05f).coerceIn(0f, 1f)
                    if (local > 0f) drawRay(c, r, angle, length * local + 0.16f * (1 - local))
                }
                drawCircle(TERRACOTTA, radius = r * 0.2f * p.coerceAtLeast(0.15f), center = c)
            }
        }
        Text(
            text,
            color = IVORY,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = with(LocalDensity.current) { (heightPx * 0.11f).toSp() },
            letterSpacing = 0.18.em,
            modifier = Modifier.align(Alignment.TopCenter)
                .offset(y = maxHeight * 0.66f)
                .alpha(((p - 0.55f) / 0.45f).coerceIn(0f, 1f)),
        )
    }
}

/** (ângulo em radianos, comprimento relativo): 12 raios com leve irregularidade. */
private val RAYS: List<Pair<Float, Float>> = (0 until 12).map { i ->
    val jitter = when (i % 3) { 1 -> 0.06f; 2 -> -0.04f; else -> 0f }
    val length = when {
        i == 3 || i == 9 -> 0.86f
        i % 2 == 0 -> 1f
        else -> 0.68f
    }
    (i * 2 * PI / 12).toFloat() + jitter to length
}

/** Pétala afunilada: fina no centro, arredondada e mais larga na ponta. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRay(c: Offset, r: Float, angle: Float, length: Float) {
    val inner = r * 0.16f
    val outer = r * length
    val w = r * 0.085f * (0.8f + 0.4f * length)
    val ca = cos(angle)
    val sa = sin(angle)
    val steps = 16
    val left = ArrayList<Offset>(steps + 1)
    val right = ArrayList<Offset>(steps + 1)
    for (k in 0..steps) {
        val t = k / steps.toFloat()
        val rr = inner + (outer - inner) * t
        val half = w * (0.15f + 0.85f * sin(t * PI.toFloat() / 2))
        left += Offset(c.x + rr * ca - half * sa, c.y + rr * sa + half * ca)
        right += Offset(c.x + rr * ca + half * sa, c.y + rr * sa - half * ca)
    }
    val path = Path().apply {
        moveTo(left[0].x, left[0].y)
        left.drop(1).forEach { lineTo(it.x, it.y) }
        right.asReversed().forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, TERRACOTTA)
    drawCircle(TERRACOTTA, radius = w, center = Offset(c.x + outer * ca, c.y + outer * sa))
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

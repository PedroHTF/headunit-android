package com.headunit.app.ui.camera

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import com.headunit.app.settings.CameraConfig
import com.headunit.core.camera.GuideColor
import com.headunit.core.camera.Ground
import com.headunit.core.camera.Guidelines
import com.headunit.core.camera.SyntheticScene
import com.headunit.core.camera.applyMap
import com.headunit.core.camera.buildUndistortMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Resolução da imagem da câmera (16:9). A real terá a resolução da câmera. */
private const val IMG_W = 480
private const val IMG_H = 270

/**
 * Imagem da câmera de ré + linhas-guia. A imagem é simulada (cena gerada pelo modelo da
 * câmera); com câmera real, o mesmo mapa de correção do fisheye é aplicado aos quadros.
 */
@Composable
fun ReverseCameraView(config: CameraConfig, steeringWheelDeg: Float, modifier: Modifier = Modifier) {
    // Só a parte que muda a imagem refaz o bitmap (as linhas são baratas).
    val imageKey = listOf(config.pose, config.lens, config.removeFisheye, config.correctedFovDeg, config.scene)
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imageKey) {
        delay(60) // agrupa mudanças rápidas (arrastar um ajuste)
        image = withContext(Dispatchers.Default) { renderImage(config) }
    }
    val lines = remember(config, steeringWheelDeg) {
        Guidelines.build(config.car, config.guides, steeringWheelDeg.toDouble())
    }
    val projector = remember(config) { config.displayProjector(IMG_W, IMG_H) }
    val labelPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
        }
    }

    Box(modifier.background(Color.Black)) {
        image?.let { Image(it, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize()) }
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / IMG_W
            val sy = size.height / IMG_H
            labelPaint.textSize = size.height / 28
            lines.forEach { line ->
                val px = densify(line.points).mapNotNull { projector.project(it) }.map { Offset(it.u.toFloat() * sx, it.v.toFloat() * sy) }
                px.zipWithNext().forEach { (a, b) ->
                    drawLine(colorOf(line.color), a, b, strokeWidth = size.height / 110, cap = StrokeCap.Round)
                }
                val label = line.label
                val anchor = px.firstOrNull()
                if (label != null && anchor != null) {
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(label, anchor.x - labelPaint.measureText(label) - 8f, anchor.y + labelPaint.textSize / 3, labelPaint)
                    }
                }
            }
        }
    }
}

private fun renderImage(config: CameraConfig): ImageBitmap {
    val raw = config.rawProjector(IMG_W, IMG_H)
    var pixels = SyntheticScene.render(raw, config.scene)
    if (config.removeFisheye) {
        pixels = applyMap(buildUndistortMap(raw, config.correctedProjector(IMG_W, IMG_H)), pixels)
    }
    return Bitmap.createBitmap(pixels, IMG_W, IMG_H, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Subdivide os segmentos para que as linhas acompanhem a curvatura do fisheye. */
private fun densify(points: List<Ground>, parts: Int = 12): List<Ground> {
    if (points.size < 2) return points
    val out = mutableListOf(points.first())
    points.zipWithNext().forEach { (a, b) ->
        for (k in 1..parts) out += Ground(a.x + (b.x - a.x) * k / parts, a.y + (b.y - a.y) * k / parts)
    }
    return out
}

private fun colorOf(c: GuideColor) = when (c) {
    GuideColor.RED -> Color(0xFFF44336)
    GuideColor.YELLOW -> Color(0xFFFFC107)
    GuideColor.GREEN -> Color(0xFF4CAF50)
    GuideColor.BLUE -> Color(0xFF29B6F6)
    GuideColor.WHITE -> Color.White
}

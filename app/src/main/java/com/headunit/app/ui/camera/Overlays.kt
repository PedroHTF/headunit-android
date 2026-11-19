package com.headunit.app.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Bloqueia toques nas telas de baixo. */
private fun Modifier.consumeTouches() = this.then(
    Modifier.clickable(interactionSource = MutableInteractionSource(), indication = null) {},
)

/**
 * Câmera de ré simulada: fundo + linhas-guia de estacionamento.
 * Com câmera real, o fundo vira a imagem da câmera (CameraX ou entrada AV da central).
 */
@Composable
fun ReverseCameraOverlay() {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3A4048), Color(0xFF1C1F24), Color(0xFF2B2A26))))
            .consumeTouches(),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Linhas-guia em perspectiva: verde (longe), amarelo, vermelho (perto).
            val bands = listOf(
                Triple(0.45f, 0.62f, Color(0xFF4CAF50)),
                Triple(0.62f, 0.80f, Color(0xFFFFC107)),
                Triple(0.80f, 0.98f, Color(0xFFF44336)),
            )
            fun xAt(y: Float, left: Boolean): Float {
                val t = (y - 0.45f) / (0.98f - 0.45f)
                val half = 0.18f + t * 0.17f
                return w * (0.5f + if (left) -half else half)
            }
            bands.forEach { (top, bottom, color) ->
                listOf(true, false).forEach { left ->
                    drawLine(color, Offset(xAt(top, left), h * top), Offset(xAt(bottom, left), h * bottom), strokeWidth = 8f)
                }
                drawLine(color, Offset(xAt(top, true), h * top), Offset(xAt(top, true) + w * 0.06f, h * top), strokeWidth = 8f)
                drawLine(color, Offset(xAt(top, false), h * top), Offset(xAt(top, false) - w * 0.06f, h * top), strokeWidth = 8f)
            }
        }
        Text(
            "CÂMERA DE RÉ (simulada)", color = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
        )
        Text(
            "Verifique o entorno antes de manobrar", color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
        )
    }
}

/** Tela preta quando o ACC está desligado, como a central real. */
@Composable
fun PowerOffOverlay(onSimulateIgnitionOn: (() -> Unit)?) {
    Box(Modifier.fillMaxSize().background(Color.Black).consumeTouches(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ACC desligado", color = Color.Gray)
            if (onSimulateIgnitionOn != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onSimulateIgnitionOn) { Text("Ligar ignição (simulador)") }
            }
        }
    }
}

package com.headunit.app.ui.camera

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.headunit.app.settings.CameraConfig

/** Bloqueia toques nas telas de baixo. */
private fun Modifier.consumeTouches() = this.then(
    Modifier.clickable(interactionSource = MutableInteractionSource(), indication = null) {},
)

/** Câmera de ré em tela cheia (abre sozinha com a ré engatada). */
@Composable
fun ReverseCameraOverlay(config: CameraConfig, steeringWheelDeg: Float) {
    Box(Modifier.fillMaxSize().background(Color.Black).consumeTouches()) {
        ReverseCameraView(config, steeringWheelDeg, Modifier.fillMaxSize())
        Text(
            "CÂMERA DE RÉ (simulada)", color = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
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

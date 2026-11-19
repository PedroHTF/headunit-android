package com.headunit.app.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headunit.app.AppContainer
import com.headunit.app.settings.CameraConfig
import com.headunit.core.camera.Scene
import java.util.Locale

/**
 * Calibração da câmera de ré com prévia ao vivo: medidas do carro, posição da câmera,
 * fisheye e linhas. Dica: use a cena "Grade 1 m" e ajuste até as marcas caírem nas linhas.
 */
@Composable
fun CameraSetupScreen(container: AppContainer, onClose: () -> Unit) {
    val config by container.camera.config.collectAsStateWithLifecycle()
    var previewSteering by remember { mutableFloatStateOf(0f) }
    fun update(t: (CameraConfig) -> CameraConfig) = container.camera.update(t)

    Row(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- Prévia ----
        Column(Modifier.weight(1.25f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReverseCameraView(
                config, previewSteering,
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
            )
            Text("Volante (prévia): %.0f°".format(previewSteering), style = MaterialTheme.typography.labelLarge)
            Slider(value = previewSteering, onValueChange = { previewSteering = it }, valueRange = -540f..540f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Cena:", style = MaterialTheme.typography.labelLarge)
                FilterChip(selected = config.scene == Scene.PARKING, onClick = { update { it.copy(scene = Scene.PARKING) } }, label = { Text("Estacionamento") })
                FilterChip(selected = config.scene == Scene.GRID, onClick = { update { it.copy(scene = Scene.GRID) } }, label = { Text("Grade 1 m") })
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = container.camera::reset) { Text("Restaurar padrão") }
                Button(onClick = onClose) { Text("Concluir") }
            }
        }

        // ---- Ajustes ----
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .verticalScroll(rememberScrollState()).padding(12.dp),
        ) {
            Header("Imagem")
            SwitchLine("Remover fisheye", config.removeFisheye) { v -> update { it.copy(removeFisheye = v) } }
            Adjust("Intensidade do fisheye da lente", config.lens.fisheye, 0.0..0.95, "%.2f") { v -> update { it.copy(lens = it.lens.copy(fisheye = v)) } }
            Adjust("Campo de visão da lente", config.lens.horizontalFovDeg, 90.0..190.0, "%.0f°") { v -> update { it.copy(lens = it.lens.copy(horizontalFovDeg = v)) } }
            if (config.removeFisheye) {
                Adjust("Campo de visão após correção", config.correctedFovDeg, 60.0..150.0, "%.0f°") { v -> update { it.copy(correctedFovDeg = v) } }
            }

            Header("Posição da câmera")
            Adjust("Altura do chão", config.pose.heightM, 0.3..1.6, "%.2f m") { v -> update { it.copy(pose = it.pose.copy(heightM = v)) } }
            Adjust("Inclinação para baixo", config.pose.pitchDeg, 0.0..75.0, "%.0f°") { v -> update { it.copy(pose = it.pose.copy(pitchDeg = v)) } }
            Adjust("Rotação lateral", config.pose.yawDeg, -20.0..20.0, "%.1f°") { v -> update { it.copy(pose = it.pose.copy(yawDeg = v)) } }
            Adjust("Giro da imagem", config.pose.rollDeg, -15.0..15.0, "%.1f°") { v -> update { it.copy(pose = it.pose.copy(rollDeg = v)) } }
            Adjust("Deslocamento do centro", config.pose.offsetXM, -0.6..0.6, "%.2f m") { v -> update { it.copy(pose = it.pose.copy(offsetXM = v)) } }

            Header("Medidas do carro")
            Adjust("Largura", config.car.widthM, 1.4..2.3, "%.2f m") { v -> update { it.copy(car = it.car.copy(widthM = v)) } }
            Adjust("Comprimento (marca \"carros\")", config.car.lengthM, 3.0..6.5, "%.2f m") { v -> update { it.copy(car = it.car.copy(lengthM = v)) } }
            Adjust("Entre-eixos", config.car.wheelbaseM, 2.0..3.6, "%.2f m") { v -> update { it.copy(car = it.car.copy(wheelbaseM = v)) } }
            Adjust("Eixo traseiro → para-choque", config.car.rearOverhangM, 0.4..1.6, "%.2f m") { v -> update { it.copy(car = it.car.copy(rearOverhangM = v)) } }
            Adjust("Relação volante/rodas", config.car.steeringRatio, 10.0..22.0, "%.1f : 1") { v -> update { it.copy(car = it.car.copy(steeringRatio = v)) } }
            Adjust("Ângulo máximo das rodas", config.car.maxWheelAngleDeg, 25.0..45.0, "%.0f°") { v -> update { it.copy(car = it.car.copy(maxWheelAngleDeg = v)) } }

            Header("Linhas")
            SwitchLine("Linhas fixas (largura e distâncias)", config.guides.showStatic) { v -> update { it.copy(guides = it.guides.copy(showStatic = v)) } }
            SwitchLine("Trajetória pelo volante", config.guides.showDynamic) { v -> update { it.copy(guides = it.guides.copy(showDynamic = v)) } }
            SwitchLine("Inverter lado da trajetória", config.guides.invertSteering) { v -> update { it.copy(guides = it.guides.copy(invertSteering = v)) } }
            Adjust("Comprimento da trajetória", config.guides.dynamicLengthM, 1.0..8.0, "%.1f m") { v -> update { it.copy(guides = it.guides.copy(dynamicLengthM = v)) } }
            Adjust("Ajuste lateral das linhas", config.guides.lineOffsetXM, -0.4..0.4, "%.2f m") { v -> update { it.copy(guides = it.guides.copy(lineOffsetXM = v)) } }
            Adjust("Ajuste de distância das linhas", config.guides.lineOffsetYM, -0.6..0.6, "%.2f m") { v -> update { it.copy(guides = it.guides.copy(lineOffsetYM = v)) } }

            Text("Marcas de distância", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            ChipToggles(METER_OPTIONS, config.guides.distanceMarkersM, { "${fmt(it)} m" }) { list -> update { it.copy(guides = it.guides.copy(distanceMarkersM = list)) } }
            Text("Marcas em carros (usa o comprimento do carro)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            ChipToggles(listOf(1, 2, 3), config.guides.carLengthMarkers, { if (it == 1) "1 carro" else "$it carros" }) { list ->
                update { it.copy(guides = it.guides.copy(carLengthMarkers = list)) }
            }
        }
    }
}

private val METER_OPTIONS = listOf(0.3, 0.5, 1.0, 1.5, 2.0, 3.0)

private fun fmt(v: Double) = if (v == v.toInt().toDouble()) v.toInt().toString() else String.format(Locale("pt", "BR"), "%.1f", v)

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

@Composable
private fun Adjust(label: String, value: Double, range: ClosedFloatingPointRange<Double>, format: String, onChange: (Double) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(String.format(Locale("pt", "BR"), format, value), style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
            modifier = Modifier.height(32.dp),
        )
    }
}

@Composable
private fun SwitchLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Comparable<T>> ChipToggles(options: List<T>, selected: List<T>, label: (T) -> String, onChange: (List<T>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { opt ->
            val on = opt in selected
            FilterChip(
                selected = on,
                onClick = { onChange(if (on) selected - opt else (selected + opt).sorted()) },
                label = { Text(label(opt)) },
            )
        }
    }
}

package com.headunit.app.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headunit.app.settings.EqualizerRepository
import com.headunit.core.hal.audio.EqualizerSettings
import java.util.Locale

/** Equalizador de 10 bandas, balanço/fader e loudness. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(repo: EqualizerRepository, onClose: () -> Unit) {
    val eq by repo.settings.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Equalizador", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Text("Predefinição: ${eq.preset}", style = MaterialTheme.typography.labelLarge)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EqualizerSettings.PRESETS.keys.forEach { name ->
                FilterChip(selected = eq.preset == name, onClick = { repo.update { it.withPreset(name) } }, label = { Text(name) })
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier.weight(3f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                EqualizerSettings.FREQUENCIES.forEachIndexed { i, hz ->
                    Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        val g = eq.gains[i]
                        Text((if (g > 0) "+" else "") + String.format(Locale("pt", "BR"), "%.1f", g), style = MaterialTheme.typography.labelSmall)
                        Box(Modifier.weight(1f).width(40.dp), contentAlignment = Alignment.Center) {
                            VerticalSlider(g, { v -> repo.update { it.withGain(i, v) } })
                        }
                        Text(if (hz >= 1000) "${hz / 1000}k" else "$hz", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Column(
                Modifier.weight(2f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
            ) {
                Text("Balanço: " + side(eq.balance, "esquerda", "direita"), style = MaterialTheme.typography.labelLarge)
                Slider(value = eq.balance, onValueChange = { v -> repo.update { it.copy(balance = v) } }, valueRange = -1f..1f)
                Text("Fader: " + side(eq.fader, "trás", "frente"), style = MaterialTheme.typography.labelLarge)
                Slider(value = eq.fader, onValueChange = { v -> repo.update { it.copy(fader = v) } }, valueRange = -1f..1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Loudness", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Switch(checked = eq.loudness, onCheckedChange = { v -> repo.update { it.copy(loudness = v) } })
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "As bandas e o loudness valem para músicas e vídeos do app. Balanço e fader " +
                        "serão aplicados pelo DSP da central.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = repo::reset) { Text("Restaurar") }
            Button(onClick = onClose) { Text("Concluir") }
        }
    }
}

private fun side(v: Float, neg: String, pos: String) = when {
    v < -0.02f -> neg
    v > 0.02f -> pos
    else -> "centro"
}

/** Slider girado 90° (o Material 3 não tem slider vertical). */
@Composable
private fun VerticalSlider(value: Float, onChange: (Float) -> Unit) {
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = -EqualizerSettings.MAX_GAIN_DB..EqualizerSettings.MAX_GAIN_DB,
        modifier = Modifier
            .graphicsLayer { rotationZ = -90f }
            .layout { measurable, constraints ->
                val p = measurable.measure(Constraints(maxWidth = constraints.maxHeight, maxHeight = constraints.maxWidth))
                layout(p.height, p.width) { p.place(-(p.width - p.height) / 2, (p.width - p.height) / 2) }
            }
            .fillMaxWidth(),
    )
}

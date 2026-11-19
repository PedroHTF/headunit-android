package com.headunit.feature.radio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headunit.core.hal.radio.RadioBand
import com.headunit.core.hal.radio.RadioHal
import com.headunit.core.hal.radio.RadioState

/**
 * @param onPowerOn avisa o app para pausar outras fontes (ex.: mídia USB).
 */
@Composable
fun RadioScreen(radio: RadioHal, onPowerOn: () -> Unit, modifier: Modifier = Modifier) {
    val state by radio.state.collectAsStateWithLifecycle()

    Row(modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RadioBand.entries.forEach { band ->
                    FilterChip(selected = state.band == band, onClick = { radio.setBand(band) }, label = { Text(band.label) })
                }
                Spacer(Modifier.weight(1f))
                if (radio.isSimulated) {
                    Text("simulado, sem áudio", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(8.dp))
                }
                FilledIconButton(
                    onClick = {
                        if (state.isOn) {
                            radio.powerOff()
                        } else {
                            onPowerOn()
                            radio.powerOn()
                        }
                    },
                    colors = if (state.isOn) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.filledTonalIconButtonColors(),
                ) { Icon(Icons.Filled.PowerSettingsNew, contentDescription = "Liga/desliga rádio") }
            }
            FrequencyDisplay(state, Modifier.weight(1f).fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ControlButton(Icons.Filled.FastRewind, "Buscar anterior", radio::seekDown)
                ControlButton(Icons.Filled.ChevronLeft, "Sintonia -", radio::stepDown)
                ControlButton(Icons.Filled.ChevronRight, "Sintonia +", radio::stepUp)
                ControlButton(Icons.Filled.FastForward, "Buscar próxima", radio::seekUp)
            }
            Text("Memórias (toque: sintonizar · segure: salvar)", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.presets.forEachIndexed { index, freq ->
                    PresetButton(
                        label = freq?.let { state.band.format(it) } ?: "—",
                        selected = freq == state.frequencyKhz,
                        onClick = { radio.recallPreset(index) },
                        onLongClick = { radio.savePreset(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Estações", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = radio::scan) { Icon(Icons.Filled.Search, contentDescription = "Procurar estações") }
            }
            if (state.foundStations.isEmpty()) {
                Text("Toque na lupa para procurar.", style = MaterialTheme.typography.bodySmall)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.foundStations, key = { it.frequencyKhz }) { station ->
                    FilledTonalButton(onClick = { radio.tune(station.frequencyKhz) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${station.band.format(station.frequencyKhz)}  ${station.name}", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencyDisplay(state: RadioState, modifier: Modifier) {
    val dim = if (state.isOn) 1f else 0.4f
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    state.frequencyLabel, fontSize = 72.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = dim),
                )
                Spacer(Modifier.width(8.dp))
                Text(state.band.unit, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 14.dp))
            }
            Text(
                when {
                    state.isSeeking -> "Buscando…"
                    !state.isOn -> "Rádio desligado"
                    else -> state.stationName ?: "Sem estação"
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            state.rdsText?.takeIf { state.isOn && !state.isSeeking }?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(8.dp))
            SignalBars(state.signal)
        }
    }
}

@Composable
private fun SignalBars(signal: Int) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        (1..5).forEach { bar ->
            val active = signal >= bar * 18
            Box(
                Modifier.width(8.dp).height((6 * bar).dp).clip(RoundedCornerShape(2.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    FilledIconButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(40.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetButton(label: String, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier.height(56.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = fg, style = MaterialTheme.typography.titleMedium) }
}

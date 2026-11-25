package com.headunit.app.ui.vehicle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headunit.app.AppContainer
import com.headunit.core.hal.DrivingRules
import com.headunit.core.hal.vehicle.Gear
import com.headunit.core.hal.vehicle.Ignition
import com.headunit.core.hal.vehicle.SimulatedVehicleHal
import com.headunit.core.hal.vehicle.SteeringKey

/** Estado do carro + painel do simulador (quando a HAL simulada está ativa). */
@Composable
fun VehicleScreen(container: AppContainer) {
    val state by container.vehicleState.collectAsStateWithLifecycle()
    val hal by container.vehicleHal.collectAsStateWithLifecycle()
    var forceSim by remember { mutableStateOf(container.forceSimulation) }

    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.weight(1f).fillMaxHeight()) {
            Text("Estado do veículo", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Info("Fonte dos dados", hal.name)
            Info("Ignição", state.ignition.label)
            Info("Marcha", state.gear.label)
            Info("Velocidade", "%.0f km/h".format(state.speedKmh))
            Info("Volante", "%.0f°".format(state.steeringWheelDeg))
            Info("Vídeo permitido", if (DrivingRules.isVideoAllowed(state)) "Sim" else "Não (em movimento)")
            if (container.isAutomotive) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Forçar simulador", modifier = Modifier.weight(1f))
                    Switch(checked = forceSim, onCheckedChange = {
                        forceSim = it
                        container.forceSimulation = it
                    })
                }
                Text(
                    "Desligado: lê o Vehicle HAL do Android Automotive " +
                        "(altere em Extended controls > Car data do emulador).",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Card(Modifier.weight(1.4f).fillMaxHeight()) {
            if (hal.isSimulated) {
                PhoneSimulator(container)
                Spacer(Modifier.height(8.dp))
                SimulatorPanel(container.simulatedVehicle, state.ignition, state.gear, state.speedKmh, state.steeringWheelDeg)
            } else {
                Text("Simulador desativado", style = MaterialTheme.typography.titleLarge)
                Text("Os dados vêm do Android Automotive. Ative \"Forçar simulador\" para controlar por aqui.")
            }
        }
    }
}

@Composable
private fun PhoneSimulator(container: AppContainer) {
    val phone by container.phone.state.collectAsStateWithLifecycle()
    Text("Telefone (Bluetooth)", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = phone.connected, onCheckedChange = container.phone::setConnected)
        FilledTonalButton(
            onClick = { container.phone.simulateIncoming("+55 11 98765-4321") },
            enabled = phone.connected && !phone.inCall,
        ) { Text("Receber ligação") }
        FilledTonalButton(onClick = container.phone::simulateRemoteHangup, enabled = phone.inCall) { Text("Outro lado desliga") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimulatorPanel(sim: SimulatedVehicleHal, ignition: Ignition, gear: Gear, speed: Float, steering: Float) {
    Text("Simulador", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))

    Text("Ignição / ACC", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Ignition.OFF, Ignition.ACC, Ignition.ON).forEach {
            FilterChip(selected = ignition == it, onClick = { sim.setIgnition(it) }, label = { Text(it.label) })
        }
    }
    Spacer(Modifier.height(8.dp))

    Text("Marcha (R abre a câmera de ré)", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Gear.PARK, Gear.REVERSE, Gear.NEUTRAL, Gear.DRIVE).forEach {
            FilterChip(selected = gear == it, onClick = { sim.setGear(it) }, label = { Text("  ${it.label}  ") })
        }
    }
    Spacer(Modifier.height(8.dp))

    Text("Velocidade: %.0f km/h".format(speed), style = MaterialTheme.typography.labelLarge)
    Slider(value = speed, onValueChange = sim::setSpeed, valueRange = 0f..SimulatedVehicleHal.MAX_SPEED_KMH)

    Text("Volante: %.0f° (trajetória da câmera de ré)".format(steering), style = MaterialTheme.typography.labelLarge)
    Slider(
        value = steering, onValueChange = sim::setSteering,
        valueRange = -SimulatedVehicleHal.MAX_STEERING_DEG..SimulatedVehicleHal.MAX_STEERING_DEG,
    )

    Text("Botões do volante", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SteeringKey.entries.forEach { key ->
            FilledTonalButton(onClick = { sim.pressKey(key) }) { Text(key.label) }
        }
    }
}

@Composable
private fun Card(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp).verticalScroll(rememberScrollState()),
    ) { content() }
}

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

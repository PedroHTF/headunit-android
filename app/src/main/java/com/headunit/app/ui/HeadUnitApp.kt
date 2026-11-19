package com.headunit.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headunit.app.AppContainer
import com.headunit.app.logo.BootSplash
import com.headunit.app.ui.apps.AppsScreen
import com.headunit.app.ui.camera.CameraSetupScreen
import com.headunit.app.ui.common.ConnectBanner
import com.headunit.app.ui.camera.PowerOffOverlay
import com.headunit.app.ui.camera.ReverseCameraOverlay
import com.headunit.app.ui.home.HomeScreen
import com.headunit.app.ui.settings.SettingsScreen
import com.headunit.app.ui.vehicle.VehicleScreen
import com.headunit.core.hal.DrivingRules
import com.headunit.core.hal.vehicle.Ignition
import com.headunit.feature.media.MediaScreen
import com.headunit.feature.radio.RadioScreen
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class Screen(val label: String, val icon: ImageVector) {
    HOME("Início", Icons.Filled.Home),
    MEDIA("Mídia", Icons.Filled.LibraryMusic),
    RADIO("Rádio", Icons.Filled.Radio),
    VEHICLE("Veículo", Icons.Filled.DirectionsCar),
    APPS("Apps", Icons.Filled.Apps),
    SETTINGS("Config.", Icons.Filled.Settings),
}

@Composable
fun HeadUnitApp(
    container: AppContainer,
    initialScreen: Screen = Screen.HOME,
    showSplashOnStart: Boolean = true,
    openCameraSetup: Boolean = false,
    initialMediaTab: Int = 0,
) {
    val vehicle by container.vehicleState.collectAsStateWithLifecycle()
    val hal by container.vehicleHal.collectAsStateWithLifecycle()
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val camera by container.camera.config.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(initialScreen) }
    var cameraSetup by rememberSaveable { mutableStateOf(openCameraSetup) }
    // Logo: ao abrir o app e, se configurado, toda vez que a ignição liga.
    var splash by rememberSaveable { mutableStateOf(showSplashOnStart) }
    var wasPowered by rememberSaveable { mutableStateOf(vehicle.ignition.isPowered) }
    LaunchedEffect(vehicle.ignition.isPowered) {
        val powered = vehicle.ignition.isPowered
        if (powered && !wasPowered && settings.logoOnIgnition) splash = true
        wasPowered = powered
    }
    val logoVersion by container.logo.version.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(Modifier.fillMaxHeight()) {
                Spacer(Modifier.height(8.dp))
                Screen.entries.forEach { s ->
                    NavigationRailItem(
                        selected = screen == s,
                        onClick = { screen = s },
                        icon = { Icon(s.icon, contentDescription = s.label) },
                        label = { Text(s.label) },
                    )
                }
            }
            Column(Modifier.fillMaxSize()) {
                StatusBar(container)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (screen) {
                        Screen.HOME -> HomeScreen(container, onNavigate = { screen = it })
                        Screen.MEDIA -> MediaScreen(
                            connection = container.player,
                            loadItems = container.mediaSource,
                            initialTab = initialMediaTab,
                            videoAllowed = DrivingRules.isVideoAllowed(vehicle, settings.videoWhileDriving),
                            onPlaybackStarted = container::onMediaStarted,
                        )
                        Screen.RADIO -> RadioScreen(container.radio, onPowerOn = container::onRadioStarted)
                        Screen.VEHICLE -> VehicleScreen(container)
                        Screen.APPS -> AppsScreen()
                        Screen.SETTINGS -> SettingsScreen(
                            container,
                            onOpenCameraSetup = { cameraSetup = true },
                            onPreviewLogo = { splash = true },
                        )
                    }
                }
            }
        }

        val suggestion by container.suggestion.collectAsStateWithLifecycle()
        suggestion?.let { s ->
            ConnectBanner(
                suggestion = s,
                onOpen = { container.openProjection(s.mode) },
                onDismiss = container::dismissSuggestion,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        if (cameraSetup) {
            CameraSetupScreen(container, onClose = { cameraSetup = false })
        } else if (DrivingRules.shouldShowReverseCamera(vehicle)) {
            ReverseCameraOverlay(camera, vehicle.steeringWheelDeg)
        }
        if (splash && vehicle.ignition.isPowered && !DrivingRules.shouldShowReverseCamera(vehicle)) {
            BootSplash(
                kind = settings.logoKind,
                file = container.logo.file,
                version = logoVersion,
                seconds = settings.logoSeconds,
                onFinished = { splash = false },
            )
        }
        if (!vehicle.ignition.isPowered) {
            PowerOffOverlay(
                onSimulateIgnitionOn = if (hal.isSimulated) {
                    { container.simulatedVehicle.setIgnition(Ignition.ON) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun StatusBar(container: AppContainer) {
    val vehicle by container.vehicleState.collectAsStateWithLifecycle()
    val hal by container.vehicleHal.collectAsStateWithLifecycle()
    val source by container.audioSource.collectAsStateWithLifecycle()
    var time by rememberSaveable { mutableStateOf(now()) }

    LaunchedEffect(Unit) {
        while (true) {
            time = now()
            delay(1_000)
        }
    }

    Row(
        Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(time, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Text("Fonte: ${source.label}", style = MaterialTheme.typography.labelLarge)
        Text("Marcha ${vehicle.gear.label}", style = MaterialTheme.typography.labelLarge)
        Text("%.0f km/h".format(vehicle.speedKmh), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(4.dp))
        Text(
            "Veículo: ${hal.name}",
            style = MaterialTheme.typography.labelMedium,
            color = if (hal.isSimulated) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        )
    }
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private fun now(): String = LocalTime.now().format(timeFormat)

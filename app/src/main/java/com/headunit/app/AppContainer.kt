package com.headunit.app

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import com.headunit.app.projection.AutoLaunchPolicy
import com.headunit.app.projection.BluetoothMonitor
import com.headunit.app.projection.BtDevice
import com.headunit.app.projection.ProjectionLauncher
import com.headunit.app.settings.CameraConfigRepository
import com.headunit.app.settings.ProjectionMode
import com.headunit.app.settings.SettingsRepository
import com.headunit.core.hal.radio.RadioHal
import com.headunit.core.hal.radio.SimulatedRadioHal
import com.headunit.core.hal.vehicle.CarApiVehicleHal
import com.headunit.core.hal.vehicle.SimulatedVehicleHal
import com.headunit.core.hal.vehicle.SteeringKey
import com.headunit.core.hal.vehicle.VehicleHal
import com.headunit.core.hal.vehicle.VehicleState
import com.headunit.feature.media.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Fonte de áudio ativa: decide para onde vão os botões do volante. */
enum class AudioSource(val label: String) { NONE("—"), MEDIA("Mídia"), RADIO("Rádio") }

/**
 * Monta e guarda as peças do app (injeção de dependência manual).
 * Aqui se decide qual HAL usar: Android Automotive ou simulada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppContainer(private val context: Context) {
    private val scope: CoroutineScope = MainScope()
    private val prefs = context.getSharedPreferences("headunit", Context.MODE_PRIVATE)
    private val audioManager = context.getSystemService(AudioManager::class.java)

    /** O aparelho é Android Automotive (ex.: emulador AAOS)? */
    val isAutomotive: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)

    val simulatedVehicle = SimulatedVehicleHal()

    private val _vehicleHal = MutableStateFlow(createVehicleHal())
    val vehicleHal: StateFlow<VehicleHal> = _vehicleHal.asStateFlow()

    val vehicleState: StateFlow<VehicleState> = _vehicleHal
        .flatMapLatest { it.state }
        .stateIn(scope, SharingStarted.Eagerly, _vehicleHal.value.state.value)

    val radio: RadioHal = SimulatedRadioHal(scope)
    val player = PlayerConnection(context)

    val settings = SettingsRepository(context)
    val camera = CameraConfigRepository(context)
    val projection = ProjectionLauncher(context, settings)
    private val autoLaunch = AutoLaunchPolicy()
    val bluetooth = BluetoothMonitor(context, ::onBluetoothConnected)

    private val _audioSource = MutableStateFlow(AudioSource.NONE)
    val audioSource: StateFlow<AudioSource> = _audioSource.asStateFlow()

    var forceSimulation: Boolean
        get() = prefs.getBoolean(KEY_FORCE_SIM, false)
        set(value) {
            prefs.edit().putBoolean(KEY_FORCE_SIM, value).apply()
            restartVehicleHal()
        }

    init {
        _vehicleHal.value.start()
        player.connect()
        bluetooth.start()

        // Botões do volante vindos da HAL (simulador ou, no futuro, o MCU).
        scope.launch { _vehicleHal.flatMapLatest { it.keys }.collect(::handleSteeringKey) }

        // ACC desligado: para tudo, como uma central de verdade.
        scope.launch {
            vehicleState.map { it.ignition.isPowered }.distinctUntilChanged().collect { powered ->
                if (!powered) {
                    player.pause()
                    radio.powerOff()
                    autoLaunch.onPowerCycle() // a próxima partida pode abrir a projeção de novo
                }
            }
        }
    }

    private fun createVehicleHal(): VehicleHal =
        if (isAutomotive && !forceSimulation) CarApiVehicleHal(context) else simulatedVehicle

    /** Recria a HAL (ex.: depois de conceder a permissão de velocidade ou trocar o modo). */
    fun restartVehicleHal() {
        _vehicleHal.value.stop()
        val hal = createVehicleHal()
        hal.start()
        _vehicleHal.value = hal
    }

    /**
     * Abre Android Auto / CarPlay. Se há um celular conectado no Bluetooth, ele passa a ser
     * "o último aparelho" desse modo, e nas próximas partidas a projeção abre sozinha.
     */
    fun openProjection(mode: ProjectionMode) {
        val device = bluetooth.connected.value.lastOrNull()
        device?.let { d -> settings.update { it.rememberDevice(d.address, d.name, mode) } }
        autoLaunch.onManualLaunch(device)
        player.pause()
        radio.powerOff()
        projection.launch(mode)
    }

    /** Nova "partida" simulada: libera a abertura automática de novo. */
    fun resetAutoLaunch() = autoLaunch.onPowerCycle()

    private fun onBluetoothConnected(device: BtDevice) {
        val mode = autoLaunch.decide(settings.settings.value, device) ?: return
        scope.launch {
            delay(AUTO_LAUNCH_DELAY_MS) // dá tempo do celular terminar de conectar
            settings.update { it.rememberDevice(device.address, device.name, mode) }
            player.pause()
            radio.powerOff()
            projection.launch(mode)
        }
    }

    fun onMediaStarted() {
        radio.powerOff()
        _audioSource.value = AudioSource.MEDIA
    }

    fun onRadioStarted() {
        player.pause()
        _audioSource.value = AudioSource.RADIO
    }

    fun handleSteeringKey(key: SteeringKey) {
        val radioActive = _audioSource.value == AudioSource.RADIO
        when (key) {
            SteeringKey.NEXT -> if (radioActive) radio.seekUp() else player.next()
            SteeringKey.PREVIOUS -> if (radioActive) radio.seekDown() else player.previous()
            SteeringKey.PLAY_PAUSE -> when {
                radioActive && radio.state.value.isOn -> radio.powerOff()
                radioActive -> radio.powerOn()
                else -> player.togglePlayPause()
            }
            SteeringKey.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
            SteeringKey.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)
            SteeringKey.MUTE -> adjustVolume(AudioManager.ADJUST_TOGGLE_MUTE)
            SteeringKey.MODE -> if (radioActive) {
                radio.powerOff()
                _audioSource.value = AudioSource.MEDIA
                player.togglePlayPause()
            } else {
                onRadioStarted()
                radio.powerOn()
            }
        }
    }

    private fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private companion object {
        const val KEY_FORCE_SIM = "force_simulation"
        const val AUTO_LAUNCH_DELAY_MS = 2_000L
    }
}

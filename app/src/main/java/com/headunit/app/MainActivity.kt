package com.headunit.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.headunit.app.ui.HeadUnitApp
import com.headunit.app.ui.theme.HeadUnitTheme
import com.headunit.core.hal.vehicle.SteeringKey

class MainActivity : ComponentActivity() {

    private val container get() = (application as HeadUnitApplication).container

    private val speedPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) container.restartVehicleHal()
    }

    private val bluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        if (container.isAutomotive &&
            ContextCompat.checkSelfPermission(this, CAR_SPEED) != PackageManager.PERMISSION_GRANTED
        ) {
            speedPermission.launch(CAR_SPEED)
        }
        // Para reconhecer o celular que conecta e abrir Android Auto / CarPlay sozinho.
        if (container.settings.settings.value.autoOpenOnBluetooth && !container.bluetooth.hasPermission() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            HeadUnitTheme(settings.themeMode, settings.highContrast) {
                HeadUnitApp(container)
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Teclas de mídia (volante no AAOS, teclado, ou `adb shell input keyevent`)
     * viram comandos do volante.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val key = KEY_MAP[keyCode] ?: return super.onKeyDown(keyCode, event)
        if (event.repeatCount == 0) container.handleSteeringKey(key)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        if (keyCode in KEY_MAP) true else super.onKeyUp(keyCode, event)

    private companion object {
        const val CAR_SPEED = "android.car.permission.CAR_SPEED"

        val KEY_MAP = mapOf(
            KeyEvent.KEYCODE_MEDIA_NEXT to SteeringKey.NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS to SteeringKey.PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to SteeringKey.PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK to SteeringKey.PLAY_PAUSE,
            KeyEvent.KEYCODE_VOLUME_UP to SteeringKey.VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN to SteeringKey.VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE to SteeringKey.MUTE,
        )
    }
}

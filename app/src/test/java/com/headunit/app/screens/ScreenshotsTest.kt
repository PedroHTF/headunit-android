package com.headunit.app.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.headunit.app.HeadUnitApplication
import com.headunit.app.logo.BootSplash
import com.headunit.app.projection.BtDevice
import com.headunit.app.settings.LogoKind
import com.headunit.app.settings.ProjectionMode
import com.headunit.app.settings.ThemeMode
import com.headunit.app.ui.HeadUnitApp
import com.headunit.app.ui.Screen
import com.headunit.app.ui.theme.HeadUnitTheme
import com.headunit.core.hal.vehicle.Gear
import com.headunit.core.hal.vehicle.Ignition
import com.headunit.feature.media.LocalMedia
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Aplicação de teste: sem conexão com o serviço de mídia. */
class ScreenshotsApp : HeadUnitApplication() {
    override fun createContainer() = com.headunit.app.AppContainer(this, connectPlayer = false)
}

/**
 * Gera capturas de todas as telas (1024×600) sem emulador, para revisar o design.
 * Só roda com -Dscreenshots=caminho:  ./gradlew :app:testDebugUnitTest --tests '*ScreenshotsTest*' -Dscreenshots=/tmp/telas
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1024dp-h600dp-land-mdpi", application = ScreenshotsApp::class)
class ScreenshotsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir: File? = System.getProperty("screenshots")?.let(::File)
    private val app get() = ApplicationProvider.getApplicationContext<HeadUnitApplication>()
    private val container get() = app.container

    private fun media() = listOf(
        LocalMedia(Uri.parse("content://m/1"), "Estrada Aberta", "Banda Exemplo", 215_000, false, "USB/SD", "usb", "Musicas/Rock"),
        LocalMedia(Uri.parse("content://m/2"), "Neon na Chuva", "Banda Exemplo", 198_000, false, "USB/SD", "usb", "Musicas/Rock/Anos 80"),
        LocalMedia(Uri.parse("content://m/3"), "Tarde de Domingo", "Artista MPB", 242_000, false, "USB/SD", "usb", "Musicas/MPB"),
        LocalMedia(Uri.parse("content://m/4"), "Beira-Mar", "Artista MPB", 187_000, false, "USB/SD", "usb", "Musicas/MPB"),
        LocalMedia(Uri.parse("content://m/5"), "Viagem Longa", "Trilha", 305_000, false, "USB/SD", "usb", "Musicas"),
        LocalMedia(Uri.parse("content://m/6"), "Show ao vivo", "Vídeo", 3_600_000, true, "USB/SD", "usb", "Videos"),
    )

    private fun shoot(
        name: String,
        waitMs: Long = 300,
        mode: ThemeMode = ThemeMode.DARK,
        highContrast: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        assumeTrue("Passe -Dscreenshots=pasta para gerar", outDir != null)
        container.mediaSource = { media() }
        // Relógio manual: a barra de status tem um relógio em loop, então a tela nunca "para".
        compose.mainClock.autoAdvance = false
        compose.setContent { HeadUnitTheme(mode, highContrast) { content() } }
        // Deixa animações e cálculos em segundo plano (câmera) terminarem.
        repeat(6) {
            compose.mainClock.advanceTimeBy(waitMs)
            Thread.sleep(250)
        }
        outDir!!.mkdirs()
        var bmp: Bitmap? = null
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bmp!!))
        }
        val image = bmp!!
        File(outDir, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun screen(name: String, s: Screen, tab: Int = 0, mode: ThemeMode = ThemeMode.DARK, hc: Boolean = false) =
        shoot(name, mode = mode, highContrast = hc) {
            HeadUnitApp(container, initialScreen = s, showSplashOnStart = false, initialMediaTab = tab)
        }

    // ---- Temas
    @Test fun t20_homeLight() = screen("20-inicio-claro.png", Screen.HOME, mode = ThemeMode.LIGHT)
    @Test fun t21_homeDarkHc() = screen("21-inicio-escuro-alto-contraste.png", Screen.HOME, hc = true)
    @Test fun t22_homeLightHc() = screen("22-inicio-claro-alto-contraste.png", Screen.HOME, mode = ThemeMode.LIGHT, hc = true)
    @Test fun t23_radioLight() {
        container.onRadioStarted(); container.radio.tune(94_700); container.radio.powerOn()
        screen("23-radio-claro.png", Screen.RADIO, mode = ThemeMode.LIGHT)
    }
    @Test fun t24_radioDarkHc() {
        container.onRadioStarted(); container.radio.tune(94_700); container.radio.powerOn()
        screen("24-radio-escuro-alto-contraste.png", Screen.RADIO, hc = true)
    }
    @Test fun t25_settingsLight() = screen("25-configuracoes-claro.png", Screen.SETTINGS, mode = ThemeMode.LIGHT)
    @Test fun t26_mediaLight() = screen("26-midia-claro.png", Screen.MEDIA, mode = ThemeMode.LIGHT)

    @Test fun t01_logo() = shoot("01-logo.png") {
        BootSplash(LogoKind.DEFAULT, container.logo.file, 0, 60, onFinished = {})
    }
    @Test fun t02_home() = screen("02-inicio.png", Screen.HOME)
    @Test fun t03_homeRadio() {
        container.onRadioStarted(); container.radio.tune(94_700); container.radio.powerOn()
        screen("03-inicio-radio.png", Screen.HOME)
    }
    @Test fun t04_mediaSongs() = screen("04-midia-musicas.png", Screen.MEDIA)
    @Test fun t05_mediaFolders() = screen("05-midia-pastas.png", Screen.MEDIA, tab = 2)
    @Test fun t06_radio() {
        container.onRadioStarted(); container.radio.tune(94_700); container.radio.powerOn()
        screen("06-radio.png", Screen.RADIO)
    }
    @Test fun t07_vehicle() = screen("07-veiculo.png", Screen.VEHICLE)
    @Test fun t08_apps() = screen("08-apps.png", Screen.APPS)
    @Test fun t09_settings() = screen("09-configuracoes.png", Screen.SETTINGS)
    @Test fun t10_reverse() {
        container.simulatedVehicle.setGear(Gear.REVERSE)
        container.simulatedVehicle.setSteering(200f)
        shoot("10-camera-re.png", waitMs = 600) { HeadUnitApp(container, showSplashOnStart = false) }
    }
    @Test fun t11_cameraSetup() = shoot("11-ajuste-camera.png", waitMs = 600) {
        HeadUnitApp(container, showSplashOnStart = false, openCameraSetup = true)
    }
    @Test fun t12_accOff() {
        container.simulatedVehicle.setIgnition(Ignition.OFF)
        screen("12-acc-desligado.png", Screen.HOME)
    }
    @Test fun t13_connectBanner() {
        container.settings.update { it.rememberDevice("AA", "Pixel", ProjectionMode.ANDROID_AUTO) }
        container.openProjection(ProjectionMode.ANDROID_AUTO) // conta como "já abriu nesta viagem"
        container.bluetooth.deviceConnected(BtDevice("AA", "Pixel"))
        screen("13-aviso-conexao.png", Screen.HOME)
    }
}

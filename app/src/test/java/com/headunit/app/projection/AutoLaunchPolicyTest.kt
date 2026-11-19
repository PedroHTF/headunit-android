package com.headunit.app.projection

import com.headunit.app.settings.ProjectionMode
import com.headunit.app.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoLaunchPolicyTest {
    private val pixel = BtDevice("AA:AA", "Pixel")
    private val iphone = BtDevice("BB:BB", "iPhone de Alguém")
    private val known = Settings()
        .rememberDevice(iphone.address, iphone.name, ProjectionMode.CARPLAY)
        .rememberDevice(pixel.address, pixel.name, ProjectionMode.ANDROID_AUTO) // último = pixel

    private var now = 0L
    private fun policy() = AutoLaunchPolicy { now }

    @Test
    fun opensLastDeviceOnFirstConnection() {
        assertEquals(ProjectionMode.ANDROID_AUTO, policy().decide(known, pixel))
    }

    @Test
    fun firstConnectionIgnoresOtherDevicesWhenOnlyLastDevice() {
        assertNull(policy().decide(known, iphone))
    }

    @Test
    fun switchingPhoneMidTripOpensItsMode() {
        val p = policy()
        p.decide(known, pixel)
        now += 5_000
        assertEquals(ProjectionMode.CARPLAY, p.decide(known, iphone))
    }

    @Test
    fun quickReconnectOfSameDeviceIsIgnored() {
        val p = policy()
        p.decide(known, pixel)
        now += 10_000
        assertNull(p.decide(known, pixel))
    }

    @Test
    fun sameDeviceReopensAfterLongDisconnection() {
        val p = policy()
        p.decide(known, pixel)
        now += AutoLaunchPolicy.QUICK_RECONNECT_MS + 1
        assertEquals(ProjectionMode.ANDROID_AUTO, p.decide(known, pixel))
    }

    @Test
    fun midTripSwitchCanBeDisabled() {
        val p = policy()
        val s = known.copy(switchPhoneMidTrip = false)
        p.decide(s, pixel)
        assertNull(p.decide(s, iphone))
        p.onPowerCycle()
        assertEquals(ProjectionMode.ANDROID_AUTO, p.decide(s, pixel))
    }

    @Test
    fun guessesCarPlayFromNameWhenAnyDeviceAllowed() {
        assertEquals(ProjectionMode.CARPLAY, policy().decide(Settings(onlyLastDevice = false), BtDevice("CC", "iPhone 15")))
    }

    @Test
    fun unknownNonAppleDeviceDoesNothing() {
        assertNull(policy().decide(Settings(), BtDevice("CC:CC", "Fone JBL")))
    }

    @Test
    fun disabledSettingDoesNothing() {
        assertNull(policy().decide(known.copy(autoOpenOnBluetooth = false), pixel))
    }

    @Test
    fun manualLaunchCountsAsThisTrip() {
        val p = policy()
        p.onManualLaunch(pixel)
        assertNull(p.decide(known, pixel)) // queda rápida do mesmo aparelho
        assertEquals(ProjectionMode.CARPLAY, p.decide(known, iphone))
    }
}

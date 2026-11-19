package com.headunit.app.projection

import com.headunit.app.settings.ProjectionMode
import com.headunit.app.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoLaunchPolicyTest {
    private val pixel = BtDevice("AA:AA", "Pixel")
    private val iphone = BtDevice("BB:BB", "iPhone de Alguém")
    private val known = Settings().rememberDevice(pixel.address, pixel.name, ProjectionMode.ANDROID_AUTO)

    @Test
    fun opensKnownDeviceOnFirstConnection() {
        val policy = AutoLaunchPolicy()
        assertEquals(ProjectionMode.ANDROID_AUTO, policy.decide(known, pixel))
    }

    @Test
    fun opensOnlyOncePerPowerCycle() {
        val policy = AutoLaunchPolicy()
        policy.decide(known, pixel)
        assertNull(policy.decide(known, pixel))
        policy.onPowerCycle()
        assertEquals(ProjectionMode.ANDROID_AUTO, policy.decide(known, pixel))
    }

    @Test
    fun ignoresOtherDevicesWhenOnlyLastDevice() {
        val policy = AutoLaunchPolicy()
        assertNull(policy.decide(known, iphone))
    }

    @Test
    fun guessesCarPlayFromNameWhenAnyDeviceAllowed() {
        val policy = AutoLaunchPolicy()
        assertEquals(ProjectionMode.CARPLAY, policy.decide(known.copy(onlyLastDevice = false), iphone))
    }

    @Test
    fun unknownNonAppleDeviceDoesNothing() {
        val policy = AutoLaunchPolicy()
        assertNull(policy.decide(Settings(), BtDevice("CC:CC", "Fone JBL")))
    }

    @Test
    fun disabledSettingDoesNothing() {
        val policy = AutoLaunchPolicy()
        assertNull(policy.decide(known.copy(autoOpenOnBluetooth = false), pixel))
    }

    @Test
    fun manualLaunchCountsAsThisCycle() {
        val policy = AutoLaunchPolicy()
        policy.onManualLaunch()
        assertNull(policy.decide(known, pixel))
    }
}

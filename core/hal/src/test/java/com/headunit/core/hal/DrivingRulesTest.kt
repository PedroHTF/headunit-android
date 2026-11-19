package com.headunit.core.hal

import com.headunit.core.hal.vehicle.Gear
import com.headunit.core.hal.vehicle.Ignition
import com.headunit.core.hal.vehicle.VehicleState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingRulesTest {
    @Test
    fun videoAllowedOnlyWhenStopped() {
        assertTrue(DrivingRules.isVideoAllowed(VehicleState(speedKmh = 0f)))
        assertFalse(DrivingRules.isVideoAllowed(VehicleState(speedKmh = 30f)))
    }

    @Test
    fun videoAllowedAtAnySpeedWhenUserEnables() {
        assertTrue(DrivingRules.isVideoAllowed(VehicleState(speedKmh = 120f), allowWhileDriving = true))
    }

    @Test
    fun reverseCameraOnlyInReverseWithPower() {
        assertTrue(DrivingRules.shouldShowReverseCamera(VehicleState(gear = Gear.REVERSE)))
        assertFalse(DrivingRules.shouldShowReverseCamera(VehicleState(gear = Gear.DRIVE)))
        assertFalse(DrivingRules.shouldShowReverseCamera(VehicleState(gear = Gear.REVERSE, ignition = Ignition.OFF)))
    }
}

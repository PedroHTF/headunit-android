package com.headunit.app.projection

import com.headunit.app.settings.ProjectionMode
import com.headunit.app.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoLaunchPolicyTest {
    private val pixel = BtDevice("AA:AA", "Pixel")
    private val iphone = BtDevice("BB:BB", "iPhone de Alguém")
    private val known = Settings()
        .rememberDevice(iphone.address, iphone.name, ProjectionMode.CARPLAY)
        .rememberDevice(pixel.address, pixel.name, ProjectionMode.ANDROID_AUTO) // último = pixel

    @Test
    fun firstConnectionOfTripOpensAutomatically() {
        assertEquals(ConnectAction.AutoOpen(ProjectionMode.ANDROID_AUTO), AutoLaunchPolicy().decide(known, pixel))
    }

    @Test
    fun reconnectionDuringTripOnlySuggests() {
        val p = AutoLaunchPolicy()
        p.decide(known, pixel)
        assertEquals(ConnectAction.Suggest(pixel, ProjectionMode.ANDROID_AUTO), p.decide(known, pixel))
    }

    @Test
    fun otherPhoneDuringTripOnlySuggests() {
        val p = AutoLaunchPolicy()
        p.decide(known, pixel)
        assertEquals(ConnectAction.Suggest(iphone, ProjectionMode.CARPLAY), p.decide(known, iphone))
    }

    @Test
    fun newTripOpensAutomaticallyAgain() {
        val p = AutoLaunchPolicy()
        p.decide(known, pixel)
        p.onPowerCycle()
        assertEquals(ConnectAction.AutoOpen(ProjectionMode.ANDROID_AUTO), p.decide(known, pixel))
    }

    @Test
    fun manualLaunchMeansLaterConnectionsOnlySuggest() {
        val p = AutoLaunchPolicy()
        p.onManualLaunch()
        assertEquals(ConnectAction.Suggest(pixel, ProjectionMode.ANDROID_AUTO), p.decide(known, pixel))
    }

    @Test
    fun otherPhoneAtStartIsOnlySuggestedWhenOnlyLastDevice() {
        assertEquals(ConnectAction.Suggest(iphone, ProjectionMode.CARPLAY), AutoLaunchPolicy().decide(known, iphone))
    }

    @Test
    fun autoOpenDisabledStillSuggests() {
        assertEquals(
            ConnectAction.Suggest(pixel, ProjectionMode.ANDROID_AUTO),
            AutoLaunchPolicy().decide(known.copy(autoOpenOnBluetooth = false), pixel),
        )
    }

    @Test
    fun guessesCarPlayFromName() {
        assertEquals(
            ConnectAction.AutoOpen(ProjectionMode.CARPLAY),
            AutoLaunchPolicy().decide(Settings(onlyLastDevice = false), BtDevice("CC", "iPhone 15")),
        )
    }

    @Test
    fun unknownNonAppleDeviceDoesNothing() {
        assertEquals(ConnectAction.Nothing, AutoLaunchPolicy().decide(Settings(), BtDevice("CC:CC", "Fone JBL")))
    }
}

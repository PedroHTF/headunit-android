package com.headunit.core.hal

import com.headunit.core.hal.audio.EqualizerSettings
import com.headunit.core.hal.phone.CallKind
import com.headunit.core.hal.phone.CallState
import com.headunit.core.hal.phone.SimulatedPhoneHal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneAndEqualizerTest {

    @Test
    fun outgoingCallGetsAnsweredAndLogged() = runTest {
        val phone = SimulatedPhoneHal(backgroundScope, answerDelayMs = 1_000)
        phone.dial("+55 11 91234-5678")
        assertEquals(CallState.DIALING, phone.state.value.callState)
        assertEquals("Ana Souza", phone.state.value.callName)
        advanceTimeBy(1_500)
        assertEquals(CallState.ACTIVE, phone.state.value.callState)
        phone.hangup()
        assertEquals(CallState.IDLE, phone.state.value.callState)
        assertEquals(CallKind.OUT, phone.state.value.recents.first().kind)
    }

    @Test
    fun rejectedIncomingIsMissed() = runTest {
        val phone = SimulatedPhoneHal(backgroundScope)
        phone.simulateIncoming("+55 11 4002-8922")
        assertEquals("Oficina", phone.state.value.callName)
        phone.reject()
        assertEquals(CallKind.MISSED, phone.state.value.recents.first().kind)
    }

    @Test
    fun disconnectedPhoneCannotDial() = runTest {
        val phone = SimulatedPhoneHal(backgroundScope)
        phone.setConnected(false)
        phone.dial("123")
        assertFalse(phone.state.value.inCall)
    }

    @Test
    fun equalizerPresetsAndLimits() {
        val eq = EqualizerSettings().withPreset("Rock")
        assertEquals(5f, eq.gains.first())
        val custom = eq.withGain(0, 20f)
        assertEquals(12f, custom.gains.first())
        assertEquals(EqualizerSettings.CUSTOM, custom.preset)
    }

    @Test
    fun equalizerInterpolatesOtherBands() {
        val eq = EqualizerSettings().withGain(5, 4f).withGain(6, 8f) // 1 kHz e 2 kHz
        assertEquals(4f, eq.gainAt(1000f))
        assert(abs(eq.gainAt(1414f) - 6f) < 0.05f)
        assertEquals(eq.gains.first(), eq.gainAt(20f))
    }
}

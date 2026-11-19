package com.headunit.core.hal

import com.headunit.core.hal.radio.RadioBand
import com.headunit.core.hal.radio.SimulatedRadioHal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedRadioHalTest {

    private fun TestScope.radio() = SimulatedRadioHal(this)

    @Test
    fun tuneAlignsToStepAndFindsStation() = runTest {
        val radio = radio()
        radio.tune(89_140)
        assertEquals(89_100, radio.state.value.frequencyKhz)
        assertEquals("ROCK SIM", radio.state.value.stationName)
        assertEquals("89.1", radio.state.value.frequencyLabel)
    }

    @Test
    fun tuneOutOfBandClampsToLimit() = runTest {
        val radio = radio()
        radio.tune(200_000)
        assertEquals(RadioBand.FM.maxKhz, radio.state.value.frequencyKhz)
        assertNull(radio.state.value.stationName)
    }

    @Test
    fun seekUpStopsAtNextStation() = runTest {
        val radio = radio()
        radio.tune(90_000)
        radio.seekUp()
        advanceUntilIdle()
        assertEquals(91_300, radio.state.value.frequencyKhz)
        assertFalse(radio.state.value.isSeeking)
    }

    @Test
    fun seekUpWrapsAroundBandEnd() = runTest {
        val radio = radio()
        radio.tune(107_000)
        radio.seekUp()
        advanceUntilIdle()
        assertEquals(89_100, radio.state.value.frequencyKhz)
    }

    @Test
    fun seekDownFindsPreviousStation() = runTest {
        val radio = radio()
        radio.tune(99_000)
        radio.seekDown()
        advanceUntilIdle()
        assertEquals(97_500, radio.state.value.frequencyKhz)
    }

    @Test
    fun presetsSaveAndRecall() = runTest {
        val radio = radio()
        radio.tune(102_100)
        radio.savePreset(5)
        radio.tune(88_000)
        radio.recallPreset(5)
        assertEquals(102_100, radio.state.value.frequencyKhz)
        assertEquals("JAZZ", radio.state.value.stationName)
    }

    @Test
    fun switchingToAmTunesFirstAmStation() = runTest {
        val radio = radio()
        radio.setBand(RadioBand.AM)
        assertEquals(RadioBand.AM, radio.state.value.band)
        assertEquals(640, radio.state.value.frequencyKhz)
    }

    @Test
    fun scanListsBandStations() = runTest {
        val radio = radio()
        radio.scan()
        advanceUntilIdle()
        assertEquals(8, radio.state.value.foundStations.size)
    }
}

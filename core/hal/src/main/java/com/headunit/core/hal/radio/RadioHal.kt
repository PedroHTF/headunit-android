package com.headunit.core.hal.radio

import kotlinx.coroutines.flow.StateFlow

/**
 * Sintonizador de rádio.
 *
 * Implementações:
 *  - [SimulatedRadioHal]: estações falsas, para desenvolvimento no PC.
 *  - futuramente: chip de rádio da central (via MCU) ou Broadcast Radio HAL do AAOS
 *    (essa é uma API de sistema: exige app instalado como app de sistema).
 */
interface RadioHal {
    val name: String
    val isSimulated: Boolean
    val state: StateFlow<RadioState>

    fun powerOn()
    fun powerOff()
    fun setBand(band: RadioBand)
    fun tune(frequencyKhz: Int)
    fun stepUp()
    fun stepDown()
    fun seekUp()
    fun seekDown()
    /** Procura todas as estações da faixa e preenche [RadioState.foundStations]. */
    fun scan()
    fun savePreset(index: Int)
    fun recallPreset(index: Int)

    companion object {
        const val PRESET_COUNT = 6
    }
}

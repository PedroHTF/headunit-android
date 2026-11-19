package com.headunit.core.hal.radio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Rádio falso: uma lista fixa de estações (nomes fictícios), com busca que leva tempo,
 * como num sintonizador de verdade. Não produz áudio.
 */
class SimulatedRadioHal(
    private val scope: CoroutineScope,
    private val stations: List<RadioStation> = DEFAULT_STATIONS,
    private val seekStepDelayMs: Long = 20,
) : RadioHal {
    override val name = "Rádio simulado"
    override val isSimulated = true

    private val _state = MutableStateFlow(
        stateFor(RadioBand.FM, RadioBand.FM.minKhz).copy(
            presets = stations.filter { it.band == RadioBand.FM }
                .take(RadioHal.PRESET_COUNT)
                .map<RadioStation, Int?> { it.frequencyKhz }
                .let { it + List(RadioHal.PRESET_COUNT - it.size) { null } },
        ),
    )
    override val state: StateFlow<RadioState> = _state.asStateFlow()

    private var seekJob: Job? = null

    override fun powerOn() = _state.update { it.copy(isOn = true) }

    override fun powerOff() {
        cancelSeek()
        _state.update { it.copy(isOn = false) }
    }

    override fun setBand(band: RadioBand) {
        if (band == _state.value.band) return
        cancelSeek()
        val first = stations.firstOrNull { it.band == band }?.frequencyKhz ?: band.minKhz
        _state.update { current ->
            stateFor(band, first).copy(isOn = current.isOn, presets = List(RadioHal.PRESET_COUNT) { null })
        }
    }

    override fun tune(frequencyKhz: Int) {
        cancelSeek()
        applyFrequency(_state.value.band.align(frequencyKhz))
    }

    override fun stepUp() = step(+1)
    override fun stepDown() = step(-1)

    private fun step(direction: Int) {
        val s = _state.value
        tune(wrap(s.band, s.frequencyKhz + direction * s.band.stepKhz))
    }

    override fun seekUp() = seek(+1)
    override fun seekDown() = seek(-1)

    private fun seek(direction: Int) {
        cancelSeek()
        val band = _state.value.band
        val start = _state.value.frequencyKhz
        seekJob = scope.launch {
            _state.update { it.copy(isSeeking = true) }
            var freq = start
            val totalSteps = (band.maxKhz - band.minKhz) / band.stepKhz + 1
            repeat(totalSteps) {
                freq = wrap(band, freq + direction * band.stepKhz)
                applyFrequency(freq)
                delay(seekStepDelayMs)
                if (stationAt(band, freq) != null) {
                    _state.update { it.copy(isSeeking = false) }
                    return@launch
                }
            }
            // Nenhuma estação: volta para onde estava.
            applyFrequency(start)
            _state.update { it.copy(isSeeking = false) }
        }
    }

    override fun scan() {
        cancelSeek()
        val band = _state.value.band
        seekJob = scope.launch {
            _state.update { it.copy(isSeeking = true) }
            delay(seekStepDelayMs * 20)
            _state.update { s ->
                s.copy(isSeeking = false, foundStations = stations.filter { it.band == band }.sortedBy { it.frequencyKhz })
            }
        }
    }

    override fun savePreset(index: Int) {
        if (index !in 0 until RadioHal.PRESET_COUNT) return
        _state.update { s -> s.copy(presets = s.presets.toMutableList().also { it[index] = s.frequencyKhz }) }
    }

    override fun recallPreset(index: Int) {
        val freq = _state.value.presets.getOrNull(index) ?: return
        tune(freq)
    }

    private fun cancelSeek() {
        seekJob?.cancel()
        seekJob = null
        if (_state.value.isSeeking) _state.update { it.copy(isSeeking = false) }
    }

    private fun applyFrequency(freq: Int) {
        _state.update { current ->
            stateFor(current.band, freq).copy(
                isOn = current.isOn,
                isSeeking = current.isSeeking,
                presets = current.presets,
                foundStations = current.foundStations,
            )
        }
    }

    private fun stateFor(band: RadioBand, freq: Int): RadioState {
        val station = stationAt(band, freq)
        return RadioState(
            band = band,
            frequencyKhz = freq,
            stationName = station?.name,
            rdsText = station?.rdsText?.takeIf { it.isNotBlank() },
            signal = station?.signal ?: noiseFloor(freq),
        )
    }

    private fun stationAt(band: RadioBand, freq: Int) =
        stations.firstOrNull { it.band == band && it.frequencyKhz == freq }

    /** Ruído pseudoaleatório (mas determinístico) entre 0 e 9. */
    private fun noiseFloor(freq: Int) = (freq * 31 % 10).let { if (it < 0) -it else it }

    private fun wrap(band: RadioBand, freq: Int): Int = when {
        freq > band.maxKhz -> band.minKhz
        freq < band.minKhz -> band.maxKhz
        else -> freq
    }

    companion object {
        val DEFAULT_STATIONS = listOf(
            RadioStation(RadioBand.FM, 89_100, "ROCK SIM", "Clássicos do rock 24h", 85),
            RadioStation(RadioBand.FM, 91_300, "NOTICIAS", "Trânsito: marginal livre", 72),
            RadioStation(RadioBand.FM, 94_700, "POP HITS", "Agora: as mais pedidas", 90),
            RadioStation(RadioBand.FM, 97_500, "MPB FM", "Música brasileira", 64),
            RadioStation(RadioBand.FM, 99_900, "SERTANEJ", "Modão sem parar", 78),
            RadioStation(RadioBand.FM, 102_100, "JAZZ", "Jazz & Blues", 55),
            RadioStation(RadioBand.FM, 104_500, "CLASSICA", "Concerto da tarde", 60),
            RadioStation(RadioBand.FM, 106_300, "ESPORTES", "Futebol ao vivo", 81),
            RadioStation(RadioBand.AM, 640, "AM NEWS", "Jornal da manhã", 50),
            RadioStation(RadioBand.AM, 1_020, "AM FUTEB", "Jogo ao vivo", 45),
        )
    }
}

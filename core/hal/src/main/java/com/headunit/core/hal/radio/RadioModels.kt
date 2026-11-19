package com.headunit.core.hal.radio

import java.util.Locale

/** Faixas de rádio. Frequências sempre em kHz. */
enum class RadioBand(val label: String, val minKhz: Int, val maxKhz: Int, val stepKhz: Int) {
    FM("FM", 87_500, 108_000, 100),
    AM("AM", 530, 1_700, 10);

    fun format(khz: Int): String = when (this) {
        FM -> String.format(Locale.ROOT, "%.1f", khz / 1000.0)
        AM -> khz.toString()
    }

    val unit: String get() = if (this == FM) "MHz" else "kHz"

    /** Encaixa a frequência na faixa e no passo da sintonia. */
    fun align(khz: Int): Int {
        val clamped = khz.coerceIn(minKhz, maxKhz)
        val steps = Math.round((clamped - minKhz).toDouble() / stepKhz).toInt()
        return minKhz + steps * stepKhz
    }
}

data class RadioStation(
    val band: RadioBand,
    val frequencyKhz: Int,
    val name: String,
    val rdsText: String = "",
    /** Intensidade do sinal, 0 a 100. */
    val signal: Int = 80,
)

data class RadioState(
    val isOn: Boolean = false,
    val band: RadioBand = RadioBand.FM,
    val frequencyKhz: Int = RadioBand.FM.minKhz,
    /** Nome da estação (RDS PS); null quando não há estação sintonizada. */
    val stationName: String? = null,
    /** Texto do RDS (RT). */
    val rdsText: String? = null,
    val signal: Int = 0,
    val isSeeking: Boolean = false,
    /** Memórias: frequências em kHz (null = vazia). */
    val presets: List<Int?> = List(RadioHal.PRESET_COUNT) { null },
    val foundStations: List<RadioStation> = emptyList(),
) {
    val frequencyLabel: String get() = band.format(frequencyKhz)
}

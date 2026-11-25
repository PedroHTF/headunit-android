package com.headunit.core.hal.audio

import kotlin.math.ln

/**
 * Equalizador de 10 bandas + balanço/fader + loudness. Mesmo modelo do headunit-linux.
 * O Android aplica as bandas pelo equalizador do sistema (que tem outras frequências,
 * normalmente 5): por isso [gainAt] interpola em qualquer frequência.
 */
data class EqualizerSettings(
    val gains: List<Float> = List(BAND_COUNT) { 0f },
    val preset: String = "Plano",
    val balance: Float = 0f,  // -1 esquerda .. +1 direita
    val fader: Float = 0f,    // -1 trás .. +1 frente
    val loudness: Boolean = false,
) {
    fun withGain(band: Int, db: Float): EqualizerSettings {
        if (band !in 0 until BAND_COUNT) return this
        val v = (Math.round(db * 2) / 2f).coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB) // passos de 0,5 dB
        return copy(gains = gains.toMutableList().also { it[band] = v }, preset = CUSTOM)
    }

    fun withPreset(name: String): EqualizerSettings =
        PRESETS[name]?.let { copy(gains = it, preset = name) } ?: this

    /** Ganho (dB) em [hz], interpolado em escala logarítmica entre as bandas. */
    fun gainAt(hz: Float): Float {
        val f = FREQUENCIES
        if (hz <= f.first()) return gains.first()
        if (hz >= f.last()) return gains.last()
        for (i in 0 until f.size - 1) {
            if (hz <= f[i + 1]) {
                val t = ((ln(hz) - ln(f[i].toFloat())) / (ln(f[i + 1].toFloat()) - ln(f[i].toFloat())))
                return gains[i] + (gains[i + 1] - gains[i]) * t
            }
        }
        return 0f
    }

    companion object {
        const val BAND_COUNT = 10
        const val MAX_GAIN_DB = 12f
        const val CUSTOM = "Personalizado"
        val FREQUENCIES = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
        val PRESETS: Map<String, List<Float>> = linkedMapOf(
            "Plano" to List(BAND_COUNT) { 0f },
            "Pop" to listOf(-1f, 1f, 3f, 4f, 3f, 0f, -1f, -1f, 1f, 2f),
            "Rock" to listOf(5f, 4f, 2f, -1f, -2f, -1f, 2f, 4f, 5f, 5f),
            "Jazz" to listOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f),
            "Clássico" to listOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f),
            "Eletrônico" to listOf(6f, 5f, 2f, 0f, -2f, 1f, 0f, 2f, 4f, 5f),
            "Voz" to listOf(-3f, -2f, -1f, 1f, 3f, 4f, 4f, 2f, 0f, -2f),
            "Graves+" to listOf(7f, 6f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
        )
    }
}

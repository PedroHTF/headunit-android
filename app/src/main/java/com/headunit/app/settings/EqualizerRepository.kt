package com.headunit.app.settings

import android.content.Context
import com.headunit.core.hal.audio.EqualizerSettings
import com.headunit.feature.media.AudioEffects
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda o equalizador e repassa para o player (AudioEffects). */
class EqualizerRepository(context: Context) {
    private val prefs = context.getSharedPreferences("equalizer", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<EqualizerSettings> = _settings.asStateFlow()

    init {
        AudioEffects.settings.value = _settings.value
    }

    fun update(transform: (EqualizerSettings) -> EqualizerSettings) {
        val s = transform(_settings.value)
        prefs.edit()
            .putString("gains", s.gains.joinToString(","))
            .putString("preset", s.preset)
            .putFloat("balance", s.balance)
            .putFloat("fader", s.fader)
            .putBoolean("loudness", s.loudness)
            .apply()
        _settings.value = s
        AudioEffects.settings.value = s
    }

    fun reset() = update { EqualizerSettings() }

    private fun load(): EqualizerSettings {
        val gains = prefs.getString("gains", null)?.split(',')?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == EqualizerSettings.BAND_COUNT } ?: List(EqualizerSettings.BAND_COUNT) { 0f }
        return EqualizerSettings(
            gains = gains,
            preset = prefs.getString("preset", null) ?: "Plano",
            balance = prefs.getFloat("balance", 0f),
            fader = prefs.getFloat("fader", 0f),
            loudness = prefs.getBoolean("loudness", false),
        )
    }
}

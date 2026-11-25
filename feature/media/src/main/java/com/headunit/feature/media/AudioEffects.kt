package com.headunit.feature.media

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import com.headunit.core.hal.audio.EqualizerSettings
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Aplica o equalizador do app ao áudio do player (efeitos do próprio Android).
 * O app atualiza [settings]; o [PlaybackService] liga os efeitos à sessão de áudio dele.
 *
 * Balanço e fader não existem nos efeitos do Android: ficam para o DSP da central.
 */
object AudioEffects {
    val settings = MutableStateFlow(EqualizerSettings())

    private var equalizer: Equalizer? = null
    private var loudness: LoudnessEnhancer? = null

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId == 0) return
        equalizer = runCatching { Equalizer(0, audioSessionId).apply { enabled = true } }
            .onFailure { Log.w(TAG, "Equalizador indisponível", it) }.getOrNull()
        loudness = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
        apply(settings.value)
    }

    fun apply(s: EqualizerSettings) {
        equalizer?.let { eq ->
            runCatching {
                val range = eq.bandLevelRange // milibéis
                for (band in 0 until eq.numberOfBands) {
                    val hz = eq.getCenterFreq(band.toShort()) / 1000f // vem em mHz
                    val level = (s.gainAt(hz) * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
                    eq.setBandLevel(band.toShort(), level.toShort())
                }
            }
        }
        loudness?.let { l ->
            runCatching {
                l.setTargetGain(if (s.loudness) LOUDNESS_GAIN_MB else 0)
                l.enabled = s.loudness
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        loudness = null
    }

    private const val TAG = "AudioEffects"
    private const val LOUDNESS_GAIN_MB = 600
}

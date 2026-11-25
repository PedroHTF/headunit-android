package com.headunit.feature.media

import androidx.media3.common.AudioAttributes
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Serviço que mantém o player vivo; a interface conversa com ele via MediaController. */
@OptIn(UnstableApi::class) // audioSessionId do ExoPlayer, para ligar o equalizador
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        session = MediaSession.Builder(this, player).build()

        // Equalizador do app no áudio deste player.
        AudioEffects.attach(player.audioSessionId)
        effectsJob = scope.launch { AudioEffects.settings.collect(AudioEffects::apply) }
    }

    private val scope = MainScope()
    private var effectsJob: Job? = null

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        effectsJob?.cancel()
        scope.cancel()
        AudioEffects.release()
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}

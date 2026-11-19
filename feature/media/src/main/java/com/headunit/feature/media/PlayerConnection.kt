package com.headunit.feature.media

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val title: String? = null,
    val subtitle: String? = null,
    val isPlaying: Boolean = false,
    val isVideo: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
) {
    val hasMedia: Boolean get() = title != null
}

/**
 * Conexão do app com o [PlaybackService]. Fica viva enquanto o app existir, para que
 * os botões do volante funcionem em qualquer tela.
 */
class PlayerConnection(private val context: Context) {
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)
    }

    fun connect() {
        if (_controller.value != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller.addListener(listener)
            _controller.value = controller
            refresh(controller)
        }, ContextCompat.getMainExecutor(context))
    }

    fun play(items: List<LocalMedia>, startIndex: Int) {
        val controller = _controller.value ?: return
        controller.setMediaItems(items.map { it.toMediaItem() }, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val c = _controller.value ?: return
        if (c.isPlaying) c.pause() else if (c.mediaItemCount > 0) c.play()
    }

    fun pause() {
        _controller.value?.pause()
    }

    fun next() {
        _controller.value?.seekToNext()
    }

    fun previous() {
        _controller.value?.seekToPrevious()
    }

    private fun refresh(player: Player) {
        val metadata = player.mediaMetadata
        _nowPlaying.value = if (player.currentMediaItem == null) {
            NowPlaying()
        } else {
            NowPlaying(
                title = metadata.title?.toString(),
                subtitle = metadata.artist?.toString(),
                isPlaying = player.isPlaying,
                isVideo = metadata.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem() || player.currentPosition > 0,
            )
        }
    }

    private fun LocalMedia.toMediaItem(): MediaItem = MediaItem.Builder()
        .setUri(uri)
        .setMediaId(uri.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(subtitle)
                .setMediaType(if (isVideo) MediaMetadata.MEDIA_TYPE_VIDEO else MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()
}

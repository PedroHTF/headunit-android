package com.headunit.feature.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalMedia(
    val uri: Uri,
    val title: String,
    val subtitle: String,
    val durationMs: Long,
    val isVideo: Boolean,
    /** Nome do volume: armazenamento interno, pendrive, cartão SD... */
    val volume: String,
)

/**
 * Lista músicas e vídeos indexados pelo Android (MediaStore) em todos os volumes,
 * incluindo pendrives USB, que o sistema indexa sozinho ao montar.
 */
class MediaRepository(private val context: Context) {

    suspend fun load(): List<LocalMedia> = withContext(Dispatchers.IO) {
        volumes().flatMap { volume -> queryAudio(volume) + queryVideo(volume) }
    }

    private fun volumes(): Set<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context)
        } else {
            setOf("external")
        }

    private fun queryAudio(volume: String): List<LocalMedia> {
        val collection = MediaStore.Audio.Media.getContentUri(volume)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )
        val result = mutableListOf<LocalMedia>()
        context.contentResolver.query(
            collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val duration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                result += LocalMedia(
                    uri = ContentUris.withAppendedId(collection, c.getLong(id)),
                    title = c.getString(title) ?: "Sem título",
                    subtitle = c.getString(artist)?.takeUnless { it == "<unknown>" } ?: "Artista desconhecido",
                    durationMs = c.getLong(duration),
                    isVideo = false,
                    volume = volumeLabel(volume),
                )
            }
        }
        return result
    }

    private fun queryVideo(volume: String): List<LocalMedia> {
        val collection = MediaStore.Video.Media.getContentUri(volume)
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
        )
        val result = mutableListOf<LocalMedia>()
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Video.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val title = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val duration = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (c.moveToNext()) {
                result += LocalMedia(
                    uri = ContentUris.withAppendedId(collection, c.getLong(id)),
                    title = c.getString(title) ?: "Sem título",
                    subtitle = "Vídeo",
                    durationMs = c.getLong(duration),
                    isVideo = true,
                    volume = volumeLabel(volume),
                )
            }
        }
        return result
    }

    private fun volumeLabel(volume: String) = when (volume) {
        "external", "external_primary" -> "Interno"
        else -> "USB/SD"
    }
}

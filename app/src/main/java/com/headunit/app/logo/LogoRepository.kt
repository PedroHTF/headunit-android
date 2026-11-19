package com.headunit.app.logo

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.headunit.app.settings.LogoKind
import com.headunit.app.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** Arquivo de logo encontrado no pendrive/armazenamento. */
data class LogoCandidate(val uri: Uri, val name: String, val isVideo: Boolean, val location: String)

/**
 * Logo da inicialização. O arquivo escolhido é **copiado** para dentro do app, então
 * continua funcionando depois que o pendrive é removido.
 *
 * Convenção no pendrive: pasta `HeadUnit/` com `logo.png`, `logo.jpg`, `logo.webp`
 * ou `logo.mp4`.
 */
class LogoRepository(private val context: Context, private val settings: SettingsRepository) {
    val file: File get() = File(context.filesDir, "boot_logo")

    /** Muda a cada importação, para a tela recarregar a imagem. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    suspend fun import(uri: Uri, isVideo: Boolean): Boolean = withContext(Dispatchers.IO) {
        val ok = runCatching {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        }.isSuccess
        if (ok) {
            settings.update { it.copy(logoKind = if (isVideo) LogoKind.VIDEO else LogoKind.IMAGE) }
            _version.value++
        }
        ok
    }

    fun useDefault() {
        settings.update { it.copy(logoKind = LogoKind.DEFAULT) }
        file.delete()
        _version.value++
    }

    /** Procura `HeadUnit/logo.*` em todos os volumes (interno e pendrives). */
    suspend fun findCandidates(): List<LogoCandidate> = withContext(Dispatchers.IO) {
        val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context)
        } else {
            setOf("external")
        }
        volumes.flatMap { volume ->
            query(MediaStore.Images.Media.getContentUri(volume), false, volume) +
                query(MediaStore.Video.Media.getContentUri(volume), true, volume)
        }
    }

    private fun query(collection: Uri, isVideo: Boolean, volume: String): List<LogoCandidate> {
        val folderCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.MediaColumns.DATA
        }
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE 'logo.%' AND $folderCol LIKE '%HeadUnit%'"
        val out = mutableListOf<LogoCandidate>()
        runCatching {
            context.contentResolver.query(
                collection, arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
                selection, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    out += LogoCandidate(
                        uri = ContentUris.withAppendedId(collection, c.getLong(0)),
                        name = c.getString(1),
                        isVideo = isVideo,
                        location = if (volume.startsWith("external")) "Interno" else "USB/SD ($volume)",
                    )
                }
            }
        }
        return out
    }
}

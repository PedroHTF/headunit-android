package com.headunit.feature.media

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val mediaPermissions: Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

/**
 * Tela de mídia: lista de músicas/vídeos (interno + pendrive) e o que está tocando.
 *
 * @param videoAllowed false quando o carro está em movimento: o vídeo some, o áudio continua.
 * @param onPlaybackStarted avisa o app para parar outras fontes (ex.: rádio).
 */
@Composable
fun MediaScreen(
    connection: PlayerConnection,
    videoAllowed: Boolean,
    onPlaybackStarted: () -> Unit,
    modifier: Modifier = Modifier,
    /** Substitui a leitura do MediaStore (usado nos testes de tela). */
    loadItems: (suspend () -> List<LocalMedia>)? = null,
    initialTab: Int = 0,
) {
    val context = LocalContext.current
    val repository = remember { MediaRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    fun hasPermission() = mediaPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var granted by remember { mutableStateOf(loadItems != null || hasPermission()) }
    var items by remember { mutableStateOf<List<LocalMedia>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }

    fun reload() {
        if (!granted) return
        scope.launch {
            loading = true
            items = runCatching { loadItems?.invoke() ?: repository.load() }.getOrDefault(emptyList())
            loading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        granted = hasPermission()
        reload()
    }

    LaunchedEffect(Unit) {
        if (granted) reload() else permissionLauncher.launch(mediaPermissions)
    }

    val visible = items.filter { it.isVideo == (tab == 1) }
    var folderVolume by rememberSaveable { mutableStateOf<String?>(null) }
    var folderPath by rememberSaveable { mutableStateOf("") }

    Row(modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // Lista
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabRow(selectedTabIndex = tab, modifier = Modifier.weight(1f)) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Músicas") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Vídeos") })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Pastas") })
                }
                IconButton(onClick = ::reload) { Icon(Icons.Filled.Refresh, contentDescription = "Atualizar") }
            }
            Spacer(Modifier.height(8.dp))
            when {
                !granted -> Placeholder(
                    "O app precisa de permissão para ler músicas e vídeos.",
                    action = "Permitir" to { permissionLauncher.launch(mediaPermissions) },
                )
                loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                tab == 2 -> FolderBrowser(
                    items = items,
                    volumeId = folderVolume,
                    path = folderPath,
                    onNavigate = { v, p -> folderVolume = v; folderPath = p },
                    onPlay = { list, index ->
                        onPlaybackStarted()
                        connection.play(list, index)
                    },
                )
                visible.isEmpty() -> Placeholder(
                    "Nenhum arquivo encontrado.\n\nNo emulador: arraste arquivos MP3/MP4 para a janela " +
                        "e toque em atualizar. Veja docs/setup-windows.md para simular um pendrive.",
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(visible, key = { _, m -> m.uri.toString() }) { index, media ->
                        MediaRow(media) {
                            onPlaybackStarted()
                            connection.play(visible, index)
                        }
                    }
                }
            }
        }
        // Tocando agora
        NowPlayingPanel(connection, videoAllowed, Modifier.weight(1f).fillMaxHeight())
    }
}

/**
 * Navega pelos volumes (interno, pendrive) e pastas. Tocar num arquivo toca a pasta
 * inteira a partir dele (músicas e vídeos misturados, como num pendrive no carro).
 */
@Composable
private fun FolderBrowser(
    items: List<LocalMedia>,
    volumeId: String?,
    path: String,
    onNavigate: (String?, String) -> Unit,
    onPlay: (List<LocalMedia>, Int) -> Unit,
) {
    val volumes = items.groupBy { it.volumeId }
    if (volumeId == null || volumeId !in volumes) {
        if (volumes.isEmpty()) {
            Placeholder("Nenhuma pasta com músicas ou vídeos.")
            return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            volumes.forEach { (id, list) ->
                item(key = "vol-$id") {
                    FolderRow(Icons.Filled.Usb, list.first().volume + " (" + id + ")", "${list.size} arquivos") { onNavigate(id, "") }
                }
            }
        }
        return
    }
    val inVolume = volumes.getValue(volumeId)
    val folders = inVolume.map { it.folder }
    val subfolders = FolderTree.subfolders(folders, path)
    val files = inVolume.filter { it.folder == path }.sortedBy { it.title.lowercase() }
    val crumbs = (listOf(inVolume.first().volume) + path.split('/').filter { it.isNotEmpty() }).joinToString("  ›  ")

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                val parent = FolderTree.parent(path)
                if (parent == null) onNavigate(null, "") else onNavigate(volumeId, parent)
            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
            Text(crumbs, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            itemsIndexed(subfolders, key = { _, f -> "dir-$f" }) { _, name ->
                val full = FolderTree.join(path, name)
                FolderRow(Icons.Filled.Folder, name, FolderTree.countUnder(folders, full).let { n -> if (n == 1) "1 arquivo" else "$n arquivos" }) { onNavigate(volumeId, full) }
            }
            itemsIndexed(files, key = { _, m -> m.uri.toString() }) { index, media ->
                MediaRow(media) { onPlay(files, index) }
            }
        }
    }
}

@Composable
private fun FolderRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MediaRow(media: LocalMedia, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (media.isVideo) Icons.Filled.Movie else Icons.Filled.MusicNote, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(media.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text(media.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        }
        if (media.volume != "Interno") {
            Icon(Icons.Filled.Usb, contentDescription = media.volume, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(formatDuration(media.durationMs), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun NowPlayingPanel(connection: PlayerConnection, videoAllowed: Boolean, modifier: Modifier) {
    val controller by connection.controller.collectAsStateWithLifecycle()
    val now by connection.nowPlaying.collectAsStateWithLifecycle()
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(controller, now.isPlaying) {
        while (true) {
            val c = controller
            progress = if (c != null && c.duration > 0) c.currentPosition.toFloat() / c.duration else 0f
            delay(500)
        }
    }

    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
            when {
                now.isVideo && videoAllowed && controller != null -> AndroidView(
                    factory = { PlayerView(it).apply { useController = false } },
                    update = { it.player = controller },
                    onRelease = { it.player = null },
                    modifier = Modifier.fillMaxSize(),
                )
                now.isVideo -> Text(
                    "Vídeo oculto com o carro em movimento.\nO áudio continua.",
                    color = Color.White, textAlign = TextAlign.Center,
                )
                else -> Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(96.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(now.title ?: "Nada tocando", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(now.subtitle.orEmpty(), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = connection::previous, enabled = now.hasMedia) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(36.dp))
            }
            FilledIconButton(onClick = connection::togglePlayPause, enabled = now.hasMedia, modifier = Modifier.size(64.dp)) {
                Icon(
                    if (now.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Tocar/pausar", modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = connection::next, enabled = now.hasNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Próxima", modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
private fun Placeholder(text: String, action: Pair<String, () -> Unit>? = null) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Usb, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(text, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = action.second) { Text(action.first) }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

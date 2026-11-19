package com.headunit.app.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headunit.app.AppContainer
import com.headunit.app.AudioSource
import com.headunit.app.settings.ProjectionMode
import com.headunit.app.ui.Screen
import com.headunit.core.hal.vehicle.SteeringKey

@Composable
fun HomeScreen(container: AppContainer, onNavigate: (Screen) -> Unit) {
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        NowPlayingCard(container, Modifier.weight(1f).fillMaxHeight())
        Column(Modifier.weight(1.3f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("Android Auto", Icons.Filled.PhoneAndroid, Modifier.weight(1f).fillMaxHeight()) {
                    container.openProjection(ProjectionMode.ANDROID_AUTO)
                }
                Tile("CarPlay", Icons.Filled.PhoneIphone, Modifier.weight(1f).fillMaxHeight()) {
                    container.openProjection(ProjectionMode.CARPLAY)
                }
            }
            listOf(Screen.MEDIA to Screen.RADIO, Screen.APPS to Screen.SETTINGS).forEach { (a, b) ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile(a.label, a.icon, Modifier.weight(1f).fillMaxHeight()) { onNavigate(a) }
                    Tile(b.label, b.icon, Modifier.weight(1f).fillMaxHeight()) { onNavigate(b) }
                }
            }
        }
    }
}

@Composable
private fun Tile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NowPlayingCard(container: AppContainer, modifier: Modifier) {
    val source by container.audioSource.collectAsStateWithLifecycle()
    val now by container.player.nowPlaying.collectAsStateWithLifecycle()
    val radio by container.radio.state.collectAsStateWithLifecycle()

    val (title, subtitle, playing) = when (source) {
        AudioSource.RADIO -> Triple(
            "${radio.frequencyLabel} ${radio.band.unit}",
            radio.stationName ?: "Rádio",
            radio.isOn,
        )
        else -> Triple(now.title ?: "Nada tocando", now.subtitle ?: "Escolha algo em Mídia ou Rádio", now.isPlaying)
    }

    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Tocando agora · ${source.label}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(24.dp))
        // Os mesmos comandos dos botões do volante: funcionam para a fonte ativa.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { container.handleSteeringKey(SteeringKey.PREVIOUS) }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(40.dp))
            }
            FilledIconButton(onClick = { container.handleSteeringKey(SteeringKey.PLAY_PAUSE) }, modifier = Modifier.size(72.dp)) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Tocar/pausar", modifier = Modifier.size(44.dp))
            }
            IconButton(onClick = { container.handleSteeringKey(SteeringKey.NEXT) }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Próxima", modifier = Modifier.size(40.dp))
            }
        }
    }
}

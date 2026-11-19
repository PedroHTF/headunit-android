package com.headunit.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.headunit.app.projection.ConnectAction
import kotlinx.coroutines.delay

/**
 * Aviso de celular conectado no meio da viagem: nada abre sozinho, quem está no carro
 * decide. Some sozinho depois de [timeoutMs].
 */
@Composable
fun ConnectBanner(
    suggestion: ConnectAction.Suggest,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    timeoutMs: Long = 20_000,
) {
    LaunchedEffect(suggestion) {
        delay(timeoutMs)
        onDismiss()
    }
    Surface(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text("${suggestion.device.name} conectou", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onOpen) { Text("Abrir ${suggestion.mode.label}") }
            TextButton(onClick = onDismiss) { Text("Agora não") }
        }
    }
}

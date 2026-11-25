package com.headunit.app.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headunit.core.hal.phone.CallState
import com.headunit.core.hal.phone.PhoneHal

private val Green = Color(0xFF2E7D32)
private val Red = Color(0xFFD32F2F)

/** Ligação chegando ou em andamento: aparece por cima de qualquer tela. */
@Composable
fun CallOverlay(phone: PhoneHal) {
    val s by phone.state.collectAsStateWithLifecycle()
    val incoming = s.callState == CallState.INCOMING
    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when (s.callState) {
                    CallState.INCOMING -> "Chamada recebida"
                    CallState.DIALING -> "Chamando…"
                    else -> "%d:%02d".format(s.callSeconds / 60, s.callSeconds % 60)
                },
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge,
            )
            Text(s.callName, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            if (s.callName != s.callNumber) Text(s.callNumber, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                if (incoming) {
                    RoundAction(Icons.Filled.CallEnd, "Recusar", Red, phone::reject)
                    RoundAction(Icons.Filled.Call, "Atender", Green, phone::accept)
                } else {
                    FilledTonalIconToggleButton(checked = s.muted, onCheckedChange = phone::setMuted, modifier = Modifier.size(72.dp)) {
                        Icon(Icons.Filled.MicOff, contentDescription = "Mudo")
                    }
                    RoundAction(Icons.Filled.CallEnd, "Desligar", Red, phone::hangup)
                }
            }
        }
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(96.dp),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = color, contentColor = Color.White),
    ) { Icon(icon, contentDescription = label, modifier = Modifier.size(40.dp)) }
}

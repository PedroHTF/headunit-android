package com.headunit.app.ui.phone

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headunit.core.hal.phone.CallKind
import com.headunit.core.hal.phone.PhoneHal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Telefone: agenda, recentes e teclado. A ligação em si aparece na CallOverlay. */
@Composable
fun PhoneScreen(phone: PhoneHal) {
    val state by phone.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var typed by rememberSaveable { mutableStateOf("") }

    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(3f).fillMaxHeight()) {
            TabRow(selectedTabIndex = tab) {
                listOf("Contatos", "Recentes", "Teclado").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.contacts, key = { it.number }) { c ->
                        PhoneRow(c.name.take(1), c.name, c.number) { phone.dial(c.number) }
                    }
                }
                1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.recents) { r ->
                        val icon = when (r.kind) {
                            CallKind.IN -> Icons.AutoMirrored.Filled.CallReceived
                            CallKind.OUT -> Icons.AutoMirrored.Filled.CallMade
                            CallKind.MISSED -> Icons.AutoMirrored.Filled.CallMissed
                        }
                        val subtitle = r.kind.label + " · " + SimpleDateFormat("dd/MM HH:mm", Locale.ROOT).format(Date(r.timeMillis))
                        PhoneRow(null, r.name, subtitle, icon = icon, missed = r.kind == CallKind.MISSED) { phone.dial(r.number) }
                    }
                }
                else -> Keypad(
                    typed = typed,
                    canCall = state.connected,
                    onDigit = { typed += it },
                    onDelete = { typed = typed.dropLast(1) },
                    onCall = { phone.dial(typed); typed = "" },
                )
            }
        }
        Column(
            Modifier.weight(2f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Celular", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(if (state.connected) state.deviceName else "Nenhum celular conectado", style = MaterialTheme.typography.titleMedium)
            if (state.connected) {
                Text("Sinal " + "▮".repeat(state.signal) + "▯".repeat(5 - state.signal) + "   Bateria ${state.battery}%",
                    style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.weight(1f))
            Text(
                (if (phone.isSimulated) "Telefone simulado: receba ligações pela tela Veículo. " else "") +
                    "O botão de telefone do volante atende e desliga.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PhoneRow(
    initial: String?,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    missed: Boolean = false,
    onCall: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onCall)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            if (initial != null) {
                Text(initial, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
            } else if (icon != null) {
                Icon(icon, contentDescription = null,
                    tint = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Filled.Call, contentDescription = "Ligar", tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Keypad(typed: String, canCall: Boolean, onDigit: (String) -> Unit, onDelete: () -> Unit, onCall: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            typed.ifEmpty { "Digite o número" },
            fontSize = if (typed.isEmpty()) 16.sp else 28.sp,
            fontWeight = FontWeight.Medium,
            color = if (typed.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
        )
        listOf("123", "456", "789", "*0#").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { d ->
                    FilledTonalButton(onClick = { onDigit(d.toString()) }, shape = CircleShape, modifier = Modifier.size(56.dp)) {
                        Text(d.toString(), fontSize = 22.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDelete, enabled = typed.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Apagar")
            }
            Button(onClick = onCall, enabled = typed.isNotEmpty() && canCall) {
                Icon(Icons.Filled.Call, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ligar")
            }
        }
    }
}

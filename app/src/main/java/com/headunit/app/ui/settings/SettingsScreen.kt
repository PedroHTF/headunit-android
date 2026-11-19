package com.headunit.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.headunit.app.AppContainer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.headunit.app.logo.DefaultLogo
import com.headunit.app.logo.LogoCandidate
import com.headunit.app.settings.LogoKind
import com.headunit.app.settings.ThemeMode
import kotlinx.coroutines.launch
import com.headunit.app.projection.AppEntry
import com.headunit.app.projection.BtDevice
import com.headunit.app.settings.LaunchTarget
import com.headunit.app.settings.ProjectionMode

@Composable
fun SettingsScreen(container: AppContainer, onOpenCameraSetup: () -> Unit, onPreviewLogo: () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val connected by container.bluetooth.connected.collectAsStateWithLifecycle()
    var btGranted by remember { mutableStateOf(container.bluetooth.hasPermission()) }
    // Incrementado para recarregar listas (aparelhos pareados, alvo detectado).
    var refresh by remember { mutableIntStateOf(0) }
    var pickerFor by remember { mutableStateOf<ProjectionMode?>(null) }

    val btPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        btGranted = container.bluetooth.hasPermission()
        refresh++
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Section("Aparência") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Tema", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { container.settings.update { it.copy(themeMode = mode) } },
                        label = { Text(mode.label) },
                    )
                }
            }
            Text(
                "Automático segue o modo noturno do sistema (nas centrais, normalmente ligado ao farol).",
                style = MaterialTheme.typography.bodySmall,
            )
            SwitchRow(
                title = "Alto contraste",
                subtitle = "Preto e branco puros e cores de destaque fortes, para ler sob sol forte ou com baixa visão.",
                checked = settings.highContrast,
                onChange = { v -> container.settings.update { it.copy(highContrast = v) } },
            )
        }

        Section("Vídeo") {
            SwitchRow(
                title = "Vídeo em qualquer velocidade",
                subtitle = "Desligado: o vídeo some acima de 5 km/h e só o áudio continua. " +
                    "Ligado: o vídeo aparece sempre. Use com responsabilidade; o motorista não deve assistir dirigindo.",
                checked = settings.videoWhileDriving,
                onChange = { v -> container.settings.update { it.copy(videoWhileDriving = v) } },
            )
        }

        Section("Logo de inicialização") {
            LogoSettings(container, onPreviewLogo)
        }

        Section("Câmera de ré") {
            Text(
                "Medidas do carro, posição da câmera, correção do fisheye, linhas de distância " +
                    "(em metros ou em carros) e trajetória pelo volante, com prévia ao vivo.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenCameraSetup) { Text("Ajustar câmera de ré") }
        }

        Section("Android Auto e CarPlay (Zlink)") {
            Text(
                "Os ícones da tela inicial abrem o app abaixo. Sem escolha, o Zlink é detectado sozinho. " +
                    "Se o Zlink tiver uma tela própria para cada modo, escolha-a em \"Escolher\".",
                style = MaterialTheme.typography.bodySmall,
            )
            ProjectionMode.entries.forEach { mode ->
                key(refresh) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(mode.label, style = MaterialTheme.typography.titleMedium)
                            Text(container.projection.describe(mode), style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { pickerFor = mode }) { Text("Escolher") }
                        Spacer(Modifier.padding(4.dp))
                        if (settings.target(mode) != null) {
                            TextButton(onClick = { container.settings.update { it.withTarget(mode, null) } }) { Text("Automático") }
                        }
                        FilledTonalButton(onClick = { container.openProjection(mode) }) { Text("Abrir") }
                    }
                }
            }
        }

        Section("Abrir automaticamente pelo Bluetooth") {
            SwitchRow(
                title = "Abrir ao conectar o celular",
                subtitle = "Só na primeira conexão depois de ligar o carro. Conexões no meio da viagem " +
                    "(queda e volta, ou outro celular) mostram um aviso: é preciso tocar para abrir.",
                checked = settings.autoOpenOnBluetooth,
                onChange = { v -> container.settings.update { it.copy(autoOpenOnBluetooth = v) } },
            )
            SwitchRow(
                title = "Ao ligar o carro, só o último aparelho",
                subtitle = "Na primeira conexão, ignora outros celulares (ex.: o do passageiro). Último: " +
                    (settings.lastDevice?.let { "${it.name} → ${it.mode.label}" } ?: "nenhum ainda"),
                checked = settings.onlyLastDevice,
                onChange = { v -> container.settings.update { it.copy(onlyLastDevice = v) } },
            )
            Text(
                "Como o app aprende: com o celular conectado no Bluetooth, toque uma vez no ícone " +
                    "Android Auto ou CarPlay. Ou defina abaixo, por aparelho.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            if (!btGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Button(onClick = { btPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) }) {
                    Text("Permitir acesso ao Bluetooth")
                }
            }
            if (connected.isNotEmpty()) {
                Text("Conectado agora: " + connected.joinToString { it.name }, style = MaterialTheme.typography.labelLarge)
            }
            val paired = remember(refresh, btGranted) { container.bluetooth.pairedDevices() }
            val known = settings.devices.values.map { BtDevice(it.address, it.name) }
            val devices = (paired + known).distinctBy { it.address }
            if (devices.isEmpty()) {
                Text("Nenhum aparelho pareado/conhecido.", style = MaterialTheme.typography.bodySmall)
            }
            devices.forEach { device ->
                val current = settings.devices[device.address]?.mode
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(device.name, modifier = Modifier.weight(1f))
                    FilterChip(selected = current == null, label = { Text("Nenhum") }, onClick = {
                        container.settings.update { it.setDeviceMode(device.address, device.name, null) }
                    })
                    ProjectionMode.entries.forEach { mode ->
                        Spacer(Modifier.padding(2.dp))
                        FilterChip(selected = current == mode, label = { Text(mode.label) }, onClick = {
                            container.settings.update { it.setDeviceMode(device.address, device.name, mode) }
                        })
                    }
                }
            }
        }

        Section("Simulação (testes no emulador)") {
            Text(
                "Simula um celular conectando no Bluetooth. Para ver o app abrindo no emulador, " +
                    "escolha qualquer app (ex.: Configurações) como alvo acima.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                SIMULATED.forEach { device ->
                    OutlinedButton(onClick = { container.bluetooth.deviceConnected(device) }) { Text("Conectar ${device.name}") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    SIMULATED.forEach(container.bluetooth::deviceDisconnected)
                }) { Text("Desconectar simulados") }
                OutlinedButton(onClick = container::resetAutoLaunch) { Text("Nova partida (ligar o carro)") }
            }
        }
    }

    pickerFor?.let { mode ->
        TargetPicker(
            container = container,
            mode = mode,
            onDismiss = { pickerFor = null },
            onPicked = { target ->
                container.settings.update { it.withTarget(mode, target) }
                pickerFor = null
                refresh++
            },
        )
    }
}

/** Escolha em duas etapas: o app e, se houver mais de uma, a tela de abertura. */
@Composable
private fun TargetPicker(
    container: AppContainer,
    mode: ProjectionMode,
    onDismiss: () -> Unit,
    onPicked: (LaunchTarget) -> Unit,
) {
    val apps = remember { container.projection.launchableApps() }
    var app by remember { mutableStateOf<AppEntry?>(null) }
    val chosen = app

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text(if (chosen == null) "App para ${mode.label}" else "Tela de abertura: ${chosen.label}") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                if (chosen == null) {
                    items(apps, key = { it.packageName }) { entry ->
                        PickRow(entry.label, entry.packageName) {
                            val screens = container.projection.exportedActivities(entry.packageName)
                            if (screens.size <= 1) onPicked(LaunchTarget(entry.packageName)) else app = entry
                        }
                    }
                } else {
                    item { PickRow("Padrão", "Tela inicial do app") { onPicked(LaunchTarget(chosen.packageName)) } }
                    items(container.projection.exportedActivities(chosen.packageName), key = { it.name }) { screen ->
                        PickRow(screen.name.substringAfterLast('.'), screen.label) {
                            onPicked(LaunchTarget(chosen.packageName, screen.name))
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PickRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LogoSettings(container: AppContainer, onPreview: () -> Unit) {
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var candidates by remember { mutableStateOf<List<LogoCandidate>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun import(uri: android.net.Uri, isVideo: Boolean) = scope.launch {
        message = if (container.logo.import(uri, isVideo)) "Logo atualizado." else "Não foi possível ler o arquivo."
        candidates = null
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) import(uri, context.contentResolver.getType(uri)?.startsWith("video") == true)
    }
    val scan = {
        scope.launch {
            candidates = container.logo.findCandidates()
            if (candidates.isNullOrEmpty()) message = "Nenhum HeadUnit/logo.* encontrado."
        }
    }
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { scan() }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(213.dp).height(120.dp).clip(RoundedCornerShape(12.dp))) {
            when (settings.logoKind) {
                LogoKind.DEFAULT -> DefaultLogo(Modifier.fillMaxSize(), animate = false)
                LogoKind.IMAGE -> PreviewLabel("Imagem personalizada")
                LogoKind.VIDEO -> PreviewLabel("Vídeo personalizado")
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "No pendrive, crie a pasta HeadUnit e coloque logo.png, logo.jpg ou logo.mp4. " +
                    "O arquivo é copiado para a central: depois o pendrive pode sair.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    readPermission.launch(perms)
                }) { Text("Procurar no pendrive") }
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*", "video/*")) }) { Text("Escolher arquivo") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreview) { Text("Ver agora") }
                if (settings.logoKind != LogoKind.DEFAULT) {
                    TextButton(onClick = container.logo::useDefault) { Text("Usar logo padrão") }
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
    candidates?.takeIf { it.isNotEmpty() }?.let { list ->
        Text("Encontrados:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        list.forEach { c ->
            PickRow("${c.name}  ·  ${c.location}", if (c.isVideo) "Vídeo — toque para usar" else "Imagem — toque para usar") {
                import(c.uri, c.isVideo)
            }
        }
    }
    Text("Tempo na tela: ${settings.logoSeconds} s", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    Slider(
        value = settings.logoSeconds.toFloat(),
        onValueChange = { v -> container.settings.update { it.copy(logoSeconds = v.toInt()) } },
        valueRange = 1f..8f,
        steps = 6,
    )
    SwitchRow(
        title = "Mostrar ao ligar a ignição",
        subtitle = "Além de quando o sistema inicia. Toque no logo para pular.",
        checked = settings.logoOnIgnition,
        onChange = { v -> container.settings.update { it.copy(logoOnIgnition = v) } },
    )
}

@Composable
private fun PreviewLabel(text: String) {
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black), contentAlignment = Alignment.Center) {
        Text(text, color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

private val SIMULATED = listOf(
    BtDevice("02:00:00:00:00:01", "Android (simulado)"),
    BtDevice("02:00:00:00:00:02", "iPhone (simulado)"),
)

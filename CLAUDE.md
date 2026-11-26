# CLAUDE.md — headunit-android

Launcher para central multimídia automotiva (uso pessoal: Honda HR-V, central genérica
Android com Zlink). Kotlin + Jetpack Compose, roda em Android comum, tablet, centrais
genéricas e Android Automotive (AAOS). Projeto irmão: `PedroHTF/headunit-linux` (Qt/QML).

## Regras de trabalho

- **Sempre em português (pt-BR):** conversa, textos da interface, comentários, commits e docs.
- O dono usa **Windows**: instruções para ele devem servir no Windows (Android Studio,
  `gradlew.bat`, caminhos `C:\...`). Guia: `docs/setup-windows.md`.
- **Toda mudança de interface é conferida com capturas de tela** (ver abaixo), nos temas
  claro, escuro e alto contraste quando fizer sentido. Tela de referência: 1024×600, paisagem.
- Mantenha a paridade com o `headunit-linux`: paletas (`ui/theme/Theme.kt` ↔ `qml/Theme.qml`,
  mesmos valores) e geometria da câmera (`core/camera` ↔ `src/camera`). Mudou de um lado,
  mude (ou anote) do outro.
- Hardware real ainda é desconhecido (modelo da central/MCU a confirmar): toda integração
  nova entra **atrás de uma HAL**, com implementação simulada que funciona no PC/emulador.
- Atualize `README.md` (tabela "O que já funciona") e `docs/` quando a função mudar.

## Comandos

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug      # tudo (igual ao CI)
./gradlew :core:hal:testDebugUnitTest                    # só um módulo
./gradlew :app:testDebugUnitTest --tests '*ScreenshotsTest*' -Dscreenshots=/caminho/pasta
```

As capturas usam Robolectric (`app/src/test/.../screens/ScreenshotsTest.kt`, SDK 34,
`w1024dp-h600dp`) — não precisa de emulador. Sem `-Dscreenshots` o teste é pulado.
Novas telas/estados: adicione um `shoot(...)` nesse arquivo.

CI (`.github/workflows/android.yml`): build + testes + lint; publica `headunit-debug-apk`.

## Contêiner novo (nuvem)

1. Android SDK em `/opt/android-sdk` (cmdline-tools, `platforms;android-35`,
   `build-tools;35.0.0`) e `local.properties` com `sdk.dir=/opt/android-sdk`.
2. O ambiente "headunit" libera `dl.google.com` e `gcp.mirrors.maven.org`.
3. Se o Robolectric der **429** no espelho do Maven: baixe os jars (android-all-instrumented
   etc.) com `curl` de `repo1.maven.org` para `~/.m2/repository` e use um init script do
   Gradle que coloque `mavenLocal()` primeiro.

## Arquitetura

```
app/                 MainActivity, AppContainer (controlador central), telas Compose
  settings/            Settings + SettingsRepository, CameraConfig, EqualizerRepository
  projection/          ProjectionLauncher (abre o Zlink), BluetoothMonitor, AutoLaunchPolicy
  logo/                BootLogo / LogoRepository (logo padrão ou HeadUnit/logo.* do pendrive)
  ui/                  home, apps, vehicle, camera, phone, audio (equalizador), settings, theme
feature/media/       MediaRepository (MediaStore), FolderTree, PlaybackService (Media3),
                     PlayerConnection, AudioEffects (equalizador real)
feature/radio/       RadioScreen
core/hal/            HALs — a única parte que muda entre PC e central
  vehicle/             VehicleHal: SimulatedVehicleHal | CarApiVehicleHal (AAOS) | (futuro) MCU
  radio/               RadioHal: SimulatedRadioHal | (futuro) chip via MCU
  phone/               PhoneHal: SimulatedPhoneHal | (futuro) serviço BT do fabricante
  audio/               EqualizerSettings
  DrivingRules         vídeo bloqueado acima de 5 km/h (salvo se liberado), câmera de ré
core/camera/         Geometria pura (Kotlin/JVM): CameraModel, Undistort (fisheye),
                     Guidelines (metros, "carros", trajetória pelo volante), SyntheticScene
```

**AppContainer** decide: fonte de áudio ativa (mídia/rádio), botões do volante, ACC desligado
para tudo, ligação pausa e retoma a mídia, regras de direção.

**Zlink:** pacote detectado sozinho (nome contém "zlink" ou app Zlink/Car Auto/CarLink) ou
escolhido nas Configurações. Abertura automática só na 1ª conexão Bluetooth da viagem
(`AutoLaunchPolicy`); depois, aviso com botão "Abrir" (`ui/common/ConnectBanner.kt`).

## Pendências conhecidas

- Telefone real: serviço Bluetooth do fabricante da central (hoje simulado).
- MCU (ACC, ré, volante, CAN), rádio real, câmera real e nome exato do pacote do Zlink:
  dependem dos dados da central (modelo, CPU, versão do Android, `adb shell pm list packages`).
- Redesenho do layout a partir do Figma do dono (1024×600; claro, escuro, alto contraste).

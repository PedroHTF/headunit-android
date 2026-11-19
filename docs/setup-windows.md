# Ambiente de desenvolvimento no Windows

## 1. Instalar o Android Studio

1. Baixe a versão estável mais recente em <https://developer.android.com/studio> e instale
   com as opções padrão (inclui o Android SDK e o emulador).
2. Ative a aceleração do emulador: **Painel de Controle → Programas → Ativar ou desativar
   recursos do Windows** → marque **Plataforma do Hipervisor do Windows** → reinicie.
   (Se o Android Studio sugerir instalar o "Android Emulator hypervisor driver", aceite.)

## 2. Abrir o projeto

1. Clone o repositório (GitHub Desktop, ou `git clone https://github.com/PedroHTF/headunit-android`).
2. Android Studio → **Open** → escolha a pasta `headunit-android`.
3. Espere o *Gradle sync* terminar (a primeira vez baixa bastante coisa).
   Se pedir, instale o **Android SDK Platform 35** pelo próprio aviso.

## 3. Criar os emuladores

Abra **Device Manager** (ícone de celular na barra lateral) → **+** → **Create Virtual Device**.

### A) Android Automotive (recomendado para testar dados do carro)

1. Categoria **Automotive** → **Automotive (1024p landscape)** → Next.
2. Escolha uma imagem de sistema *Automotive* recente, x86_64 (clique no ícone de download).
3. Finish e inicie o emulador.

### B) Tablet 1024×600 (mais parecido com a sua central genérica)

A central genérica roda Android comum, não Automotive. Para imitar:

1. **New Hardware Profile**: nome `Central 1024x600`, tela 7", resolução **1024 × 600**,
   orientação padrão **Landscape**, sem câmera/sensores. Finish.
2. Selecione o perfil → imagem de sistema **API 29 ou 30** (Android 10/11, comum nessas centrais).
3. Finish e inicie.

Nesse emulador os dados do carro vêm do **simulador** (tela "Veículo" do app).

## 4. Rodar o app

Selecione o emulador no topo do Android Studio e clique em **Run ▶**.
Na primeira vez, permita o acesso a músicas e vídeos.

Para virar a tela inicial: aperte **Home** no emulador e escolha **HeadUnit → Sempre**.
(No AAOS a escolha pode não aparecer; abra o app normalmente.)

## 5. Simulações

### Dados do carro
- **Qualquer emulador**: tela **Veículo** do app (ignição, marcha, velocidade, volante).
- **Emulador AAOS**: botão **⋯ (Extended controls) → Car data**. Altere *Gear*,
  *Ignition* e *Speed*; o app reage na hora. O app pede a permissão de velocidade na primeira vez.

### Botões do volante
Com o app aberto, em um terminal (o `adb` fica em
`%LOCALAPPDATA%\Android\Sdk\platform-tools`):

```bat
adb shell input keyevent KEYCODE_MEDIA_NEXT
adb shell input keyevent KEYCODE_MEDIA_PREVIOUS
adb shell input keyevent KEYCODE_MEDIA_PLAY_PAUSE
```

### Músicas e vídeos
Arraste arquivos MP3/MP4 para a janela do emulador (vão para *Download*), depois toque em
**atualizar** na tela Mídia. Se não aparecerem, force a indexação:

```bat
adb shell content call --uri content://media --method scan_volume --arg external_primary
```

### Simular um pendrive (avançado)
O Android consegue criar um "disco virtual" removível, igual a um pendrive:

```bat
adb shell sm set-virtual-disk true
adb shell sm list-disks
:: use o ID que aparecer, por exemplo disk:7,392
adb shell sm partition disk:7,392 public
adb shell sm list-volumes
```

O volume aparece como `public:7,xxx mounted XXXX-XXXX`. Copie arquivos para ele:

```bat
adb push musica.mp3 /storage/XXXX-XXXX/
```

Na lista de mídia, esses arquivos aparecem com o ícone de USB.

## 6. Linha de comando (opcional)

```bat
gradlew.bat assembleDebug
gradlew.bat testDebugUnitTest
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Problemas comuns

| Sintoma | Solução |
|---|---|
| Emulador muito lento | Verifique a Plataforma do Hipervisor do Windows (passo 1.2) |
| "SDK location not found" | Abra o projeto pelo Android Studio uma vez; ele cria o `local.properties` |
| Mídia vazia | Permissão negada? Configurações do emulador → Apps → HeadUnit → Permissões |
| Dados do carro não mudam no AAOS | Conceda a permissão de velocidade e confira se *Forçar simulador* está desligado |

# headunit-android

Software para central multimídia automotiva baseado em **Android**: um launcher
próprio com mídia USB, rádio e integração com os dados do carro. Roda em:

- **Emulador Android Automotive (AAOS)** no PC: dados do carro reais da API automotiva;
- **Emulador Android comum / tablet** e **centrais genéricas Android**: dados do carro simulados;
- futuramente, numa ROM própria (ROM de fábrica enxuta ou AOSP/AAOS compilado).

> Projeto irmão: [`headunit-linux`](https://github.com/PedroHTF/headunit-linux),
> o caminho com sistema Linux próprio + Qt.

## O que já funciona

| Função | Situação |
|---|---|
| Launcher (pode ser a tela inicial) | ✅ |
| Músicas e vídeos do armazenamento e de **pendrive USB** | ✅ real (MediaStore + ExoPlayer, toca em segundo plano) |
| Vídeo bloqueado com o carro em movimento (> 5 km/h), desativável | ✅ |
| Rádio FM/AM: sintonia, busca, memórias, RDS | 🟡 simulado (sem áudio) |
| Marcha, ignição e velocidade | ✅ Android Automotive / 🟡 simulado nas demais |
| Câmera de ré automática ao engatar a ré | 🟡 imagem simulada com linhas-guia |
| Tela apagada com ACC desligado | ✅ |
| Botões do volante (próxima, anterior, play, volume, fonte) | ✅ teclas de mídia / simulador |
| Ícones **Android Auto** e **CarPlay** na tela inicial, abrindo o Zlink | ✅ (app/tela configuráveis) |
| Abrir Android Auto/CarPlay sozinho ao conectar o celular no Bluetooth | ✅ 1ª conexão após ligar o carro |
| Configurações (vídeo em qualquer velocidade, projeção, Bluetooth) | ✅ |
| Abrir outros apps | ✅ tela "Apps" |
| Bluetooth (chamadas, agenda) | ⏳ próximos passos |

## Como rodar

Passo a passo completo para Windows: **[docs/setup-windows.md](docs/setup-windows.md)**.

Resumo: instale o Android Studio, abra esta pasta, crie um emulador
(*Automotive 1024p landscape* ou um tablet 1024×600) e clique em **Run ▶**.

Sem Android Studio, cada commit no GitHub gera o APK automaticamente:
aba **Actions** → execução mais recente → **Artifacts** → `headunit-debug-apk`.

## Simulação

- **Tela "Veículo"**: ignição/ACC, marcha (R abre a câmera de ré), velocidade e botões do volante.
- **Emulador AAOS**: *Extended controls (⋯) → Car data* altera marcha, ignição e velocidade
  pelo Vehicle HAL de verdade. Para usar o simulador do app mesmo assim, ligue *Forçar simulador*.
- **Volante via ADB**: `adb shell input keyevent KEYCODE_MEDIA_NEXT` (ou `PREVIOUS`, `PLAY_PAUSE`).
- **Pendrive**: veja a seção "Simular um pendrive" no guia do Windows.

## Android Auto / CarPlay (Zlink)

- Os ícones da tela inicial abrem o **Zlink**, detectado sozinho (pacote com "zlink" ou app
  chamado Zlink/Car Auto/CarLink). Em **Configurações** dá para escolher outro app e, se o Zlink
  tiver telas separadas para cada modo, a tela exata de cada ícone.
- **Abertura automática:** com o celular conectado no Bluetooth, toque uma vez no ícone do modo
  dele. O app guarda "este celular usa Android Auto" e passa a abrir sozinho na primeira conexão
  Bluetooth depois de ligar o carro. Também dá para definir o modo de cada aparelho pareado.
  Opção "só para o último aparelho usado" para ignorar o celular do passageiro.
- **No emulador:** em Configurações → Simulação, os botões "Conectar Android/iPhone (simulado)"
  disparam a mesma lógica. Escolha qualquer app (ex.: Configurações do Android) como alvo
  para ver a abertura acontecer.
- **Limitações a confirmar na central:** em algumas centrais o Bluetooth do telefone é um módulo
  à parte e o Android não vê a conexão; nesse caso vamos ouvir os avisos do app do fabricante.
  E o Android pode bloquear abrir outro app se o nosso não estiver na tela (normalmente estará,
  por ser a tela inicial).

## Arquitetura

```
app/              Launcher: navegação, tela inicial, veículo, apps, câmera de ré, configurações
  settings/         Configurações salvas no aparelho
  projection/       Zlink (ProjectionLauncher), Bluetooth (BluetoothMonitor), regra de abertura automática
feature/media/    Biblioteca de mídia (MediaStore) + PlaybackService (Media3/ExoPlayer)
feature/radio/    Tela do rádio
core/hal/         Camada de hardware (HAL) — a única parte que muda entre PC e central
  vehicle/          VehicleHal: SimulatedVehicleHal | CarApiVehicleHal (AAOS) | (futuro) MCU
  radio/            RadioHal:   SimulatedRadioHal  | (futuro) chip de rádio via MCU
  DrivingRules      Regras de segurança (vídeo em movimento, câmera de ré)
```

```
 Interface (Compose)  ──►  AppContainer  ──►  VehicleHal / RadioHal / PlayerConnection
                              │                    │
                   fonte de áudio ativa,     simulado no PC,
                   botões do volante,        real na central
                   ACC desligado
```

`AppContainer` decide qual HAL usar: Android Automotive → `CarApiVehicleHal`; qualquer
outro aparelho → `SimulatedVehicleHal`. Para a central real, a ideia é criar uma
`McuVehicleHal` que fale com o MCU (porta serial ou broadcasts do app do fabricante).

## Comandos

```bash
./gradlew assembleDebug        # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # testes (rádio simulado, regras de direção)
./gradlew lintDebug            # análise estática
```

No Windows use `gradlew.bat` no lugar de `./gradlew`.

## Limitações conhecidas

- **Rádio real**: a API de rádio do Android (Broadcast Radio HAL) só é acessível a apps de
  sistema. Na central genérica, o rádio costuma ser controlado pelo MCU; teremos de
  descobrir o protocolo.
- **Câmera de ré real**: nas centrais genéricas a câmera geralmente entra por uma entrada
  AV com driver do fabricante; ainda a definir.
- **CarPlay/Android Auto**: continuam pelo Zlink (aberto pela tela Apps).

## Próximos passos

1. Testar no emulador e ajustar a interface para a resolução da central.
2. Identificar o hardware da central (SoC, MCU, versão do Android).
3. Criar a `McuVehicleHal` / rádio real a partir da engenharia reversa.
4. Bluetooth (HFP/PBAP/A2DP), equalizador, configurações.
5. Decidir a ROM: de fábrica enxuta ou AOSP/AAOS.

# Como organizar o pendrive

A central lê **qualquer** estrutura de pastas. A pasta `HeadUnit` é reservada para
arquivos do sistema, como o logo.

```
PENDRIVE/
├── HeadUnit/
│   └── logo.png        ← logo da inicialização (ou logo.jpg, logo.webp, logo.mp4)
├── Musicas/
│   ├── Rock/
│   │   ├── Anos 80/
│   │   │   └── ... .mp3
│   │   └── ... .mp3
│   └── MPB/
│       └── ... .flac
└── Videos/
    ├── Shows/
    │   └── ... .mp4
    └── ... .mkv
```

## Mídia

- Tela **Mídia → Pastas**: escolha o volume (Interno ou USB) e navegue pelas pastas.
  Tocar num arquivo toca a pasta inteira a partir dele.
- As abas **Músicas** e **Vídeos** mostram tudo, sem pastas.
- Formatos: os que o Android da central tocar (MP3, AAC, FLAC, OGG, WAV, MP4, MKV, WebM...).
- O pendrive precisa estar em **FAT32 ou exFAT**. Algumas centrais não leem NTFS.

## Logo da inicialização

1. Crie `HeadUnit/logo.png` no pendrive. Use a resolução da tela (ex.: 1024×600 ou 1280×720)
   e fundo escuro, para não ofuscar à noite. Também vale `logo.mp4`: o vídeo começa sem som e
   tem no máximo 15 s.
2. Na central: **Configurações → Logo de inicialização → Procurar no pendrive** e toque no
   arquivo encontrado. (Ou **Escolher arquivo** para pegar de qualquer lugar.)
3. O arquivo é copiado para a central: depois o pendrive pode ser removido.
4. **Usar logo padrão** volta ao logo do app. O texto do logo padrão (ex.: "HR-V") pode ser trocado.

> Este é o logo que o **app** mostra ao iniciar e ao ligar a ignição. A animação de boot do
> próprio Android (antes do app abrir) só pode ser trocada numa ROM modificada.

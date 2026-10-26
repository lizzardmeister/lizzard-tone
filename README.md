# Lizzard Tone

Piano de referência pra afinar voz e qualquer instrumento a partir de um widget na tela inicial, sem abrir o app. Android primeiro; iOS fica pra depois. Fases, decisões e armadilhas em [PLANO.md](PLANO.md).

## O que faz

- Widget com uma oitava de teclado (brancas e pretas). Toque numa tecla e ela soa até você tocar nela de novo; outra tecla troca a nota.
- Seletor de oitava (2 a 6) e tom do instrumento (Dó, Si♭, Lá, Fá, Mi♭) no próprio widget. A tecla mostra a nota escrita e soa a nota real.
- App com o mesmo teclado em tela cheia, status da nota (nota escrita, nota real, Hz) e Lá de referência 440/442/443.
- Som sintetizado na hora (fundamental + harmônicos), por um serviço em primeiro plano cuja notificação mostra a nota e tem o botão Parar.

## Para testar no Samsung

1. Instale o Android Studio com SDK Android 36 e JDK 17 ou 21, ou use a toolchain local em `_local_toolchain` (JDK 17 + SDK), que não está no git.
2. Ative a Depuração USB no celular (Configurações > Sobre o telefone > Informações do software > 7 toques em Número da versão > Opções do desenvolvedor) e aceite o aviso ao conectar o cabo.
3. Compile e instale:

```bash
./gradlew.bat --offline assembleDebug
```

```bash
./_local_toolchain/android-sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
```

4. Na tela inicial, adicione o widget **Lizzard Tone** (4×2 células ou maior).
5. Abra o app uma vez e aceite a permissão de notificação, senão o Android 13+ esconde a notificação com o botão Parar (o som toca mesmo assim).

O projeto usa Android 8.0 (API 26) como versão mínima.

## Estrutura

- `app/src/main/java/com/lizzardtone/app/`
  - `Notes.kt`: nomes, MIDI, frequências e tons de instrumento.
  - `TuningSettings.kt`: estado compartilhado (oitava, tom, Lá, nota ativa).
  - `ToneSynth.kt`: tom contínuo por `AudioTrack`.
  - `TonePlaybackService.kt`: serviço em primeiro plano com o toggle e a notificação.
  - `ReferenceWidget.kt`: widget Glance.
  - `MainActivity.kt`: tela do app.

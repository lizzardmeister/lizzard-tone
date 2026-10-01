# Lizzard Tone

Protótipo Android para conferir se um widget pode tocar notas de referência sem abrir a interface do app.

## O que há nesta etapa

- App mínimo com quatro notas: Dó4, Mi4, Sol4 e Lá4 (440 Hz).
- Widget de tela inicial com as mesmas quatro notas.
- Reprodução local de um som sintetizado curto, iniciado por um serviço de áudio a cada toque. Os sons são provisórios; a escolha de amostras de piano será uma etapa posterior.

## Para testar no Samsung

1. Instale o Android Studio com SDK Android 36 e JDK 17 ou 21.
2. Abra este diretório como projeto no Android Studio e aguarde a sincronização do Gradle.
3. Conecte o celular com depuração USB ativada e execute o módulo `app`.
4. Na tela inicial do celular, adicione o widget **Lizzard Tone**.
5. Toque em cada botão e confira se a nota soa sem trazer o app para frente. Teste também toques repetidos e a primeira nota depois de o app ficar fechado por alguns minutos.

O projeto usa Android 8.0 (API 26) como versão mínima. Esta prova de conceito prioriza a ação do widget, não o desenho final de um teclado de piano.

## Estrutura

- `app/src/main/java`: interface, widget e serviço de áudio.
- `app/src/main/res/raw`: quatro arquivos WAV gerados localmente.
- `scripts/generate-prototype-notes.ps1`: reproduz os sons provisórios com afinação temperada e Lá4 = 440 Hz.

No Windows com execução de scripts desativada, regenere os arquivos com `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\generate-prototype-notes.ps1`.

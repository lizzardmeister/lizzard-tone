# Plano — Lizzard Tone

Piano de referência no celular, com widget na tela inicial, para afinar voz e qualquer
instrumento sem precisar abrir app nenhum. Uso pessoal, sem publicação em loja.

## Decisão de plataforma

**Android primeiro, iOS depois.** Motivos:

- O coração do app é o widget tocando som. Widget e áudio são as duas partes que
  nenhum framework multiplataforma (Flutter, React Native, Kotlin Multiplatform)
  compartilha de verdade: no Android é Glance + serviço de áudio, no iOS é
  WidgetKit/SwiftUI + App Intents. Seriam dois códigos de qualquer jeito.
- A parte que daria pra compartilhar (notas, frequências, transposição) tem umas 50
  linhas. Não compensa montar uma estrutura multiplataforma por isso.
- Sem loja, instalar no Android é só plugar o cabo USB. No iOS precisa de Mac com Xcode,
  e com Apple ID gratuito o app expira a cada 7 dias (ou US$ 99/ano pela conta de
  desenvolvedor pra valer 1 ano). Isso sozinho já justifica deixar o iOS pra depois.
- No iOS, botão interativo em widget só existe a partir do iOS 17, e uma nota que soa
  até ser desligada exige áudio em segundo plano iniciado pelo widget, que o sistema
  tende a bloquear. Precisa de prova de conceito própria antes de prometer paridade.

O que o iOS herda do Android não é código, é o desenho: teclado, toggle, transposição,
Lá de referência. Isso fica documentado aqui pra servir de spec.

## Como funciona hoje (2026-10-26)

- **Toggle.** Toque numa tecla e ela soa até você tocar nela de novo. Outra tecla troca
  a nota. Botão "Parar" no app e na notificação.
- **Som sintetizado contínuo** (`ToneSynth`): fundamental com 3 harmônicos, gerado na
  hora por `AudioTrack`. Sem amostras de piano: não precisava, e síntese dá qualquer
  frequência de graça (transposição, oitavas, Lá ajustável). Troca de nota com fade de
  20 ms, sem estalo.
- **Serviço em primeiro plano** (`TonePlaybackService`, tipo `mediaPlayback`). Voltou
  porque uma nota sem fim definido precisa do processo vivo com a tela inicial à frente.
  A notificação agora tem função: mostra o que está soando e tem o botão Parar. Pede
  foco de áudio (abaixa música de outro app) e para se perder o foco. O widget inicia o
  serviço por `PendingIntent.getForegroundService`, que o Android 16 aceita vindo do
  launcher ("Background started FGS: Allowed" no logcat).
- **Teclado de uma oitava** no widget e no app: linha de sustenidos alinhada coluna a
  coluna com as brancas (cada ♯ fica acima da tecla que ele altera). Tecla ativa fica
  âmbar nos dois.
- **Transposição por tom do instrumento**: Dó, Si♭, Lá, Fá, Mi♭. A tecla mostra a nota
  escrita e soa a nota real; cabeçalho e notificação mostram "Lá4 → Sol4". Instrumentos
  com oitava deslocada (sax tenor, barítono) usam o seletor de oitava.
- **Oitava** de 2 a 6, **Lá de referência** 440/442/443. Mudar tom ou Lá com uma nota
  soando reafina na hora.
- **Estado compartilhado** (`TuningSettings`): SharedPreferences + StateFlow, mesmo
  processo para app, widget e serviço. Toda mudança redesenha o widget. Ao abrir um
  processo novo a nota ativa é zerada (o serviço vive no mesmo processo, então um
  processo novo significa silêncio; sem isso, o estado ficava órfão depois de
  reinstalar ou de o sistema matar o app, e o primeiro toque "desligava" nada).

### Armadilhas encontradas nos testes

- **Glance recompõe, não refaz.** A sessão do widget fica aberta por ~45 s e
  `updateAll` só recompõe. Estado lido fora do `provideContent` fica congelado e o
  widget mostra sempre a atualização anterior. Solução: `collectAsState()` dentro do
  conteúdo.
- **`fillMaxSize` depois de `defaultWeight`** anula o peso: a primeira tecla ocupava a
  linha inteira. Usar `defaultWeight().fillMaxHeight()`.
- **Broadcast segura a fila.** Na versão anterior (nota curta tocada dentro do
  broadcast do botão, sem serviço) descobrimos que o Android entrega broadcasts a um
  processo um de cada vez: segurar o receiver atrasava o toque seguinte. Irrelevante
  agora que o serviço toca, mas vale lembrar se a ideia voltar.
- **`uiautomator dump` do launcher às vezes vem defasado**; conferir com as
  preferências (`run-as com.lizzardtone.app cat shared_prefs/tuning.xml`).

## Fase 1 — Prova de conceito Android (feito)

- [x] Widget tocando nota pelo toque, sem abrir o app.
- [x] Medido no S24 Ultra (Android 16, One UI 8.5): com o app vivo, 7–27 ms do toque
      ao som; com o processo morto, ~340 ms.

## Fase 2 — Áudio (feito)

- [x] Som contínuo sintetizado no lugar de WAVs.
- [x] Toggle: toca até desligar ou trocar.
- [x] Serviço em primeiro plano com notificação útil (nota + Parar).
- [x] Reafinar ao vivo quando tom ou Lá mudam.

## Fase 3 — Teclado e transposição (feito)

- [x] Widget com uma oitava (brancas e pretas), redimensionável, mínimo 4×2 células.
- [x] Seletor de oitava no widget e no app.
- [x] Tom do instrumento (Dó, Si♭, Lá, Fá, Mi♭) no widget (botão que cicla) e no app
      (chips).
- [x] Lá de referência 440/442/443 no app (o widget usa o valor escolhido).
- [x] App com o mesmo teclado em tela cheia, status da nota e ajustes.

## Fase 4 — Polimento e atalhos (próximo)

- [ ] **Timeout da nota.** Hoje a nota soa até ser desligada. Adicionar um limite
      configurável no app, com 30 s como padrão, pra não ficar tocando pra sempre se
      esquecer ligado. Ao estourar, o serviço para como se tivesse tocado em Parar
      (zera o estado, tira o destaque da tecla, remove a notificação). Opção "sem
      limite" pra quem quiser. Guardar em `TuningSettings` como os outros ajustes.
- [ ] **Lá 444 Hz** na lista de referência (hoje 440/442/443).
- [ ] Teste de ouvido por alguns dias: timbre, volume, se o som some quando o Samsung
      congela apps em segundo plano, se a notificação incomoda.
- [ ] Ícone do app e do widget (hoje é o ícone padrão).
- [ ] Widget em tamanho menor (só brancas, ou só a oitava mais usada).
- [ ] Tile de configurações rápidas (Quick Settings) que toca o Lá ou abre o teclado.
- [ ] Atalho de app (long-press no ícone) direto pra nota mais usada.
- [ ] Mais tons se precisar (Sol, Ré) e Lá 415 barroco.
- [ ] Timbre alternativo (mais suave para voz, mais brilhante para metais).

## Fase 5 — iOS (quando tiver Mac à mão)

- [ ] Prova de conceito isolada: widget WidgetKit com um botão (App Intent) ligando um
      tom contínuo com o app em segundo plano. Se não soar de forma confiável, o
      fallback é o botão abrir o app já tocando a nota (um toque a mais, ainda rápido).
- [ ] Portar o desenho (teclado, toggle, tom do instrumento, oitava, Lá) em SwiftUI.
- [ ] Instalação: Xcode + cabo, reassinar a cada 7 dias ou pagar a conta de desenvolvedor.

## Fora de escopo

- Publicação em loja (Play ou App Store).
- Afinador por microfone (ouvir a nota tocada). Pode virar fase futura, mas é outro app.
- Suporte a Android abaixo do 8.0 (API 26).

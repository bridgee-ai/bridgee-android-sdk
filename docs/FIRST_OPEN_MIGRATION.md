# firstOpen: API preservada, entrega analytics alterada

Alvos candidatos: Android 3.0.0, iOS 2.0.0 e React Native 0.2.0. Esta nota não confirma publicação nem atualização de aplicativos clientes.

- `firstOpen`, MatchBundle, retorno UTM e callback/completion/Promise existentes continuam disponíveis. Não substituir chamadas do app por um evento manual Firebase.
- Bridgee deixa deliberadamente de entregar `first_open` e `<tenant>_first_open` ao AnalyticsProvider. Firebase é o responsável por `first_open`; a integração de ecommerce existente continua responsável por `purchase`, sem duplicação pela Bridgee.
- Permanecem `campaign_details`, `<tenant>_campaign_details` e as propriedades `install_source`, `install_medium` e `install_campaign`. Hífens do tenant são normalizados para sublinhados nos nomes dos eventos. Os parâmetros de campanha conservam `utm_source`, `utm_medium`, `utm_campaign`, `source`, `medium` e `campaign`.
- RN novo filtra os dois nomes first_open mesmo quando ligado aos SDKs nativos antigos. Isso não atualiza os binários nativos e não promete outras funcionalidades novas dos nativos. Eventos não-alvo continuam passando pelo provider.
- `dryRun` preserva a resolução e o resultado, sem entrega analytics nativa. Não é um modo offline: pode consultar a API. Os testes desta mudança usam somente doubles locais, sem essas chamadas.

## Antes do rollout

Inventariar relatórios, funis, audiências, conversões e exports que dependam de `<tenant>_first_open` ou de parâmetros Bridgee no `first_open`. Esses eventos customizados deixam de chegar nas versões novas; não interpretar a queda como perda de instalações. Usar o `first_open` nativo Firebase para a métrica de primeira abertura, registrando a mudança de definição/data/versão. O `<tenant>_first_open` antigo era emitido após match, não comprovava instalação. A primeira abertura nativa pode representar outra população e outra contagem; tratar a transição como quebra de série/critério, sem equivalência histórica automática ou backfill fabricado. Não somar `campaign_details` e sua variante do tenant: são duas entregas da mesma campanha.

Um retorno de match ou uma entrega de campanha não é um registro idempotente de instalação. Chamadas repetidas de `firstOpen` podem repetir resolução e campanha; não há garantia de exactly-once. As propriedades `install_*` não são identidade de instalação. Não usar essa contagem para faturamento. Uma futura métrica Bridgee de instalações exige contrato e persistência próprios, não a renomeação de matches.

## Validação de compatibilidade

No repositório React Native, o teste `tests/first-open-contract.cjs` verifica com ponte local simulada: configuração, MatchBundle com caracteres especiais, retorno e erro sem substituição, resultado vazio, parâmetros/propriedades do provider, exceções do provider, dryRun encaminhado, chamadas repetidas, reconfiguração e filtro seletivo com tenant contendo hífen. Ele executa a fonte candidata e, opcionalmente, o CommonJS efetivamente distribuído no npm antigo:

```sh
node --test tests/first-open-contract.cjs
BRIDGEE_LEGACY_SDK=/caminho/pacote-0.1.0-extraido/package node --test tests/first-open-contract.cjs
```

No segundo comando a expectativa registra os quatro eventos legados; no primeiro, apenas os dois eventos de campanha. A ponte simulada não comprova HTTP, comportamento nativo, instalação pela loja ou Firebase/GA4. A comparação de fonte iOS 1.1.1 e bytecode Android 2.3.0 confirma os quatro eventos legados; os testes nativos de entrega candidatos verificam a retirada dos dois first_open. Assinaturas preservadas não significam comportamento analytics idêntico.

Para homologar um app, registrar versões/lockfiles, testar callback/payload antes e depois, e validar Firebase/GA4 separadamente em build de homologação. Sem versão, lockfile, build e logs do cliente, não é possível concluir que ocorreu regressão nele. Publicar o SDK não altera um app já instalado.

## Versionamento e dependências

A mudança na entrega analytics é incompatível para consumidores desses eventos. Android usa major 3.0.0 e iOS major 2.0.0. Em particular, o intervalo CocoaPods `~> 1.1` aceita 1.2.0, mas não 2.0.0: manter a nova política fora de 1.x evita uma atualização indireta pelo RN antigo. React Native passa de 0.1.0 para 0.2.0.

O RN candidato ainda fixa Android 2.3.0 e iOS `~> 1.1`. Isso é temporário até a publicação aprovada dos nativos; não apresentar esse pacote intermediário como upgrade nativo completo. A atualização futura dos pins está documentada em `docs/NATIVE_PINS_AFTER_PUBLICATION.md` do repositório React Native, sem aplicação nesta etapa.

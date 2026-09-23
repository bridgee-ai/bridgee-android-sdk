# Measurement Lab → bridgee-android-sdk

Base inspecionada: `aa0d78b64ccb671bac95d1bc115f95d2906082d3` (main em 22/09/2026).

[Mapa completo, origem e dependências](https://github.com/bridgee-ai/bridgee-measurement-lab/blob/codex/lab-migration-plan/docs/MIGRACAO_BRIDGEE_AI.md).

## Delta desta rodada

`firstOpen` continua resolvendo Install Referrer e `/match` e mantém seus callbacks.
A entrega analytics deixa de emitir `first_open` e `<tenant>_first_open`; Firebase
continua dono desse evento. `campaign_details`, variante do tenant, propriedades
install_source/medium/campaign e dryRun continuam existentes. Testes de entrega
usam Robolectric, sem rede. Nenhuma versão Maven foi publicada ou alterada.

## Reaproveitar

`InstallReferrerResolver` **já existe**. `MatchBundle.withCustomParam` já transporta
campos opacos, sem necessidade de criar mais um referrer adapter nem um SDK paralelo.
Manter AnalyticsProvider sem dependência Firebase, assinatura firstOpen e MatchResponse.

## Dependências e próximos deltas

- API-01/02 do mapa: contrato de instalação/decisão opcional, consentimento, IDs e
  ações de entrega antes de definir propriedades novas no app.
- Resolver referrer por chave/valor, sem lowercase e sem decodificar delimitadores
  antes de separar pares. Diferenciar erro do resolver de instalação orgânica.
- Guardar estado de entrega por tenant/app/instalação; definir reinstall/reattribution.
  Não usar apenas flag global de singleton como dedupe de instalação.
- Preservar atribuição Google nativa conforme decisão/consentimento validado;
  mera presença de string gclid não comprova entrega Firebase válida.
- Setar bridgee_install_id somente quando servidor o produzir com consentimento.
  O app fornece links e consentimento; não colocar segredo de HMAC no pacote.
- Qualidade usa BigQuery por padrão; checkpoint opcional depois do receptor API.
  Não coletar ou emitir purchase, nem interceptar outros eventos Firebase.
- Redigir logs de referrer/payload; expiração, offline e revogação precisam de testes.

## Release/aceite

A retirada de `<tenant>_first_open` muda observabilidade de clientes: registrar na
nota de release e revisar consumidores. Atualizar VERSION_NAME/VERSION_CODE,
create-bundle.sh, README, RN e exemplo somente ao publicar a versão aprovada.
CI testa/compila, não publica. Validar Orbi e Play Install Referrer num app real;
Robolectric não comprova Firebase/Google Ads/Play Store homologados.

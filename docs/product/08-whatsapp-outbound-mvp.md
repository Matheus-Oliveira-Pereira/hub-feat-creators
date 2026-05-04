# PRD-008: WhatsApp Outbound — MVP

## Context

Brasil = WhatsApp dominante em comunicação B2B leve. Marca/influenciador respondem WA muito mais rápido que e-mail. Sem WA, follow-up sofre. Decisão: API oficial Meta (Cloud API) — ADR-006 — não soluções não-oficiais.
Vision: Fase 2 — comunicação ampliada.
Depende de: PRDs 01, 02, 04 (paralelo padrão template + opt-out).
Lead time crítico: aprovação Meta de templates HSM 24-48h → começar cedo.

## Objective

Permitir envio de mensagens WhatsApp 1-to-1 a partir de prospecção/contato usando templates HSM aprovados e mensagens free-form dentro da janela de 24h, com tracking de status (sent/delivered/read/failed) e opt-in obrigatório.

## Scope

### Includes
- [ ] **CRUD WhatsApp Business Account** (`whatsapp_account`): waba_id, phone_number_id, display_name, access_token cifrado AES-GCM, status
- [ ] **Cadastro de templates HSM**: nome, idioma, categoria (`MARKETING|UTILITY|AUTHENTICATION`), corpo com variáveis, status sincronizado com Meta (`PENDING|APPROVED|REJECTED`)
- [ ] **Submeter template** à aprovação Meta via API + polling de status (15min)
- [ ] **Envio template** a partir de prospecção/contato — modal compor com select template + preenche variáveis
- [ ] **Envio free-form** quando contato respondeu nas últimas 24h (janela de atendimento)
- [ ] **Detecção de janela 24h**: ao receber webhook de `messages` (inbound), abre janela; após 24h sem inbound, força template
- [ ] **Webhook receiver** Meta: `messages.statuses` (sent/delivered/read/failed) atualiza envio; `messages` (inbound) registra resposta na timeline
- [ ] **Verificação webhook**: validar `X-Hub-Signature-256` HMAC SHA256 com `app_secret`
- [ ] **Opt-in obrigatório**: contato precisa flag `whatsapp_optin = true` (com prova em `consentimentos` PRD-007); sem opt-in → bloqueio
- [ ] **Opt-out**: usuário envia "PARAR"/"SAIR" → registrar `whatsapp_optout`, bloquear envios
- [ ] **Histórico** na timeline da prospecção/contato (paralelo a e-mail)
- [ ] **Anexos**: imagem (JPG/PNG ≤ 5MB), PDF (≤ 100MB), áudio (OGG ≤ 16MB) — upload via Meta media API
- [ ] **Rate limit por número**: tier Meta inicial 1k msgs/dia → contador + circuit breaker
- [ ] **Idempotência**: chave por envio (UUID v7); retry da fila não duplica
- [ ] **Multi-número**: assessoria pode cadastrar > 1 número; envio escolhe número

### Excludes
- [ ] Bot conversacional / chatbot — Fase 3
- [ ] Bulk broadcast — exige consentimento + lista validada; PRD futuro
- [ ] Voice/video calls — fora escopo
- [ ] Catalog/Commerce features — fora escopo
- [ ] Inbox compartilhado tipo helpdesk — Fase 3 (visualizar conversa basta no MVP)

## Not Doing

- **Bulk** — risco compliance + ban de número Meta. Só 1-to-1.
- **Bot/IA respondendo** — escopo IA é Fase 3 (PRD-016).
- **Provedores não-oficiais** (Z-API, Twilio sandbox) — ADR-006 documentou: bloqueio Meta + risco bani.

## User Stories

- Como assessora, quero enviar mensagem WA para contato a partir da prospecção
- Como assessora, quero saber se mensagem foi lida
- Como OWNER, quero submeter template e acompanhar aprovação Meta
- Como contato, quero parar de receber respondendo "SAIR"

## Acceptance Criteria

- [ ] **AC-1**: Cadastro WABA testa token via `GET /me` Meta; falha retorna erro classificado
- [ ] **AC-2**: Submissão template chama `POST /{waba_id}/message_templates`; status sincronizado a cada 15min até `APPROVED|REJECTED`
- [ ] **AC-3**: Envio template usa `POST /{phone_id}/messages` com `template.components`; resposta retorna `wamid` salvo
- [ ] **AC-4**: Free-form bloqueado fora janela 24h → 422 `JANELA_FECHADA` com sugestão de template
- [ ] **AC-5**: Webhook signature inválida → 401; válida atualiza envio em ≤ 2s
- [ ] **AC-6**: Inbound `STOP|SAIR|PARAR` (case-insensitive) registra opt-out + envia confirmação template
- [ ] **AC-7**: Envio sem `whatsapp_optin` no contato → 422 `SEM_OPTIN`
- [ ] **AC-8**: Idempotência: dois POSTs mesma chave → segundo retorna mesmo `id` sem chamar Meta
- [ ] **AC-9**: Cross-tenant: tentar usar WABA de outra assessoria → 404
- [ ] **AC-10**: Rate limit estoura → 429 `Retry-After`; fila respeita
- [ ] **AC-11**: Anexo > limite → 422 antes de subir para Meta
- [ ] **AC-NF-1**: Token Meta cifrado AES-GCM em repouso; nunca em log
- [ ] **AC-NF-2**: Latência envio (request → enfileirado) < 200ms p95
- [ ] **AC-NF-3**: Cobertura ≥ 80% em `WhatsappService`, webhook handlers
- [ ] **AC-NF-4**: Webhook idempotência: replay de mesmo evento Meta (mesmo `id`) não duplica eventos

## Technical Decisions

- **Cliente**: HTTP cru via `RestClient` Spring (sem SDK Meta oficial Java); endpoints `graph.facebook.com/v20.0`
- **Cifra**: AES-GCM com `app.secrets.whatsapp-key` (separada de e-mail)
- **Fila**: tabela `job` com `tipo=WHATSAPP_SEND` (ADR-010)
- **Webhook URL**: `/api/v1/whatsapp/webhook` — pública (signature verification); rate limit por IP
- **Janela 24h**: cache `(assessoria_id, contact_e164)` com TTL = `last_inbound_at + 24h`
- **Reuso de timeline**: módulo `historico-unificado` (PRD-010)

### Schema

```sql
whatsapp_accounts (id, assessoria_id, waba_id, phone_number_id, phone_e164, display_name,
  access_token_enc, token_nonce, app_secret_enc, app_secret_nonce, status, created_at, deleted_at,
  UNIQUE (assessoria_id, phone_number_id))

whatsapp_templates (id, assessoria_id, account_id, nome, idioma, categoria, corpo, variaveis TEXT[],
  status, meta_template_id, motivo_rejeicao, submetido_em, atualizado_em,
  UNIQUE (account_id, nome, idioma))

whatsapp_envios (id, assessoria_id, account_id, template_id NULL, destinatario_e164, tipo CHECK ('TEMPLATE','FREEFORM','MIDIA'),
  payload JSONB, idempotency_key UNIQUE, status CHECK ('ENFILEIRADO','ENVIADO','ENTREGUE','LIDO','FALHOU'),
  wamid TEXT NULL, contexto JSONB, autor_id, created_at, sent_at, delivered_at, read_at, failed_at, falha_motivo)

whatsapp_eventos_inbound (id, assessoria_id, account_id, from_e164, wamid, tipo CHECK ('TEXT','MEDIA','REACTION','STATUS'),
  payload JSONB, processado_em, created_at)

whatsapp_optouts (id, assessoria_id, e164 CITEXT, motivo, created_at, UNIQUE (assessoria_id, e164))

whatsapp_window_cache (assessoria_id, e164, last_inbound_at, PRIMARY KEY (assessoria_id, e164))
```

## Impact on Specs

- **Compliance**: opt-in obrigatório, `consentimentos` (PRD-007) registra prova; opt-out perpétuo por (tenant, e164)
- **Security**: cifra de token, validação HMAC, rate limit em webhook
- **Scalability**: webhook recebe alta cardinalidade — particionar `whatsapp_eventos_inbound` por mês
- **Observability**: `whatsapp_enviado_total{status}`, `whatsapp_template_aprovado_total`, `whatsapp_webhook_recebido_total`, `whatsapp_janela_fechada_total`
- **API**: `/api/v1/whatsapp/{accounts,templates,envios,webhook}`
- **Testing**: mock servidor Meta (WireMock) em ITs; teste de signature verification

## Rollout

- **Feature flag**: `feature.whatsapp.enabled` por tenant
- **Migrations**: V0008_*
- **Rollback**: flag off; tokens cifrados ficam em repouso
- **Onboarding**: runbook "Como criar WABA e obter access token" (Meta Business Manager)
- **Monitoramento**: alerta envios falhados > 10% em 1h; alerta token inválido (auth fail)

## Métricas

- ≥ 50% assessorias com WhatsApp configurado em 60d
- Tempo template submetido → aprovado mediana < 24h
- Read rate ≥ 70% (canal de alto engajamento)
- Zero ban de número (proxy: zero status `RESTRICTED` Meta)

## Open questions

- [ ] On-Behalf-Of (Embedded Signup) ou cliente cadastra WABA próprio? — MVP: WABA próprio (menos fricção legal HUB)
- [ ] Templates compartilhados entre tenants? — não, isolado por WABA
- [ ] Reagir a "MENU" ou comandos especiais? — não no MVP

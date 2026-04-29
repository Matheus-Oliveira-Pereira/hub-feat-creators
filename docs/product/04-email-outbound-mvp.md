# PRD-004: E-mail Outbound multi-conta — MVP

## Context

Prospecção (PRD-002) só tem valor se a assessora **conversa com a marca**. Hoje, comunicação acontece em e-mail pessoal externo, fora do sistema → histórico fragmentado, sem tracking, sem template, sem padronização visual.

Decisão de produto (ADR-005): assessoria envia do **e-mail dela** via SMTP relay externo (Gmail/Outlook/M365/custom), não do nosso domínio. Isso:
- preserva relacionamento já existente (marca conhece o e-mail da assessora)
- evita problemas de deliverability/reputação no nosso domínio multi-tenant
- transfere ônus de quota/policy para o provedor da assessora

Vision: `docs/product/vision.md` — Fase 1.
Depende de: [[01-cadastros-mvp]] (contato, marca), [[02-prospeccao-mvp]] (vínculo de envio).
ADR técnico: [[adr-005-email-smtp-multi-conta]].

## Objective

Permitir que assessoria cadastre múltiplas contas SMTP, crie templates HTML reutilizáveis com variáveis, e envie e-mails 1-to-1 a partir de prospecções/contatos com tracking de abertura/clique e histórico unificado — preparando base para sequências automatizadas em PRD futuro.

## Scope

### Includes
- [ ] CRUD de **conta SMTP** (`email_account`): host, port, username, senha (cifrada AES-GCM), from_address, from_name, tls_mode (`STARTTLS|SSL`), daily_quota, status (`ATIVA|PAUSADA|FALHA_AUTH`)
- [ ] Botão "Testar conexão" — handshake SMTP + AUTH sem enviar; reportar erro legível
- [ ] CRUD de **template** (`email_template`): nome, assunto, corpo HTML, corpo texto-plano (auto-gerado se não fornecido), variáveis declaradas
- [ ] **Wrapper de layout** por assessoria (`email_layout`): header HTML, footer HTML (logo, dados de contato, link unsubscribe). Aplicado automaticamente ao corpo do template no envio
- [ ] **Sistema de variáveis** Mustache-like: `{{contato.nome}}`, `{{marca.nome}}`, `{{influenciador.nome}}`, `{{prospeccao.titulo}}`, `{{assessor.nome}}`, `{{assessor.email}}`, `{{data}}`, custom — render server-side, sem execução de código
- [ ] **Editor de template** no front: tabs "HTML | Variáveis | Preview"; preview com dados-mock ou de uma entidade real
- [ ] **Envio 1-to-1** a partir de:
  - Detalhe de prospecção → escolher template + conta + destinatário (contato da marca pré-preenchido)
  - Detalhe de contato → mesmo fluxo
- [ ] **Histórico** de envios na timeline da prospecção/contato (autor, conta usada, template, destinatário, status, timestamps)
- [ ] **Tracking** opcional (toggle por envio):
  - Abertura: pixel transparente 1x1 hospedado em `apps/api`
  - Clique: links reescritos para `/api/v1/email/click/{token}` → 302 para original
- [ ] **Webhook de bounce/complaint** — receber e processar bounces (parsing de retorno SMTP + ARF se disponível); marcar contato como `email_invalido` após hard bounce
- [ ] **Idempotência**: chave `idempotency_key` (UUID v7) por envio; retry de fila não duplica
- [ ] **Fila de envio** assíncrona (Postgres-backed jobs no MVP; Redis/SQS em escala) — request HTTP retorna `202 Accepted` + ID
- [ ] **Connection pool** por conta SMTP (Jakarta Mail) com circuit breaker em falhas de auth (3 falhas em 10min → conta marcada `FALHA_AUTH`, alerta para OWNER)
- [ ] **Rate limit** por conta: respeitar `daily_quota` cadastrada (default Gmail 500, Outlook 300, M365 10000); 429 quando estourar
- [ ] **Header `List-Unsubscribe`** (RFC 8058) obrigatório em todo envio + link visível no footer (LGPD)
- [ ] **Opt-out store**: tabela de e-mails que pediram descadastro — bloqueia envio futuro independente de conta/assessoria? **Decisão**: bloqueio por (`assessoria_id` + `email`) — opt-out de uma assessoria não afeta outra
- [ ] **Anexos**: até 3 arquivos por envio, tamanho total ≤ 10MB; armazenados em object storage (Railway volume / S3 futuro)

### Excludes
- [ ] Sequências automáticas / drip campaigns — PRD futuro (depende de motor de regras)
- [ ] Envio em massa (bulk) — só 1-to-1 no MVP; bulk pede compliance LGPD adicional (consentimento explícito por destinatário)
- [ ] Editor WYSIWYG visual (drag-and-drop blocks) — usar HTML cru + preview no MVP; reavaliar após adoção
- [ ] A/B testing de subject — Fase 3
- [ ] Integração com IMAP (sincronizar respostas para a thread) — Fase 2
- [ ] Sugestão de horário ótimo de envio — Fase 3
- [ ] DKIM/SPF/DMARC config — responsabilidade da assessoria no domínio dela; nosso sistema só envia via SMTP autenticado
- [ ] Provedor transacional (Resend/SES/Postmark) — descartado em ADR-005

## Not Doing (and why)

- **Bulk send no MVP** — exige fluxo de consentimento, pré-validação de lista, controle de unsubscribe agregado. Risco LGPD alto e adoção MVP não exige.
- **Editor WYSIWYG visual** — assessoras técnicas o suficiente para HTML simples; investimento de tempo desproporcional ao valor. Reavaliar com data de uso real.
- **Sincronização IMAP de respostas** — complexidade alta (parsing de threads, conflito de leitura, OAuth IMAP). Resposta do destinatário continua na inbox da assessora; vínculo via `In-Reply-To` adia para Fase 2.
- **Sugestão de horário** — exige histórico estatístico inexistente no MVP.
- **Provedor transacional** — ADR-005 documentou; envio via conta da própria assessoria preserva relacionamento + reputação.

## User Stories

- **Como** OWNER, **quero** cadastrar a conta SMTP da assessoria (Gmail/Outlook) **para** enviar do nosso e-mail real
- **Como** OWNER, **quero** testar conexão antes de salvar **para** detectar senha errada na hora
- **Como** assessora, **quero** criar template com placeholders **para** padronizar pitch sem reescrever
- **Como** assessora, **quero** ver preview do e-mail com dados reais **para** validar antes de enviar
- **Como** assessora, **quero** enviar e-mail direto da prospecção com 2 cliques **para** não perder contexto trocando de aba
- **Como** assessora, **quero** ver no histórico se o destinatário abriu **para** decidir hora do follow-up
- **Como** assessora, **quero** anexar contrato/proposta **para** não usar Drive externo
- **Como** dona da assessoria, **quero** ser alertada quando conta SMTP falhar **para** trocar senha rapidamente
- **Como** destinatário (marca), **quero** clicar em "descadastrar" **para** parar de receber (LGPD)

## Design

- **Flow A — Claude Design**. Tokens em `docs/specs/design-system/`.
- Telas:
  - **Configurações > E-mail** (lista de contas, botão "Adicionar conta", modal de teste)
  - **Configurações > Templates** (lista, botão criar, editor com tabs HTML/Variáveis/Preview)
  - **Configurações > Layout** (single form: header HTML, footer HTML, preview combinado com template selecionado)
  - **Compor e-mail** (modal a partir de prospecção/contato): selects de conta + template + destinatário; campo de override de assunto/corpo; toggle tracking; anexos
  - **Histórico de envios** (tab no detalhe da prospecção/contato — eventos com status visual: enviado / aberto / clicado / falhou / bounced)

## Acceptance Criteria

### Funcional
- [ ] **AC-1**: Cadastro de conta cifra `password` em repouso com AES-GCM; senha nunca retorna em GET (campo write-only)
- [ ] **AC-2**: "Testar conexão" abre socket SMTP, autentica, fecha sem enviar mensagem; retorna `OK` ou erro classificado (`AUTH_FAILED`,`CONN_REFUSED`,`TLS_ERROR`,`TIMEOUT`)
- [ ] **AC-3**: Template renderiza variáveis declaradas; variável não declarada usada no corpo levanta erro de validação ao salvar
- [ ] **AC-4**: Variável referenciada em entidade nula (ex: `{{influenciador.nome}}` quando prospecção sem influenciador) renderiza como string vazia, não quebra envio
- [ ] **AC-5**: Wrapper de layout aplicado: corpo final = `{{layout.header}} + {{template.body}} + {{layout.footer}}`. Footer contém link unsubscribe automático
- [ ] **AC-6**: Envio retorna 202 com `id` e `status=ENFILEIRADO`; processamento async escreve `EmailEnvio` com status `ENVIADO|FALHOU` + `smtp_message_id`
- [ ] **AC-7**: Idempotência: dois POSTs com mesma `idempotency_key` → segundo retorna o mesmo `id` sem reenviar
- [ ] **AC-8**: Tracking de abertura: pixel registra `EmailEvento{tipo=ABERTO}` na primeira requisição; subsequentes registram mas não duplicam contagem única
- [ ] **AC-9**: Tracking de clique: link reescrito redireciona com 302 e registra evento; tempo médio de redirect < 100ms
- [ ] **AC-10**: Hard bounce → contato marcado `email_invalido=true`; tentativa de novo envio para esse contato pela mesma assessoria retorna 422
- [ ] **AC-11**: Opt-out: GET `/email/unsubscribe/{token}` exibe página de confirmação; POST registra opt-out e bloqueia envios futuros para `(assessoria_id, email)`
- [ ] **AC-12**: Header `List-Unsubscribe` presente em todo envio com URL one-click (RFC 8058)
- [ ] **AC-13**: Conta com 3 falhas de auth em 10min → status `FALHA_AUTH`; tentativas de envio enquanto `FALHA_AUTH` retornam 422 com mensagem clara
- [ ] **AC-14**: `daily_quota` respeitada: contagem reseta às 00:00 timezone da assessoria; estourar quota retorna 429 com `Retry-After`
- [ ] **AC-15**: Cross-tenant: não é possível enviar usando conta de outra assessoria; tentativa retorna 404
- [ ] **AC-16**: Anexos: rejeita > 3 ou > 10MB total com 422; armazenamento serve link assinado válido por 7 dias

### Não-funcional
- [ ] **AC-NF-1**: Senha SMTP nunca aparece em log (massagear logger; teste verifica grep no output)
- [ ] **AC-NF-2**: Throughput: ≥ 50 envios/min por conta SMTP (limite real do provedor é o teto)
- [ ] **AC-NF-3**: Fila tem retry exponencial (1m, 5m, 30m, 2h) com 4 tentativas; após esgotar, status `FALHOU` definitivo
- [ ] **AC-NF-4**: WCAG 2.1 AA no editor (HTML editor com label, contraste); página de unsubscribe acessível
- [ ] **AC-NF-5**: Cobertura ≥ 80% nos services de envio + cifra; teste de cifra usa chave estática de teste
- [ ] **AC-NF-6**: Template HTML é sanitizado contra XSS no preview (DOMPurify ou equivalente server-side com OWASP Java HTML Sanitizer)

## Technical Decisions

- **Reusa**:
  - [[adr-001-monorepo]], [[adr-002-stack-base]], [[adr-003-flyway-migrations]]
  - [[adr-005-email-smtp-multi-conta]] (decisão SMTP relay externo + AES-GCM)
  - [[adr-008-auth-jwt]] (auth), [[adr-009-multi-tenant-strategy]] (multi-tenant strict)
  - [[adr-010-async-jobs-postgres-queue]] (fila de envio + retry exponencial + idempotência)
  - [[adr-011-lgpd-baseline]] (opt-out por (assessoria, email), retenção 5y `email_envios`, base legal "legítimo interesse" + opt-out, notificação de tratamento)
  - [[adr-012-attachment-storage]] (anexos via abstração `AttachmentStorage`)
  - [[adr-013-observability-stack]] (métricas `email_*`, alerta auth falha, traces)
  - PRDs 001/002 (entidades vinculadas)
- **Cifra de senha SMTP**: AES-GCM com chave do `app.secrets.email-key` (env). Rotação documentada em runbook
- **Engine de template**: Mustache (Java `mustache.java`) — simples, sem execução de código, alinhado a Handlebars-like
- **Sanitização**: OWASP Java HTML Sanitizer no corpo do template (allowlist de tags/atributos seguros para e-mail: `a`, `img`, `table`, etc.; remove `script`, `on*`)
- **Fila + retry**: tabela `job` com `tipo=EMAIL_SEND` (ADR-010); idempotência via UNIQUE(`assessoria_id`, `tipo`, `idempotency_key`)
- **Storage de anexos**: driver `LocalVolumeAttachmentStorage` no MVP, troca p/ S3 sem refactor (ADR-012)

### Schema

```sql
email_accounts (
  id UUID PK,
  assessoria_id UUID FK,
  nome TEXT NOT NULL,                       -- "Equipe Comercial"
  host TEXT NOT NULL,
  port INT NOT NULL,
  username TEXT NOT NULL,
  password_encrypted BYTEA NOT NULL,        -- AES-GCM
  password_nonce BYTEA NOT NULL,
  from_address CITEXT NOT NULL,
  from_name TEXT NOT NULL,
  tls_mode TEXT NOT NULL CHECK (tls_mode IN ('STARTTLS','SSL')),
  daily_quota INT NOT NULL DEFAULT 500,
  status TEXT NOT NULL DEFAULT 'ATIVA' CHECK (status IN ('ATIVA','PAUSADA','FALHA_AUTH')),
  ultima_falha_em TIMESTAMPTZ NULL,
  created_at, updated_at, deleted_at,
  UNIQUE (assessoria_id, from_address)
)

email_layouts (
  id UUID PK,
  assessoria_id UUID FK UNIQUE,             -- 1 layout por assessoria no MVP
  header_html TEXT NOT NULL,
  footer_html TEXT NOT NULL,                -- inclui {{unsubscribe_url}}
  updated_at
)

email_templates (
  id UUID PK,
  assessoria_id UUID FK,
  nome TEXT NOT NULL,
  assunto TEXT NOT NULL,
  corpo_html TEXT NOT NULL,
  corpo_texto TEXT NULL,                    -- auto-gerado se null
  variaveis_declaradas TEXT[] NOT NULL DEFAULT '{}',
  created_at, updated_at, deleted_at
)

email_envios (
  id UUID PK,
  assessoria_id UUID FK,
  account_id UUID FK → email_accounts(id),
  template_id UUID FK NULL,                 -- null se override sem template
  destinatario_email CITEXT NOT NULL,
  destinatario_nome TEXT NULL,
  assunto TEXT NOT NULL,
  corpo_html_renderizado TEXT NOT NULL,     -- snapshot do que foi enviado
  contexto JSONB NOT NULL,                  -- {prospeccao_id, contato_id, ...}
  idempotency_key UUID NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('ENFILEIRADO','ENVIADO','FALHOU','BOUNCED')),
  smtp_message_id TEXT NULL,
  enviado_em TIMESTAMPTZ NULL,
  falha_motivo TEXT NULL,
  tentativas INT NOT NULL DEFAULT 0,
  tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  autor_id UUID FK → usuarios(id),
  created_at,
  UNIQUE (assessoria_id, idempotency_key)
)

email_eventos (
  id UUID PK,
  envio_id UUID FK → email_envios(id),
  assessoria_id UUID FK,
  tipo TEXT NOT NULL CHECK (tipo IN ('ABERTO','CLICADO','BOUNCE','COMPLAINT','UNSUBSCRIBE')),
  payload JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

email_optouts (
  id UUID PK,
  assessoria_id UUID FK,
  email CITEXT NOT NULL,
  motivo TEXT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (assessoria_id, email)
)

email_attachments (
  id UUID PK,
  envio_id UUID FK,
  filename TEXT NOT NULL,
  content_type TEXT NOT NULL,
  size_bytes INT NOT NULL,
  storage_path TEXT NOT NULL,
  created_at
)
```

Índices:
- `(assessoria_id, status)` em `email_accounts`
- `(assessoria_id, idempotency_key)` UNIQUE em `email_envios`
- `(assessoria_id, status, created_at desc)` para histórico
- `(envio_id, tipo, created_at)` em eventos
- `(assessoria_id, email)` UNIQUE em optouts

## Impact on Specs

- **Compliance**: LGPD — base legal "legítimo interesse" para prospecção comercial; opt-out obrigatório, registrado, irreversível por (assessoria, email); retenção de envios = 5 anos (auditoria) com possibilidade de pseudonimização do destinatário sob demanda do titular
- **Security**: cifra de senha SMTP, sanitização HTML (XSS), redirect open via token (sem open redirect), rate limit em pixel de tracking (anti-abuso)
- **Scalability**: fila desacoplada do request; pool por conta; rate limit por quota; retry exponencial. Eventos de tracking são eventos de alta cardinalidade — partition por mês quando passar de 10M
- **Observability**: métricas `email_enviado_total{status}`, `email_aberto_total`, `email_clicado_total`, `email_bounce_total{tipo}`, `email_quota_estourou_total{account}`, `email_auth_falha_total{account}`. Histograma latência envio
- **Accessibility**: editor com aria-labels; preview navegável por teclado
- **API**: `/api/v1/email/{accounts,templates,layout,envios}`; webhook `/api/v1/email/track/open/{token}` e `/click/{token}`
- **Testing**: GreenMail server (test container) para integração SMTP; teste de cifra; teste de idempotência; teste de unsubscribe end-to-end

## Rollout

- **Feature flag**: `feature.email.enabled` (assessoria-level)
- **Migrations**: Flyway V_email_*; criar role Postgres com permissão de leitura/escrita nas novas tabelas
- **Rollback**: flag off bloqueia rotas e jobs; senhas cifradas permanecem (não vazam)
- **Onboarding**: seed de template-exemplo ("Apresentação inicial") e layout default na primeira ativação
- **Monitoramento pós-release**: alerta se `email_auth_falha_total` por conta > 5 em 1h (provavelmente senha trocou); alerta se `email_bounce_total` > 10% em 24h (lista pode estar suja)

## Métricas de sucesso

- ≥ 80% das assessorias ativas configuram ≥ 1 conta SMTP em 30 dias
- ≥ 60% enviam ≥ 1 e-mail via plataforma na primeira semana
- Taxa de bounce < 5% (saúde de lista)
- Open rate médio reportado (baseline para meta futura)
- Zero incidente de XSS via template (varredura SAST + audit manual no editor)
- Zero vazamento de senha SMTP em log

## Open questions

- [ ] Limite de templates por assessoria? — propor 50 no plano free; ilimitado pago
- [ ] Variáveis custom (não-derivadas de entidade): texto livre ao enviar? — sim, declarar no template como `{{custom.nome_da_var}}` e prompt no compor
- [ ] Anexos antivírus scan? — adiar; documentar como risco aceito; reavaliar se houver incidente
- [ ] Conta SMTP compartilhada entre OWNER e ASSESSOR ou exclusiva por usuário? — propor compartilhada por assessoria no MVP; "atribuir conta a usuário" como Fase 2
- [ ] Webhooks de bounce para Gmail/Outlook funcionam via parsing SMTP DSN ou exigem APIs dedicadas? — pesquisar na implementação; fallback = parsing DSN básico

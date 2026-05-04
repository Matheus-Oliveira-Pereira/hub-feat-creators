# PRD-007: Compliance LGPD — MVP

## Context

Produto trata dados pessoais (influenciadores, contatos de marca, e-mails enviados). LGPD (Lei 13.709/2018) exige base legal documentada, atendimento aos direitos do titular (art. 18), retenção limitada e registro de tratamento (art. 37). Sem essa fundação, lançamento em produção é exposição legal direta.
Vision: pré-requisito de go-live (`docs/specs/compliance/`).
Depende de: PRDs 01, 02, 03, 04, 06.

## Objective

Implementar controles técnicos e processos para conformidade LGPD: registro de tratamento, atendimento aos direitos do titular (acesso, correção, exclusão, portabilidade, oposição), políticas de retenção automatizadas, consentimento granular e auditoria.

## Scope

### Includes
- [ ] **ROPA digital** (Registro de Operações de Tratamento) — `data_processing_record` por finalidade: dados coletados, base legal, retenção, compartilhamento
- [ ] **Base legal por entidade**: enum `base_legal` em `influenciador`, `marca`, `contato`, `email_envios` (`CONSENTIMENTO|EXECUCAO_CONTRATO|LEGITIMO_INTERESSE|OBRIGACAO_LEGAL`); obrigatório no create
- [ ] **Consentimento** quando aplicável: tabela `consentimentos(id, titular_id, finalidade, dado_em, revogado_em, prova JSONB)` — prova = IP, user-agent, snapshot do texto aceito, hash do documento
- [ ] **Direitos do titular** — endpoints públicos autenticados por token de identidade:
  - **Acesso (art. 18 II)**: GET retorna JSON com todos dados sobre titular (cross-table)
  - **Correção (art. 18 III)**: PATCH com campos editáveis
  - **Exclusão / anonimização (art. 18 VI)**: anonimiza PII (nome, e-mail, telefone → hash) preservando dados estatísticos
  - **Portabilidade (art. 18 V)**: export CSV/JSON estruturado
  - **Oposição (art. 18 §2)**: opt-out de finalidade específica
- [ ] **Soft-delete + retenção**: job diário purga registros com `deleted_at` + retenção esgotada por tabela
  - `email_envios`: 5 anos (auditoria fiscal)
  - `prospeccao`: 5 anos (auditoria comercial)
  - `audit_log`: 6 meses (rotação)
  - `influenciador`/`marca`/`contato`: anonimização imediata em delete; registros de relacionamento mantidos com FK órfã
- [ ] **Pseudonimização em logs**: middleware de log mascara CPF, e-mail, telefone (`m***@***.com`)
- [ ] **DPO inbox**: e-mail dedicado `dpo@<assessoria-domain>` cadastrável; notifica sobre solicitações via DSR
- [ ] **Política de privacidade** versionada: tabela `policy_version(versao, texto, vigente_de)`; usuário re-aceita em mudança material
- [ ] **Cookie banner** (web) com categorias: essencial, analytics opt-in, marketing opt-in
- [ ] **Data Processing Agreement** (DPA) — documento entre HUB e assessoria; aceite registrado no signup
- [ ] **Sub-processadores**: lista pública (`/legal/subprocessors`) — Railway, Vercel, provedores SMTP/WhatsApp; notificação de mudança 30d antes
- [ ] **Incident response**: runbook + endpoint interno de notificação à ANPD em até 2 dias úteis

### Excludes
- [ ] Certificação ISO 27001 — fora MVP
- [ ] Encriptação at-rest customizada além de Postgres TDE/AES-GCM já implantado
- [ ] DLP automatizado em e-mails — Fase 2

## Not Doing

- **Anonimização irreversível imediata em delete**: optamos por manter `deleted_at` durante retenção legal e só purgar/anonimizar no fim — exigência fiscal/auditoria.
- **Cookies analytics ativos por padrão**: opt-in explícito (LGPD art. 8 + CNIL alinhada).

## User Stories

- Como titular (influenciador), quero baixar meus dados em formato legível
- Como titular, quero pedir exclusão e ter prova de atendimento
- Como OWNER, quero saber quais sub-processadores acessam dados da minha assessoria
- Como DPO, quero log auditável de todo acesso a dado pessoal

## Acceptance Criteria

- [ ] **AC-1**: Toda criação de `influenciador|marca|contato` exige `base_legal`; falta retorna 422
- [ ] **AC-2**: Endpoint DSR `/api/v1/dsr/{titular_token}` autentica via token enviado a e-mail do titular (validade 7d, single-use por ação)
- [ ] **AC-3**: Exclusão por DSR anonimiza em < 5min: nome→`Titular #<short_hash>`, email/telefone→`null`, registros agregados (count) preservados
- [ ] **AC-4**: Export portabilidade gera ZIP (JSON + CSV) com todos dados ligados ao titular cross-tabela; assinado e disponível por 7d
- [ ] **AC-5**: Job retenção roda diário 03:00 UTC; relatório registrado em `retention_runs(data, tabela, anonimizados, purgados)`
- [ ] **AC-6**: Logger central tem filtro `PiiMaskingFilter` que mascara CPF (regex), e-mail, telefone BR; teste unit verifica mascaramento
- [ ] **AC-7**: Audit log de acesso a PII: SELECT em `influenciador|contato|email_envios` por usuário humano grava entrada em `pii_access_log` (interceptor JPA)
- [ ] **AC-8**: Mudança material de política dispara re-aceite no próximo login; usuário não pode prosseguir sem aceitar
- [ ] **AC-9**: Cookie banner bloqueia analytics até consentimento; revogação no rodapé funcional sem reload
- [ ] **AC-10**: Solicitação DSR responde em ≤ 15 dias (SLA legal); job de alerta a 10 dias
- [ ] **AC-NF-1**: Cobertura ≥ 90% em `DsrService`, `RetentionJob`, `PiiMaskingFilter` (criticidade legal)
- [ ] **AC-NF-2**: Auditoria não-repudiável: append-only, hash chain (cada linha contém hash da anterior)

## Technical Decisions

- **Hash chain audit**: `audit_log.prev_hash` + `audit_log.hash = sha256(prev_hash || row_canonical_json)`
- **Anonimização**: função `pg_anonymize_titular(titular_id)` em transação
- **Token DSR**: JWT curto (24h) + nonce em tabela `dsr_tokens` para single-use
- **Logger**: Logback `MaskingPatternLayout`
- **Sub-processadores**: arquivo `/legal/subprocessors.md` versionado; webhook Slack em mudança

### Schema

```sql
data_processing_records (id, finalidade, base_legal, dados_coletados TEXT[], retencao_meses INT, compartilhado_com TEXT[], created_at, vigente)
consentimentos (id, assessoria_id, titular_tipo, titular_id, finalidade, dado_em, revogado_em, prova JSONB,
  UNIQUE (titular_tipo, titular_id, finalidade))
dsr_solicitacoes (id, assessoria_id, titular_tipo, titular_id, tipo CHECK IN ('ACESSO','CORRECAO','EXCLUSAO','PORTABILIDADE','OPOSICAO'),
  status, criado_em, atendido_em, resultado_path, prazo_legal_em)
dsr_tokens (id, solicitacao_id, token_hash, expires_at, used_at)
policy_versions (versao, texto, vigente_de, hash)
policy_aceites (user_id, versao, aceito_em, ip, user_agent)
pii_access_log (id, user_id, recurso, recurso_id, acao, ts)
retention_runs (id, data, tabela, anonimizados INT, purgados INT, duracao_ms)
audit_log_chain (id, prev_hash, hash, ...)  -- evolução de audit_log do PRD-006
```

## Impact on Specs

- **Compliance**: este é o spec
- **Security**: hash chain anti-tampering, token DSR single-use
- **Scalability**: retenção pode ser pesado em escala — particionar `email_envios`, `audit_log` por mês
- **Observability**: métricas `dsr_solicitacao_total{tipo}`, `retencao_purgados_total`, alerta DSR pendente > 10d
- **API**: `/api/v1/dsr/*` (público com token), `/api/v1/admin/compliance/*` (interno)
- **Testing**: testes legais críticos = ≥ 90% coverage; testar anonimização real em DB com fixtures

## Rollout

- **Feature flag**: `feature.compliance.strict` — em off, validações relaxadas (período de transição clientes legados)
- **Migrations**: V0007_*; backfill `base_legal = LEGITIMO_INTERESSE` em registros existentes com flag de revisão pendente
- **Rollback**: flag off não derruba schema; reativar exige curadoria
- **Treinamento**: runbook `runbooks/lgpd-incidente.md`, `lgpd-dsr.md`
- **Comunicação**: e-mail para todas assessorias 30d antes do cutover obrigatório

## Métricas

- 100% das entidades novas têm `base_legal`
- DSR atendida em mediana ≤ 5d (SLA legal 15d)
- Zero solicitação DSR vencida
- Zero PII em log (varredura SAST + sample manual mensal)

## Open questions

- [ ] DPO interno do HUB ou contrato externo? — externo no MVP (custo)
- [ ] Anonimização preserva métricas — ok com agregação, mas dashboards por nome quebram. Aceitar
- [ ] Logs de aplicação retenção: 6m suficiente? — sim, audit_log fica 5y

# PRD-015: Ingestão de Redes Sociais

## Context

Match IA (PRD-016) e relatórios avançados precisam de **dados objetivos** sobre creators: seguidores, engajamento médio, vertical, audiência. Hoje cadastro tem só link/handle manual. Sem ingestão automática, decisão segue subjetiva.
Vision: Fase 3.
Depende de: PRDs 01, 06, 07.

## Objective

Conectar contas Instagram, YouTube e TikTok via OAuth (com consentimento do creator) e ingerir métricas públicas em snapshots periódicos para análise e match.

## Scope

### Includes
- [ ] **OAuth Instagram Graph** (via Facebook Login + Business Account)
- [ ] **OAuth YouTube Data v3** (Google OAuth)
- [ ] **OAuth TikTok Login Kit + Display API**
- [ ] **Token storage**: access_token + refresh_token cifrados AES-GCM
- [ ] **Snapshots periódicos**: job diário coleta métricas públicas (followers, posts recentes, engagement rate)
- [ ] **Refresh tokens**: job antes de expirar; falha marca conta `TOKEN_EXPIRADO` + alerta
- [ ] **Detecção de revogação**: 401/403 da plataforma → marcar `REVOGADA` + notificar
- [ ] **Rate limit por plataforma**: respeitar quotas (IG 200/h/user, YT 10k units/dia)
- [ ] **Cache agressivo**: snapshot diário; consultas internas batem cache, não a API
- [ ] **Backfill**: na primeira conexão, busca últimos 30 posts
- [ ] **Métricas armazenadas**: followers, following, posts_total, engagement_rate_30d, top_verticals (inferido), sample posts
- [ ] **Consentimento explícito**: creator assina termo via portal (PRD-013) antes de conectar; revoga a qualquer momento
- [ ] **UI no portal**: "Conectar Instagram/YouTube/TikTok" + status conexão + última sync

### Excludes
- [ ] Scraping não-oficial — proibido (TOS + risco legal/ban)
- [ ] Métricas privadas (insights de história, demografia detalhada) sem permissão Business — Fase futura
- [ ] Comentários / DMs — fora escopo
- [ ] Twitter/X — postergado (API custosa)
- [ ] Análise de imagem/vídeo (CV) — Fase 4

## Not Doing

- **Scraping** — risco de banimento, legal e técnico. Só APIs oficiais.
- **Estimativa via 3rd party (HypeAuditor, Modash)** — fora MVP; reavaliar como complemento Fase 4.

## User Stories

- Como creator, quero conectar Instagram para a assessora ver minhas métricas
- Como assessora, quero ver follower count atualizado sem perguntar
- Como creator, quero revogar conexão a qualquer momento

## Acceptance Criteria

- [ ] **AC-1**: Fluxo OAuth completo IG/YT/TikTok com state CSRF + nonce
- [ ] **AC-2**: Tokens cifrados AES-GCM; nunca em log
- [ ] **AC-3**: Refresh token rodado 24h antes do expiry; falha → status `TOKEN_EXPIRADO`
- [ ] **AC-4**: Snapshot diário grava em `social_snapshots(plataforma, conta_id, dia, payload)`; idempotente por dia
- [ ] **AC-5**: Rate limit por plataforma respeitado via token bucket; estouro → backoff 1h
- [ ] **AC-6**: Consentimento: revoga via portal → revoga upstream + apaga tokens local + mantém snapshots históricos
- [ ] **AC-7**: Cross-tenant: snapshot de creator A não acessível via assessoria B mesmo se compartilham creator
- [ ] **AC-8**: Falha API platforma com 5xx → retry exponencial até 3x; após esgotar, snapshot pulado e alerta
- [ ] **AC-NF-1**: Latência snapshot por creator < 30s (90 creators/30min worker)
- [ ] **AC-NF-2**: Cobertura ≥ 75% em `IngestionService`, OAuth handlers
- [ ] **AC-NF-3**: PII: snapshot sem dados sensíveis (sem e-mail privado do creator); só dados que creator escolheu público

## Technical Decisions

- **Cliente HTTP**: `RestClient` Spring + retry; clientes por plataforma com módulos isolados
- **Storage tokens**: tabela `social_accounts` com cifra AES-GCM (chave separada `app.secrets.social-key`)
- **Worker**: job `tipo=SOCIAL_SYNC` (ADR-010); chunk de 50 contas por execução
- **Quota tracker**: tabela `quota_uso(plataforma, dia, units_usados)` por API key/app
- **Engagement rate**: `(likes + comentários) / followers` em últimos N posts (heurística simples MVP)

### Schema

```sql
social_accounts (id, assessoria_id, influenciador_id FK, plataforma CHECK ('INSTAGRAM','YOUTUBE','TIKTOK'),
  external_user_id TEXT, handle TEXT, access_token_enc, refresh_token_enc, token_nonce, expires_at,
  status CHECK ('ATIVA','TOKEN_EXPIRADO','REVOGADA','ERRO'), conectado_em, ultimo_sync_em,
  UNIQUE (plataforma, external_user_id))

social_snapshots (id, social_account_id FK, dia DATE, followers INT, following INT, posts_total INT,
  engagement_rate NUMERIC, payload_full JSONB, created_at,
  UNIQUE (social_account_id, dia))

social_posts (id, social_account_id FK, external_post_id, posted_at, tipo, caption TEXT, likes INT,
  comments INT, shares INT, views INT, url TEXT, payload_full JSONB,
  UNIQUE (social_account_id, external_post_id))

social_consentimentos (id, influenciador_id, plataforma, dado_em, revogado_em, prova JSONB)

quota_uso (plataforma TEXT, dia DATE, units_usados BIGINT, PRIMARY KEY (plataforma, dia))
```

## Impact on Specs

- **Compliance**: consentimento via PRD-007 explícito; revogação → DSR exclusão dos snapshots se solicitado
- **Security**: cifra de tokens, CSRF state OAuth, validação `redirect_uri` registrada
- **Scalability**: snapshots particionados por mês quando cresce; quota global por app
- **Observability**: `social_sync_total{plataforma,status}`, `social_quota_uso`, alerta token expirado > 5
- **API**: `/api/v1/social/{auth/{plataforma}/start,callback,disconnect}`, `/api/v1/social/snapshots/*`
- **Testing**: WireMock pelas APIs; teste de refresh; teste de revogação

## Rollout

- **Feature flag**: `feature.social.enabled`
- **App registrations**: criar app Meta, Google Cloud Project, TikTok Developer; documentar em runbook
- **Compliance review**: Meta App Review obrigatório para escopos não-básicos (24-72h)
- **Migrations**: V0015_*
- **Rollback**: flag off; tokens permanecem cifrados (válidos quando reativar)

## Métricas

- ≥ 50% creators conectam ≥ 1 plataforma em 60d
- Snapshot daily success rate ≥ 95%
- Zero token vazado em log

## Open questions

- [ ] App único multi-tenant ou app por assessoria (whitelabel)? — único MVP (review caro); whitelabel Fase 2
- [ ] Frequência snapshot: diária basta ou sub-diária? — diária MVP, evita quota burn
- [ ] Histórico de posts até onde? — 30 dias inicial; backfill estendido sob demanda

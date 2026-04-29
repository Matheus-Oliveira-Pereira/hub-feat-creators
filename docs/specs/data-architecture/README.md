# Module: Data Architecture — HUB Feat Creator

Modelagem, pipelines, governança de dados.

## Bancos em uso

| Banco | Tipo | Uso | Hosting |
|-------|------|-----|---------|
| **PostgreSQL 16** | Relacional | OLTP — todos os dados transacionais | Railway managed |
| **Redis 7** | Key-value | Cache + rate limiting + filas BullMQ-like | Railway managed (ou Upstash) |
| **pgvector** (extensão Postgres) | Vetor | Memória semântica L4 (dev local Chroma; team-shared opcional) | mesmo Postgres da app (schema separado) ou DB dedicado |

> Não há Elasticsearch no MVP. Postgres full-text search (`tsvector`) cobre busca em `influenciadores`/`marcas` por nome.

## Modelagem

- **Paradigma**: relacional normalizado (3NF) com denormalizações pontuais quando perfilagem mostrar gargalo
- **ERD**: manter em `docs/architecture/erd.md` (diagrama dbml ou Mermaid)
- **Naming**:
  - Tabelas: `snake_case`, **plural** em pt-BR (`influenciadores`, `marcas`, `prospeccoes`, `tarefas`)
  - Colunas: `snake_case` (`created_at`, `assessoria_id`)
  - PKs: `id UUID` (v7 quando possível)
  - FKs: `<entidade_singular>_id` (ex: `assessoria_id`, `influenciador_id`)
  - Índices: `idx_<tabela>_<colunas>` (ex: `idx_prospeccoes_assessoria_id_status`)
  - FK constraint: `fk_<tabela>_<ref>` (ex: `fk_tarefas_prospeccao`)
  - Unique: `uq_<tabela>_<colunas>`
- **Soft-delete**: **obrigatório** em `assessorias`, `usuarios`, `influenciadores`, `marcas`, `contatos`, `prospeccoes`, `tarefas` — coluna `deleted_at TIMESTAMPTZ NULL`
- **Timestamps**: `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` + `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` em **todas** as tabelas (trigger atualiza)
- **IDs**: **UUID v7** (preserva ordenação temporal, índice mais eficiente que v4)

## Multi-tenancy
Toda tabela de domínio carrega `assessoria_id UUID NOT NULL` + FK para `assessorias`.
- Hibernate `@Filter` injeta `WHERE assessoria_id = :tenantId` automaticamente
- Índice composto sempre começa com `assessoria_id` (cardinality alta + filtro mandatório)
- Detalhes em `docs/specs/security/#multi-tenancy`

## Schema inicial (alto nível — detalhar nos PRDs)

```
assessorias       (id, nome, slug, plano, created_at, updated_at, deleted_at)
usuarios          (id, assessoria_id, email, senha_hash, role, mfa_secret, created_at, ...)
influenciadores   (id, assessoria_id, nome, handles_redes (jsonb), nicho, audiencia_total, ...)
marcas            (id, assessoria_id, nome, segmento, site, observacoes, ...)
contatos          (id, marca_id, nome, email, telefone, cargo, ...)
prospeccoes       (id, assessoria_id, influenciador_id, marca_id, status, valor_proposto, ...)
tarefas           (id, assessoria_id, prospeccao_id (nullable), titulo, due_date, status, atribuido_a, ...)
emails_enviados   (id, assessoria_id, prospeccao_id, template, destinatario, status, idempotency_key, ...)
audit_log         (id, assessoria_id, usuario_id, entidade, entidade_id, acao, payload (jsonb), created_at)
```

> Schema detalhado vai em ADR específico + PRD-001 (cadastros).

## Convenções Postgres

- **Locale/Collation**: `pt_BR.UTF-8` (`CREATE DATABASE ... LC_COLLATE 'pt_BR.UTF-8'`)
- **Timezone**: DB em UTC (`timezone = 'UTC'` no `postgresql.conf`); aplicação converte
- **JSONB** preferido sobre JSON
- **Constraints nomeadas** (não anônimas) para diff legível em migrations
- **NOT NULL por padrão** — colunas nullable são exceção e precisam justificativa em ADR ou comment
- **Enums em código (Java)**, **string em DB** com CHECK constraint — facilita evolução sem `ALTER TYPE`
- **Money**: `NUMERIC(12,2)` (não `FLOAT`) — para `valor_proposto`, `valor_fechado`

## Migrations
- Tool: **Flyway**
- Localização: `apps/api/src/main/resources/db/migration/`
- Naming: `V<YYYYMMDDHHmm>__<descricao>.sql` (ex: `V202604291430__create_assessorias.sql`)
- **Nunca editar V já aplicada em qualquer ambiente** — criar nova migration
- Reversibilidade: `U<...>.sql` opcional para rollback dev (não usado em prod)
- Migrations destrutivas (DROP, ALTER TYPE) exigem ADR + plano de rollback
- Detalhes em `docs/specs/versioning/#migrations`

## Indexação — princípios
- Índice em toda FK
- Índice composto começando por `assessoria_id` em entidades multi-tenant
- Índice parcial para `deleted_at IS NULL` em soft-deletables (queries comuns)
- Índice para campos de filtro/ordenação frequentes (`status`, `due_date`, `created_at`)
- `EXPLAIN ANALYZE` antes de adicionar índice "porque sim"
- Documentar índices não-óbvios em ADR

## Pipelines de dados (futuro — Fase 3)
- ETL: provavelmente **dbt** sobre o próprio Postgres (read replica)
- DW: não necessário no MVP; reavaliar após 1M de eventos
- Sync: ainda não aplicável

## Streaming de eventos (futuro — Fase 2)
- Plataforma: **Postgres LISTEN/NOTIFY** no MVP; migrar para **RabbitMQ** ou **Redis Streams** quando precisar de durabilidade
- Eventos de domínio (rascunho):
  - `prospeccao.criada`
  - `prospeccao.status_alterado`
  - `tarefa.criada`
  - `tarefa.atrasada`
  - `email.enviado`
  - `influenciador.cadastrado`
- Schema registry: JSON Schema versionado em `docs/specs/data-architecture/events/`
- DLQ: sim (RabbitMQ x-dead-letter)
- Idempotência: chave única por evento (`event_id` UUID)

## Governança de dados

### Classificação
- **Pública**: nada por padrão
- **Interna**: dados agregados, métricas
- **Confidencial**: cadastros (influenciador, marca, contato), prospecções, e-mails — **maioria**
- **Restrita**: senhas (hash), tokens, MFA secrets, dados financeiros

### Owner por domínio
- Tudo no MVP: time único — owner = Tech Lead
- Após 5+ pessoas: separar (Cadastros, Comunicação, Match)

### Catálogo
- MVP: README de cada módulo + DDL anotado
- Futuro: DataHub ou similar quando dúvidas de schema crescerem

### Qualidade
- Constraints no DB (NOT NULL, CHECK, FK, UNIQUE) são primeira linha
- Validação na borda (Bean Validation no DTO) é segunda
- Testes de integração validam invariantes de negócio
- Job diário roda checks de consistência (ex: `prospeccao.status = FECHADA` ⇒ `valor_fechado IS NOT NULL`)

### Lineage
- MVP: rastreio em `audit_log` (entidade, ação, payload, autor)
- Retenção: 2 anos para audit; 5 anos para dados financeiros (futuro)

## Analytics

### Eventos de produto (Fase 2)
- Tool: **PostHog** (self-hosted ou cloud) — open source, LGPD-friendly
- Eventos core:
  - `assessoria.signup`
  - `usuario.login`
  - `prospeccao.criada`
  - `prospeccao.fechada` (com `time_to_close_days`)
  - `email.enviado`
  - `tarefa.concluida`
- Consent: opt-in obrigatório no onboarding (LGPD)
- PII em eventos: pseudonimizada (hash de email, nunca raw)

### Dashboards de produto
- WAU assessoras
- Funil prospecção (criada → enviada → respondida → fechada)
- Time-to-close
- Cohort retention de assessorias

## Retenção e LGPD

| Dado | Retenção ativa | Após exclusão | Base legal |
|------|---------------|---------------|------------|
| Cadastro influenciador | enquanto ativo | soft-delete + purge em 90d | execução de contrato |
| Cadastro marca/contato | enquanto ativo | soft-delete + purge em 90d | legítimo interesse |
| E-mails enviados | 2 anos | hard-delete | execução de contrato |
| Audit log | 2 anos | hard-delete | obrigação legal |
| Logs de aplicação | 90 dias | rotação automática | legítimo interesse |
| Backups | 30 dias incrementais + 1 ano mensais | hard-delete | continuidade |

Detalhar em `docs/specs/compliance/` quando módulo for ativado.

## Backup
- **RPO**: 24h (backup diário)
- **RTO**: 4h (restore em produção)
- **Estratégia**: Railway PITR (point-in-time recovery) últimos 7 dias + snapshot diário S3 retido 90d
- **Teste de restore**: trimestral em ambiente staging

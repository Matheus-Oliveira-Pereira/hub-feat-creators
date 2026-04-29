# PRD-001: Cadastros — MVP

## Context

Toda operação de assessoria começa com cadastros. Hoje, assessoras usam planilhas dispersas: uma para influenciadores, outra para marcas, contatos espalhados em Notion / WhatsApp. Resultado:
- Dados duplicados/desatualizados
- Difícil cruzar informações (ex: "qual marca já trabalhou com este influenciador?")
- Sem histórico estruturado

A primeira release precisa entregar **valor imediato**: substituir as planilhas básicas com um sistema multi-tenant, rápido e organizado. Sem cadastros sólidos, nenhuma feature subsequente (prospecção, e-mail, match) faz sentido.

Vision relacionada: `docs/product/vision.md` — Fase 1.

## Objective

Permitir que uma assessora cadastre, edite, busque e remova (soft) **influenciadores**, **marcas** e **contatos** dentro de um workspace isolado por assessoria, com auditoria das alterações e respeitando LGPD desde o dia 1.

## Scope

### Includes
- [ ] CRUD de **assessorias** (cadastro auto-serviço; uma assessoria = um workspace = um tenant)
- [ ] CRUD de **usuários** dentro de assessoria com roles `OWNER` e `ASSESSOR`
- [ ] Convite de novos usuários por e-mail (link com token de uso único)
- [ ] CRUD de **influenciadores**: nome, handles de redes sociais (jsonb: instagram, youtube, tiktok, twitch, twitter, outras), nicho, audiência total estimada, observações livres, tags
- [ ] CRUD de **marcas**: nome, segmento, site, observações, tags
- [ ] CRUD de **contatos** (vinculados a marca): nome, e-mail, telefone, cargo
- [ ] Listagens com paginação cursor-based, busca por nome, filtros por tag/nicho/segmento
- [ ] Soft-delete em todas entidades (`deleted_at`)
- [ ] Audit log: toda criação/edição/deleção registrada (autor, timestamp, payload)
- [ ] Visualização de detalhe com histórico de alterações (somente OWNER vê audit log completo no MVP)
- [ ] Exportação CSV das listas (LGPD direito de portabilidade)

### Excludes
- [ ] Portal do influenciador (visualização própria) — Fase 2
- [ ] Integração com APIs sociais (puxar dados auto) — Fase 3
- [ ] Match marca↔influencer — Fase 3
- [ ] Importação CSV (importar planilha existente) — Fase 1.1 (incluir se houver tempo)
- [ ] Permissões granulares por entidade (todos `ASSESSOR` veem tudo da assessoria) — Fase 2
- [ ] Anexos / arquivos (foto de perfil, mídia) — Fase 1.1

## User Stories

- **Como** assessora dona da empresa, **quero** criar minha conta e workspace **para** começar a usar o sistema imediatamente sem aguardar onboarding manual
- **Como** assessora, **quero** convidar minha equipe pelo e-mail **para** trabalharmos juntas no mesmo workspace
- **Como** assessora, **quero** cadastrar influenciadores com seus handles e nicho **para** centralizar minha carteira
- **Como** assessora, **quero** cadastrar marcas e contatos **para** ter um CRM básico
- **Como** assessora, **quero** buscar e filtrar minhas listas **para** encontrar rápido um influenciador ou marca
- **Como** assessora, **quero** exportar minhas listas em CSV **para** ter backup ou usar dados em outro sistema
- **Como** influenciador (futuro), **quero** ser informado de que estou cadastrado em um sistema **para** cumprir LGPD (envio de e-mail de notificação de tratamento na primeira inserção)

## Design

- **Flow A — Claude Design**. Sem PROMPT.md ainda; tokens placeholder em `docs/specs/design-system/` orientam UI inicial até primeiro handoff.
- Layouts envolvidos:
  - Onboarding (signup + criação de workspace)
  - Sidebar + content (app principal pós-login)
  - Listagem (table responsivo + card view em mobile)
  - Detalhe (drawer lateral com tabs: dados / histórico / relacionamentos)
  - Formulários (single column, validação inline)

## Acceptance Criteria

### Funcional
- [ ] Usuária consegue criar conta, criar assessoria, fazer login (e-mail + senha; Argon2id no DB)
- [ ] Usuária consegue convidar outro e-mail; convidado recebe link de uso único válido por 72h
- [ ] CRUD de influenciador, marca, contato funciona com validação de campos obrigatórios
- [ ] Listagem retorna apenas registros da própria assessoria (multi-tenant strict)
- [ ] Busca por nome retorna resultados com `ILIKE %termo%` + ordenação por relevância simples (exato > prefixo > infixo)
- [ ] Filtros combinados funcionam (ex: nicho=fitness AND tags contém "promissor")
- [ ] Soft-delete: registro some das listagens, permanece no DB com `deleted_at` setado
- [ ] Audit log registra toda criação/edição/deleção com payload diff
- [ ] Export CSV retorna registros não-deletados da assessoria atual
- [ ] Cada criação de influenciador dispara e-mail de notificação de tratamento (LGPD) — assíncrono, com `Idempotency-Key`

### Não-funcional
- [ ] Latência p95 < 300ms em listagens com até 1000 registros
- [ ] Multi-tenant: teste E2E confirma que user da assessoria A recebe 403/404 em recurso da B
- [ ] WCAG 2.1 AA em todos formulários e listagens (axe-core sem violações `serious`)
- [ ] Cobertura ≥ 80% em controllers + services dos módulos novos
- [ ] OpenAPI spec gerada e validada
- [ ] Erros respeitam formato padrão (`docs/specs/api/`)

## Technical Decisions

- ADRs relacionados:
  - [[adr-001-monorepo]] — estrutura
  - [[adr-002-stack-base]] — Java 21 + Spring + Next + Postgres
  - [[adr-003-flyway-migrations]] — migrations
  - [[adr-004-pre-commit-review]] — gates de commit (já existente no template)
- ADRs habilitadores (aceitos):
  - [[adr-005-email-smtp-multi-conta]] — envio de e-mail LGPD ao criar influenciador (notificação de tratamento)
  - [[adr-008-auth-jwt]] — JWT HS256 + refresh rotation (jjwt) + Argon2id
  - [[adr-009-multi-tenant-strategy]] — shared schema + `@Filter` Hibernate + Postgres RLS
  - [[adr-010-async-jobs-postgres-queue]] — disparo do e-mail LGPD via fila (idempotência)
  - [[adr-011-lgpd-baseline]] — base legal por entidade, retenção, soft-delete + purga, opt-out
  - [[adr-013-observability-stack]] — métricas `cadastros_criados_total`, MDC com `assessoria_id`

## Impact on Specs

- **Compliance** (pendente): LGPD — base legal "execução de contrato" para dados de cadastro próprio; "legítimo interesse" para influenciador/marca/contato (devem receber notificação + opt-out). Detalhar quando módulo for ativado.
- **Security**: novo surface — auth flow, convites por token, multi-tenant isolation. Tudo coberto por checklist em `docs/specs/security/`.
- **Scalability**: listagem com paginação cursor-based; índices `(assessoria_id, deleted_at)` em todas tabelas multi-tenant.
- **Observability**: novas métricas `cadastros_criados_total`, `convites_enviados_total`; logs com `assessoriaId` em toda request autenticada.
- **Accessibility**: formulários e tabelas seguem WCAG 2.1 AA; testar com NVDA antes de release.
- **i18n**: pt-BR apenas (vision define MVP).
- **API**: convenções já cobertas em `docs/specs/api/`.
- **Data Architecture**: schema inicial documentado neste PRD; ERD em `docs/architecture/erd.md` (criar durante implementação).
- **Testing Strategy**: cada endpoint tem teste de isolamento multi-tenant.
- **Versioning**: tudo na `/api/v1`.

## Schema (alto nível — refinar em implementação)

```sql
assessorias (
  id UUID PK,
  nome TEXT NOT NULL,
  slug TEXT NOT NULL UNIQUE,
  plano TEXT NOT NULL DEFAULT 'free',
  created_at, updated_at, deleted_at
)

usuarios (
  id UUID PK,
  assessoria_id UUID FK → assessorias(id),
  email CITEXT NOT NULL,
  senha_hash TEXT NOT NULL,           -- Argon2id
  role TEXT NOT NULL CHECK (role IN ('OWNER','ASSESSOR')),
  mfa_secret TEXT NULL,
  ultimo_login_em TIMESTAMPTZ,
  created_at, updated_at, deleted_at,
  UNIQUE (assessoria_id, email)
)

convites (
  id UUID PK,
  assessoria_id UUID FK,
  email CITEXT NOT NULL,
  token TEXT NOT NULL UNIQUE,
  role TEXT NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ NULL,
  created_at, created_by UUID FK → usuarios(id)
)

influenciadores (
  id UUID PK,
  assessoria_id UUID FK,
  nome TEXT NOT NULL,
  handles JSONB NOT NULL DEFAULT '{}',  -- {instagram, youtube, tiktok, twitch, ...}
  nicho TEXT NULL,
  audiencia_total BIGINT NULL,
  observacoes TEXT NULL,
  tags TEXT[] NOT NULL DEFAULT '{}',
  created_at, updated_at, deleted_at,
  created_by UUID FK
)

marcas (
  id UUID PK,
  assessoria_id UUID FK,
  nome TEXT NOT NULL,
  segmento TEXT NULL,
  site TEXT NULL,
  observacoes TEXT NULL,
  tags TEXT[] NOT NULL DEFAULT '{}',
  created_at, updated_at, deleted_at,
  created_by UUID FK
)

contatos (
  id UUID PK,
  marca_id UUID FK → marcas(id),
  assessoria_id UUID FK,                -- denormalizado para query mais simples
  nome TEXT NOT NULL,
  email CITEXT NULL,
  telefone TEXT NULL,
  cargo TEXT NULL,
  created_at, updated_at, deleted_at
)

audit_log (
  id UUID PK,
  assessoria_id UUID FK,
  usuario_id UUID FK,
  entidade TEXT NOT NULL,
  entidade_id UUID NOT NULL,
  acao TEXT NOT NULL CHECK (acao IN ('CREATE','UPDATE','DELETE','RESTORE')),
  payload JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)
```

Índices iniciais (refinar com `EXPLAIN`):
- Toda tabela: `(assessoria_id, deleted_at)`, `(created_at desc)`
- `usuarios`: `UNIQUE (assessoria_id, email)`
- `influenciadores`: GIN em `tags`; trigram em `nome` para busca ILIKE
- `marcas`: GIN em `tags`; trigram em `nome`
- `audit_log`: `(assessoria_id, entidade, entidade_id, created_at desc)`

## Rollout

- [x] **Feature flag**: não aplicável (cadastros são core do MVP — release única)
- [x] **Data migration**: nenhuma (banco novo)
- [x] **Rollback plan**: rollback de release = redeploy versão anterior (Railway tem 1-click rollback). Migrations Flyway são forward-only — caso seja preciso reverter schema, criar migration nova de rollback (não editar aplicada). Auditar dados criados na nova versão antes do rollback (LGPD: notificar usuários se dados forem perdidos).

## Métricas de sucesso (após release)

- ≥ 5 assessorias com pelo menos 10 influenciadores cadastrados em 30 dias pós-launch
- WAU ≥ 60% das assessorias cadastradas
- Latência p95 < 300ms em listagens
- Zero incidentes de vazamento cross-tenant em 90 dias
- ≥ 80% das assessorias usam busca/filtros (não só listagem crua)

## Open questions

- [ ] Paywall em `usuarios` por plano? (ex: free = 1 OWNER + 2 ASSESSOR; pro = ilimitado) — definir antes de cobrar
- [ ] Tags são livres ou catálogo controlado? — propor livres com sugestões baseadas em uso (autocomplete)
- [ ] Campo "instagram handle" valida formato (`@nome` sem espaços) ou aceita texto livre? — validar formato; permitir multi-handle no futuro

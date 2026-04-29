# Changelog

Todas mudanças notáveis do **HUB Feat Creator** documentadas aqui.

Formato baseado em [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) + [SemVer](https://semver.org/lang/pt-BR/).

---

## [Unreleased]

### Added
- Estrutura inicial do projeto a partir do template `claude-proj-blueprint` (L4)
- `CLAUDE.md` (164 linhas) com stack, convenções, gotchas, módulos ativos
- `docs/product/vision.md` com visão, problema, personas, roadmap 4 fases, métricas
- `docs/product/01-cadastros-mvp.md` (PRD-001) — escopo da primeira release
- ADR-001: Estrutura monorepo (`apps/api` + `apps/web`)
- ADR-002: Stack base (Java 21 + Spring Boot 3 + Next.js 14 + PostgreSQL 16)
- ADR-003: Migrations com Flyway
- ADR-004: Pre-commit code review hook (herdado do template, adaptado)
- `docs/specs/` com 12 módulos ativos preenchidos:
  - `security/` — controles OWASP, multi-tenant strict, Argon2id, JWT refresh rotation
  - `observability/` — Micrometer + OpenTelemetry, Grafana, SLOs definidos
  - `scalability/` — Redis caching, Postgres tuning, rate limits, performance budget
  - `versioning/` — semver produto, API path versioning, Flyway naming
  - `design-system/` — tokens placeholder + shadcn/ui + Tailwind
  - `accessibility/` — WCAG 2.1 AA + checklist POUR + axe-core CI
  - `testing-strategy/` — pirâmide 70/25/5, Testcontainers, Playwright
  - `devops/` — GitHub Actions + Railway + Vercel
  - `data-architecture/` — schema inicial, multi-tenant, soft-delete, retenção
  - `api/` — REST + paginação cursor + formato erro padrão + Idempotency-Key
  - `ai-ml/` — placeholder Fase 3 (match marca↔influencer)
  - `long-term-memory/` — Chroma local + global memory cross-project
- `docs/specs/compliance/` (pendente) — baseline LGPD aplicado desde dia 1
- `docs/specs/i18n/` (desativado) — pt-BR only no MVP, convenções i18n-ready desde já
- `.env.example` com vars api + web + memory + integrações futuras
- `memory/config.yaml` configurado para `apps/api/src/` (Java) + `apps/web/` (TS/TSX)
- `README.md` com quickstart e estrutura
- `CONTRIBUTING.md` com convenções do projeto

### Changed
- `docs/specs/README.md` — tabela de módulos ativos/pendentes/desativados
- `docs/design-flow-guide.md` — adaptado para Flow A (Claude Design)

### Decisions registered
- Hash de senha: Argon2id (OWASP 2023 params)
- Auth: JWT HS256 + refresh rotation com reuse detection
- IDs: UUID v7 (preserva ordenação temporal)
- DB locale: `pt_BR.UTF-8`
- Migrations: forward-only, expand-contract para zero-downtime
- Multi-tenant: Hibernate `@Filter` global + testes contractuais cross-tenant
- Component library: shadcn/ui + Radix primitives
- E-mail provider: Resend (sugerido; ajustável)
- Hosting: Railway (api) + Vercel (web)
- Observability: OpenTelemetry + Grafana stack

---

## Releases

> Primeira release tagueada será **v0.1.0** quando PRD-001 (cadastros MVP) entrar em produção. Tudo até lá vive em `[Unreleased]`.

---

## Convenção

- Categorias: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`
- Releases: tag `v<MAJOR>.<MINOR>.<PATCH>` + entry com data
- Releases significativas linkam ao PRD/ADR correspondente
- Geração futura: `git-cliff` a partir de Conventional Commits (avaliar quando releases > 2/semana)

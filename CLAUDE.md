# HUB Feat Creator

## Project
SaaS de centralização para assessorias de influenciadores digitais. Gerencia prospecção, tarefas, envio de e-mails e cadastros (influenciadores + marcas). Roadmap futuro: sugestão de match marca↔influenciador via IA.
Vision completa: `docs/product/vision.md`.

## Tech Stack
- Backend: Java 21 (LTS) + Spring Boot 3.x
- Frontend: Next.js 14+ (App Router) + TypeScript (`strict: true`) + Tailwind CSS
- Database: PostgreSQL 16
- Tests: JUnit 5 + Mockito + Testcontainers (api) | Vitest + Playwright (web)
- Package manager: Maven (api), pnpm (web)
- Hosting: Railway (api), Vercel (web)

## Architecture
Monorepo:
- `/apps/api` → Spring Boot
- `/apps/web` → Next.js
- `/docs` → Obsidian vault (PRDs, ADRs, specs, runbooks)
- `/.claude/` → skills, commands, agents, hooks
- `/memory` → vector DB (long-term memory L4)
- `/scripts` → automation (hooks, bootstrap, review, agent events)

### Documentation directories
- `/docs/product/` → PRDs, vision, roadmap
- `/docs/architecture/` → ADRs
- `/docs/specs/` → módulos ativos (seção "Modular Specifications")
- `/docs/runbooks/` → deploy, debug, onboarding, post-mortems

## Code Conventions
- Style: Spotless + Checkstyle (Java) | ESLint + Prettier (TS)
- Types: TS estrito; Java prefere `record`/`sealed` quando cabe
- Commits: Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`)
- Commit body: incluir `Não alterou:` listando arquivos/módulos intencionalmente não tocados
- Branches: `feature/<nome>`, `fix/<nome>`, `docs/<nome>`, `refactor/<nome>`
- PRs: descrição obrigatória + referência a PRD/ADR
- Idioma: código/identificadores em inglês; docs em pt-BR

## Commands
### Backend (`apps/api`)
- `./mvnw spring-boot:run` → dev
- `./mvnw test` → unit + integration
- `./mvnw verify` → build + lint + tests
- `./mvnw spotless:apply` → format

### Frontend (`apps/web`)
- `pnpm dev` → dev
- `pnpm test` → Vitest
- `pnpm test:e2e` → Playwright
- `pnpm lint` → ESLint
- `pnpm build` → prod

### Slash commands (Claude)
- `/implement <PRD>` → implementa feature de PRD
- `/ralph <PRD>` → persistência até critérios passarem
- `/debug <error|file>` → debug sistemático
- `/refactor <file|module>` → refactor seguro
- `/clean [path]` → remove slop AI
- `/debt [path]` → scan tech debt
- `/learn` → extrai padrões de commits/conversas
- `/deploy` → checklist deploy
- `/spec-review <path>` → audit security+compliance+quality+performance
- `/memory <search|index|stats>` → memória semântica L4

## Workflow Rules
- Rodar `./mvnw verify` (api) e `pnpm lint && pnpm test` (web) antes de cada commit
- Nunca commitar `.env`, segredos, dump de DB
- Mudança em `/apps/*` que afeta produto exige update em `/docs`
- LGPD desde dia 1: dado de pessoa (influenciador, contato, e-mail) com base legal documentada
- E-mails só via provedor configurado (Resend/SES/Postmark) — nunca SMTP local
- Testes de integração tocam DB real via Testcontainers — sem mocks de DB

### Documentation Rules
| Mudança | Atualizar |
|---|---|
| Nova feature/módulo | CLAUDE.md (mapa) + README |
| Nova env var | `.env.example` com default + comentário |
| Decisão arquitetural | ADR em `docs/architecture/` (próximo número) |
| Bug em produção | Post-mortem em `docs/runbooks/post-mortems/` |
| Insight de produto | Nota no PRD em `docs/product/` |
| Mudança de API | Runbook em `docs/runbooks/` + bump de versão |
| Gotcha descoberto | Seção Gotchas abaixo |

Hook `docs-check` avisa se `/apps` mudou sem `/docs`.

### Code Review Gates (L3)
`scripts/pre-commit-review.sh` valida: compilação, testes, secrets, qualidade, error handling, cobertura.
- MUST FIX = bloqueia commit
- SHOULD FIX = warning
- CONSIDER = info
Detalhes em ADR-004 e `docs/specs/code-review-gates.md`.

### Context management (L3+)
- Context guard avisa em conversas longas (50+ tool calls)
- Pre-compact hook salva contexto em `memory/compact-context.md` antes de compactar
- Sessão longa: append em `memory/session-notes.md`
- Início de sessão: ler `memory/compact-context.md` se existir

## Design — Flow A (Claude Design)
- Design system gerado por Claude Design a partir do código + brand assets
- Handoff salvo em `docs/design/<slug>-PROMPT.md` via "Send to Claude Code"
- `/implement` detecta PROMPT.md → skill `claude-design-handoff` reconcilia com PRD + CLAUDE.md
- Conflito: PRD > CLAUDE.md > PROMPT.md (PROMPT.md nunca sobrescreve convenção em silêncio)
- Tokens em `docs/specs/design-system/`

> **Como usar**: app.claude.com → Design → conecta repo → gera DS a partir de assets/código → botão "Send to Claude Code" exporta PROMPT.md.

## Modular Specifications
Detalhes em `docs/specs/<modulo>/README.md`.

### Ativos
- ✅ `security/` → controles técnicos, OWASP
- ✅ `observability/` → logs, métricas, traces, alertas
- ✅ `scalability/` → performance, caching, filas
- ✅ `versioning/` → API versions, migrations Flyway, semver
- ✅ `design-system/` → tokens + padrões UI (Next + Tailwind)
- ✅ `accessibility/` → WCAG 2.1 AA
- ✅ `testing-strategy/` → pirâmide de testes
- ✅ `devops/` → CI/CD (GitHub Actions), IaC, ambientes
- ✅ `data-architecture/` → modelagem Postgres, pipelines
- ✅ `api/` → REST conventions, paginação, erros, versionamento
- ✅ `ai-ml/` → match marca↔influencer (futuro)
- ✅ `long-term-memory/` → vector DB (L4)

### Pendentes / Desativados
- ⏳ `compliance/` → LGPD — ativar antes de produção
- ❌ `i18n/` → MVP só pt-BR

## Model Presets (L4)
| Agent | Model | Por quê |
|---|---|---|
| Lead | opus | Raciocínio complexo, código, arquitetura |
| `security-auditor` | opus | Vulnerabilidades sutis |
| `compliance-auditor` | opus | Interpretação LGPD |
| `quality-guardian` | sonnet | Checklist objetivo |
| `performance-auditor` | sonnet | N+1, índices, caching |

Configurar via `model:` no frontmatter de `.claude/agents/*.md`.

### Deliverables verification (L4)
Saída de agents validada contra schemas em `docs/specs/deliverables/`. Hook `SubagentStop` checa campos obrigatórios.

## Gotchas
- **OAuth tokens sociais** (Instagram/YouTube/TikTok) expiram — refresh com fila de retry; tokens podem revogar silenciosamente quando creator muda senha
- **Rate limits APIs sociais**: IG Graph (200/h/user), YouTube Data (10k units/dia) — backoff exponencial + cache agressivo
- **E-mail deliverability**: SPF/DKIM/DMARC alinhados antes de enviar em massa; warmup de IP/domínio; honrar `List-Unsubscribe`
- **LGPD desde dia 1**: pseudonimizar logs, soft-delete com retenção definida, base legal documentada por finalidade
- **Timezone**: DB em UTC; converter para `America/Sao_Paulo` no front; cuidado com agendamento de tarefas e DST (BR não usa mais, mas libs antigas erram)
- **Postgres locale**: collation `pt_BR.UTF-8` para ordenar nomes com acento ("Álvaro" < "Bruno")
- **Spring Boot 3 + Java 21**: travar `<java.version>21</java.version>` no `pom.xml` e na CI; Java 22+ não LTS
- **Next App Router**: server components default — `'use client'` quando precisar hooks; hidratação de datas requer ISO no servidor
- **Soft-delete obrigatório**: `influenciador`, `marca`, `prospeccao` nunca DELETE direto — `deleted_at` + job de purga após retenção
- **Idempotência envio e-mail**: chave idempotente na fila (retry não pode reenviar)
- **CORS Vercel↔Railway**: `CORS_ALLOWED_ORIGINS` explícito; cuidado com preview URLs do Vercel
- **Migrations**: Flyway versionado em `apps/api/src/main/resources/db/migration` — nunca editar V já aplicada, criar nova

## Memory (L4)
Busca semântica em `docs/` e `apps/`:
- Index: `python memory/index.py`
- Search: `python memory/query.py "consulta"`
- Incremental: `python memory/index.py --incremental`
- Global (cross-project): `python memory/query.py "consulta" --global`
- Aprender de conversas: `/learn --conversations 5`
- Config: `memory/config.yaml`

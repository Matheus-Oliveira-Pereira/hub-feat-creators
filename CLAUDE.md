# HUB Feat Creator

## Project
SaaS de centralização para assessorias de influenciadores digitais. Gerencia prospecção, tarefas, envio de e-mails e cadastros (influenciadores + marcas). Roadmap futuro: sugestão de match marca↔influenciador via IA.
Vision completa: `docs/product/vision.md`.

## Tech Stack
- Backend: Java 21 (LTS) + Spring Boot 3.x
- Frontend: Next.js 14+ (App Router) + TypeScript (`strict: true`) + Tailwind CSS + shadcn/ui (Radix) + Framer Motion + Recharts
- Mobile: React Native + Expo (SDK 50+) + TypeScript (`strict: true`) — usuário final apenas
- Database: PostgreSQL 16
- E-mail: Jakarta Mail (Spring Mail) via SMTP relay externo (multi-conta cadastrada pelo usuário)
- WhatsApp: Meta WhatsApp Cloud API (oficial)
- Tests: JUnit 5 + Mockito + Testcontainers (api) | Vitest + Playwright (web) | Jest + Detox (mobile)
- Package manager: Maven (api), pnpm (web + mobile)
- Hosting: Railway (api), Vercel (web), EAS (mobile builds)

## Architecture
Monorepo:
- `/apps/api` → Spring Boot
- `/apps/web` → Next.js (admin + operacional)
- `/apps/mobile` → Expo (usuário final — sem telas admin/config)
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

### Mobile (`apps/mobile`)
- `pnpm start` → Expo dev server
- `pnpm ios` / `pnpm android` → run em simulador/device
- `pnpm test` → Jest + RNTL
- `pnpm test:e2e` → Detox
- `pnpm lint` → ESLint
- `eas build --profile preview` / `eas build --profile production` → builds via EAS

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
- E-mails enviados via SMTP relay externo cadastrado pelo usuário (Jakarta Mail) — credenciais cifradas em repouso (AES-GCM com KMS); nunca MTA local; nunca commitar credenciais
- Notificações ao usuário final via WhatsApp Cloud API (oficial Meta) com templates aprovados; opt-in obrigatório (LGPD)
- Mobile (Expo) só telas de usuário final — admin/config exclusivo na web
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

> **Como usar**: claude.ai (ícone paleta na sidebar) → Design → conecta repo → gera DS a partir de assets/código → botão "Send to Claude Code" exporta PROMPT.md. Requer plano Pro/Max/Team/Enterprise.

### Brand
- Cores: `#C2E000` (lime/primary) + `#141414` (ink/foreground) + `#FFFFFF` (background)
- Tipo: Bricolage Grotesque (display) + Inter (body) + JetBrains Mono — todas Google Fonts via `next/font`
- Logo: wordmark "feat. creators" + mark com símbolo grafema lime sobre quadrado escuro
- Tema dark/light com toggle + auto-detect (`next-themes`, atributo `data-theme`)

### Web architecture (`apps/web`)
- `app/(auth)/` → login, signup (layout duo-painel hero+form)
- `app/(app)/` → rotas autenticadas com AppShell (sidebar colapsável + topbar + Cmd+K)
- `app/(app)/page.tsx` → dashboard home com KPIs reais + funil chart
- `app/(app)/influenciadores`, `app/(app)/marcas` → listagens cards/tabela + drawer detalhe + modal create
- `app/(app)/prospeccao` → kanban (`@dnd-kit`) + lista + drawer com tabs + modais
- `app/(app)/tarefas` → lista + agenda semanal + drawer detalhe + modal criar (PRD-003)
- `app/(app)/perfis` → CRUD de perfis RBAC com checkboxes agrupados por entidade
- `components/ui/` → primitives shadcn (button, input, textarea, select, checkbox, dialog, sheet, dropdown-menu, command, tabs, popover, tags-input, etc.)
- `components/app/` → shell e blocos compostos (sidebar, topbar, command-palette, page-header, filter-bar, entity-form-modal, stat-card, empty-state, prospeccao-kanban, prospeccao-detail-sheet, tarefa-detail-sheet, tarefa-agenda)
- `components/forms/` → form modais por entidade (influenciador, marca, contato, perfil, prospeccao, fechar-perdida, tarefa)
- `components/auth/can.tsx` → componente RBAC `<Can role="...">` para esconder UI sem permissão
- `components/brand/` → logo
- `lib/api.ts` → fetch client + tipos por entidade (influenciador, marca, contato, perfil, prospeccao)
- `lib/queries.ts` → hooks TanStack Query + `qk` query keys
- `lib/schemas.ts` → schemas zod
- `lib/auth.ts` → AuthProvider + decodeJwt + usePermissions/usePermission
- `lib/rbac.ts` → catálogo de roles 4-letter (espelha `PermissionCodes.java`)
- `lib/prospeccao.ts` → STATUS_LABEL/TONE/ORDER + isValidTransition (espelha state machine)
- `lib/utils.ts` → `cn()` helper

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
- ✅ `prospeccao/` → state machine + endpoints + métricas (PRD-002, ADR-015 visibility)
- ✅ `rbac/` → roles 4-letter + perfis + aspect (PRD-005, ADR-015)
- ✅ `tarefas/` → CRUD + scheduler digest + alertas in-app (PRD-003, ADR-010)
- ⏳ `email/` → SMTP relay multi-conta (Jakarta Mail) — ADR-005
- ⏳ `whatsapp/` → Cloud API oficial Meta — ADR-006
- ⏳ `mobile/` → Expo (usuário final) — ADR-007

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
- **SMTP multi-conta**: connection pool por conta; respeitar rate limit do provedor (Gmail 500/dia, Outlook 300/dia, M365 10k/dia); circuit breaker em falha de auth (senha trocou/app password revogado); rotacionar credenciais cifradas
- **WhatsApp Cloud API**: templates HSM precisam aprovação Meta (24-48h); janela de 24h para mensagens free-form após resposta do usuário; webhook de status (sent/delivered/read/failed) é assíncrono; rate limit por número (1k msgs/dia tier inicial)
- **Expo OTA updates**: mudanças JS via `eas update` sem store review; mudanças nativas exigem novo build + submit; travar SDK version no monorepo
- **Mobile auth**: token JWT em SecureStore (iOS Keychain / Android Keystore) — nunca AsyncStorage
- **CORS Vercel↔Railway**: `CORS_ALLOWED_ORIGINS` explícito; cuidado com preview URLs do Vercel
- **Migrations**: Flyway versionado em `apps/api/src/main/resources/db/migration` — nunca editar V já aplicada, criar nova
- **Next 14 + `useSearchParams`**: páginas client que usam `useSearchParams()` precisam de `<Suspense>` boundary acima — sem isso, `pnpm build` falha em prerender com "useSearchParams should be wrapped in a suspense boundary". Padrão: exportar wrapper `Page` com Suspense, conteúdo real em `Inner`
- **lucide-react**: ícone `Instagram` foi removido em versões recentes — usar `AtSign` (ou texto "@") para handles
- **typedRoutes**: `next.config.mjs` tem `experimental.typedRoutes: true` — `href` exige `Route` (`import { type Route } from 'next'` + cast `'/path' as Route`); rotas com querystring também
- **Tailwind tokens via HSL**: cores em `globals.css` armazenam HSL components sem prefixo `hsl()` (ex: `--primary: 68 100% 44%`); `tailwind.config.ts` aplica `hsl(var(--primary))` — nunca hardcodear hex em componente
- **Recharts SSR**: warning "width/height -1" no prerender é benigno (container tem 0px no momento da geração estática) — chart hidrata correto no cliente
- **Digest idempotência**: chave = `UUID.nameUUIDFromBytes(assessoriaId + ":EMAIL_DIGEST:" + data)` — rerun do job no mesmo dia ignora silenciosamente via UNIQUE index em `job(assessoria_id, tipo, idempotency_key)`
- **Tarefa prazo default**: quando usuária escolhe só data (sem hora), default é `23:59` do dia — definido em `toFormDefaults()` no form modal
- **DigestJobHandler sem destinatário**: handler loga o digest mas não envia e-mail real até integração com PRD-004 (e-mail outbound); `enviado_para` resolvido por `UserRepository` quando disponível
- **State machine tarefas**: `FEITA → TODO` limpa `concluida_em`; editar tarefa com status terminal (`FEITA|CANCELADA`) retorna HTTP 422
- **`@RequirePermission` em controllers novos**: sempre anotar; aspect `RequirePermissionAspect @Order(1)` é separado do `TenantAspect @Order(3)` — não exige transação ativa, só `SecurityContext` populado pelo `JwtAuthFilter`. Falta da anotação = endpoint público (cuidado)
- **Mudança de perfil só propaga no próximo refresh JWT** (até 60 min): documentado em ADR-015. Se UX virar dor, evento via websocket Fase 2
- **State machine de prospecção**: matriz fixa em `ProspeccaoStateMachine.java` espelhada em `lib/prospeccao.ts` — quando alterar uma, alterar a outra. Transição inválida → HTTP 422
- **Visibilidade ASSESSOR vs OWNER**: row-level filter aplicado no service, não via Hibernate `@Filter` (dependeria do principal). `findAllAssessor` usa predicate `created_by OR assessor_responsavel_id = me`. Tentativa fora do escopo retorna `404` (não vaza existência)
- **Códigos 4-letter**: lista canônica em `PermissionCodes.java` + `lib/rbac.ts`. Sincronizar manualmente — sem code-gen

## Memory (L4)
Busca semântica em `docs/` e `apps/`:
- Index: `python memory/index.py`
- Search: `python memory/query.py "consulta"`
- Incremental: `python memory/index.py --incremental`
- Global (cross-project): `python memory/query.py "consulta" --global`
- Aprender de conversas: `/learn --conversations 5`
- Config: `memory/config.yaml`

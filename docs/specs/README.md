# Modular Specifications — HUB Feat Creator

Cada módulo é independente e plug-and-play. Esta página lista o **estado atual** de cada módulo no projeto.

## Módulos ativos

| Módulo | Estado | Responsabilidade |
|--------|--------|------------------|
| `security/` | ✅ ativo | Autenticação, autorização, OWASP, segredos, multi-tenant isolation |
| `observability/` | ✅ ativo | Logs estruturados, métricas, traces (OpenTelemetry), alertas |
| `scalability/` | ✅ ativo | Caching, filas (envio de e-mail, jobs), índices Postgres, paginação |
| `versioning/` | ✅ ativo | Versionamento de API REST, migrations Flyway, semver de releases |
| `design-system/` | ✅ ativo | Tokens (cores, espaçamento, tipografia), padrões de componentes Tailwind, estratégia UI |
| `accessibility/` | ✅ ativo | WCAG 2.1 AA — contrastes, navegação por teclado, ARIA, leitores de tela |
| `testing-strategy/` | ✅ ativo | Pirâmide de testes (unit > integration > e2e), Testcontainers, Playwright |
| `devops/` | ✅ ativo | CI (GitHub Actions), CD (Railway + Vercel), IaC, ambientes (dev/staging/prod) |
| `data-architecture/` | ✅ ativo | Modelagem Postgres, integridade referencial, soft-delete, retenção LGPD |
| `api/` | ✅ ativo | REST conventions, formato de erro, paginação, idempotência, versionamento |
| `ai-ml/` | ✅ ativo | (Fase 3) Modelo de match marca↔influencer, prompts, evals, guardrails |
| `long-term-memory/` | ✅ ativo | Vector DB para busca semântica em docs/código (L4) |

## Pendentes / Desativados

| Módulo | Estado | Motivo |
|--------|--------|--------|
| `compliance/` | ⏳ pendente | LGPD — ativar antes de ir para produção |
| `i18n/` | ❌ desativado | MVP só pt-BR; reavaliar pós-PMF |

## Convenção
- Cada módulo tem um `README.md` como entry point
- Documentos de apoio dentro da pasta do módulo
- Skill correspondente em `.claude/skills/<modulo>/SKILL.md` (quando aplicável)
- Agente de auditoria em `.claude/agents/` (quando aplicável)
- Marcadores de placeholder em `README.md` de módulos pendentes serão preenchidos quando o módulo for ativado

## Adicionar módulo customizado
1. Criar pasta `docs/specs/<nome>/`
2. Adicionar `README.md` com a estrutura do módulo
3. Opcional: criar skill em `.claude/skills/<nome>/SKILL.md`
4. Adicionar entrada na seção "Modular Specifications" do `CLAUDE.md`

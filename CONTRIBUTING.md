# Contribuindo — HUB Feat Creator

Obrigado por considerar contribuir. Este projeto é privado/solo dev no momento; este guia documenta as convenções para o autor e futuros colaboradores.

## Setup

Pré-requisitos:
- Java 21 LTS (recomendado: Eclipse Temurin)
- Node 20+
- pnpm 9+
- Docker (Testcontainers + Postgres local)
- Git

```bash
git clone https://github.com/Matheus-Oliveira-Pereira/hub-feat-creators.git
cd hub-feat-creators
cp .env.example .env   # editar com valores reais

# Backend
cd apps/api
./mvnw verify

# Frontend
cd ../web
pnpm install
pnpm dev
```

## Workflow

1. **Issue ou PRD primeiro** — todo trabalho > 1h tem issue ou PRD em `docs/product/` referenciado
2. **Branch** a partir de `main`:
   - `feature/<descricao-curta>` — nova feature
   - `fix/<descricao-curta>` — bug
   - `docs/<descricao-curta>` — só docs
   - `refactor/<descricao-curta>` — refactor
   - `chore/<descricao-curta>` — config / CI / deps
3. **Commits**: Conventional Commits
   - `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `perf:`
   - Body inclui `Não alterou:` listando arquivos/módulos intencionalmente não tocados (convenção do projeto)
   - Idioma do commit: pt-BR ou en — consistente dentro do mesmo PR
4. **Pre-commit local**: `./mvnw verify` (api) e `pnpm lint && pnpm test` (web) antes de `git commit`
5. **PR**:
   - Título conciso (< 70 chars)
   - Descrição com:
     - O que muda
     - Por que (referência a PRD/ADR/issue)
     - Como testar
   - Rodar checklist em `.github/PULL_REQUEST_TEMPLATE.md` (criar quando primeiro PR)
6. **Review**:
   - CI verde obrigatório
   - 1 review aprovado (auto-aprovação solo dev nesta fase, mas justificar no comentário do merge)
   - Branches efêmeras: deletar após merge

## Convenções de código

### Backend (Java)
- **Style**: Spotless (`./mvnw spotless:apply` antes de commitar)
- **Lint**: Checkstyle baseado em Google Java Style com customizações
- **Estrutura**: pacote por feature (`com.hubfeatcreators.influenciadores`, não `com.hubfeatcreators.controllers`)
- **Imutabilidade**: preferir `record` para DTOs; `final` em variáveis locais
- **Null**: evitar; usar `Optional` em retornos onde aplicável
- **Exceções**: custom exceptions por domínio (`InfluenciadorNotFoundException`); handler global mapeia para erro padrão
- **Nomeação**: identificadores em **inglês**; mensagens user-facing em **pt-BR** via `MessageSource`
- **Tests**: `should<Behavior>_when<Condition>` (ver `docs/specs/testing-strategy/`)

### Frontend (TypeScript)
- **Style**: Prettier + ESLint (configs em `apps/web/`)
- **Strict TS**: `strict: true`, `noUncheckedIndexedAccess: true`
- **Componentes**: 1 componente por arquivo; nome do arquivo = nome do componente (`PascalCase.tsx`)
- **Hooks**: prefixo `use`; arquivo `useFoo.ts`
- **Server vs Client**: prefer Server Components; `'use client'` só quando precisa hooks/events
- **Forms**: `react-hook-form` + `zod` para validação
- **Data fetching**: TanStack Query
- **Estilização**: Tailwind utility-first; tokens via CSS variables (`docs/specs/design-system/`)

### Docs (pt-BR)
- Markdown padrão; Obsidian-friendly (wiki-links `[[adr-001-monorepo]]`)
- ADRs: numerados sequencialmente; nunca remover, só marcar `Superseded by ADR-XXX`
- PRDs: numerados; usar `_template-prd.md`
- Post-mortems: usar `_template-post-mortem.md`

## Estrutura de docs (Obsidian vault em `/docs`)

- `product/` → PRDs, vision, roadmap
- `architecture/` → ADRs
- `specs/` → módulos modulares (security, api, etc)
- `runbooks/` → deploy, debug, post-mortems

Toda mudança que afete produto exige update em `/docs` (hook `docs-check` avisa).

## Workflow Claude Code

Slash commands disponíveis em `CLAUDE.md`. Principais:
- `/implement <PRD>` — implementar feature de PRD
- `/spec-review <path>` — auditoria multi-agente
- `/debug` — debug sistemático
- `/clean` — remover slop AI
- `/memory <search>` — busca semântica histórica

Skills auto-invocadas por keywords (ex: "debug", "refactor", "tech debt").

## Code review gates

Toda commit passa por `scripts/pre-commit-review.sh`:
- MUST FIX → bloqueia
- SHOULD FIX → warning
- CONSIDER → info

Detalhes em `docs/specs/code-review-gates.md` e ADR-004.

## Reportando bugs

- Issue no GitHub com:
  - Reprodução passo a passo
  - Comportamento esperado vs observado
  - Stack trace ou screenshot quando aplicável
  - Versão do produto (tag/commit hash)
  - Browser (se frontend) ou JDK (se backend)
- Bug crítico (vazamento de dados, perda de dados): marcar como **P0** + contatar autor diretamente

## Sugerindo features

- Issue tipo "feature request" descrevendo:
  - Problema do usuário (não a solução)
  - Quem é afetado e em qual frequência
  - Proposta inicial (opcional)
- Discussão na issue antes de PR — features grandes viram PRD em `docs/product/` antes de código

## Code of Conduct

Seja claro. Seja construtivo. Seja respeitoso. Estamos aqui para construir software bom.

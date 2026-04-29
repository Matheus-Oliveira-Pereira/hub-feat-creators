# HUB Feat Creator

SaaS de centralização para assessorias de influenciadores digitais — prospecção, tarefas, comunicação e cadastros em um único hub. Roadmap futuro: sugestão de match marca↔influenciador via IA.

> Construído sobre o blueprint [claude-proj-blueprint](https://github.com/rtazima/claude-proj-blueprint) (L4 — autonomous system).

---

## Stack

| Camada | Tech |
|--------|------|
| Backend | Java 21 + Spring Boot 3.x |
| Frontend | Next.js 14+ (App Router) + TypeScript + Tailwind CSS |
| Database | PostgreSQL 16 |
| Tests | JUnit 5 + Mockito + Testcontainers (api) / Vitest + Playwright (web) |
| Hosting | Railway (api) + Vercel (web) |
| Package manager | Maven (api), pnpm (web) |

## Estrutura do repositório

```
hub-feat-creators/
├── apps/
│   ├── api/                ← Spring Boot
│   └── web/                ← Next.js
├── docs/                   ← Obsidian vault (PRDs, ADRs, specs, runbooks)
├── memory/                 ← Long-term vector memory (L4)
├── scripts/                ← Hooks, bootstrap, review automation
├── .claude/                ← Skills, commands, agents, hooks
├── CLAUDE.md               ← Hub de contexto para Claude Code
└── README.md
```

## Quick start

### Pré-requisitos
- Java 21 (LTS)
- Node 20+ e pnpm 9+
- Docker (para Testcontainers + Postgres local)
- Maven 3.9+ (ou usar `./mvnw`)

### Setup
```bash
# Clonar
git clone https://github.com/Matheus-Oliveira-Pereira/hub-feat-creators.git
cd hub-feat-creators

# Variáveis de ambiente
cp .env.example .env
# editar .env com valores reais

# Backend
cd apps/api
./mvnw verify
./mvnw spring-boot:run

# Frontend (em outro terminal)
cd apps/web
pnpm install
pnpm dev
```

### Comandos úteis
| Comando | O que faz |
|---------|-----------|
| `./mvnw verify` | build + lint + tests (api) |
| `./mvnw spring-boot:run` | dev server (api) |
| `pnpm dev` | dev server (web) |
| `pnpm test` | Vitest |
| `pnpm test:e2e` | Playwright |
| `pnpm build` | build produção (web) |

## Documentação

Toda documentação vive em `/docs` (Obsidian vault):

- **Vision**: [`docs/product/vision.md`](docs/product/vision.md)
- **PRDs**: `docs/product/`
- **ADRs**: `docs/architecture/`
- **Specs modulares**: [`docs/specs/README.md`](docs/specs/README.md)
- **Runbooks** (deploy, debug, post-mortems): `docs/runbooks/`

> Abrir `docs/` como vault no Obsidian para navegação por wiki-links.

## Workflow com Claude Code

Slash commands disponíveis:

| Comando | Uso |
|---------|-----|
| `/implement <PRD>` | implementa feature de PRD |
| `/ralph <PRD>` | persistência até critérios passarem |
| `/debug <error\|file>` | debug sistemático |
| `/refactor <file\|module>` | refactor seguro |
| `/clean [path]` | remove slop AI |
| `/debt [path]` | scan tech debt |
| `/spec-review <path>` | audit security + compliance + quality + performance |
| `/memory <search\|index\|stats>` | memória semântica L4 |
| `/learn` | extrai padrões de commits/conversas |

Convenções completas em [`CLAUDE.md`](CLAUDE.md).

## Convenções

- **Commits**: Conventional Commits (`feat:`, `fix:`, `docs:`, etc.) com `Não alterou:` no body
- **Branches**: `feature/<nome>`, `fix/<nome>`, `docs/<nome>`, `refactor/<nome>`
- **Idioma**: código/identificadores em inglês; docs em pt-BR
- **PRs**: descrição obrigatória + referência a PRD/ADR

## Licença

Veja [LICENSE](LICENSE).

## Autor

Matheus Oliveira Pereira — [@Matheus-Oliveira-Pereira](https://github.com/Matheus-Oliveira-Pereira)

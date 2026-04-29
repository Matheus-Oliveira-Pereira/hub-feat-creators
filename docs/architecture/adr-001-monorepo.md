# ADR-001: Estrutura Monorepo (apps/api + apps/web)

## Status
Accepted — 2026-04-29

## Context
HUB Feat Creator é composto por um backend Java (Spring Boot) e um frontend TypeScript (Next.js). Precisamos decidir como organizar o código fonte: monorepo único ou repositórios separados.

Variáveis em jogo:
- Time: solo dev no MVP, com possibilidade de crescer
- Releases: backend e frontend evoluem juntos no MVP (features cross-stack)
- Ferramentas: blueprint adotado (`claude-proj-blueprint`) assume `/src` único; queremos manter compatibilidade com hooks, skills e specs sem reescrever
- Tipos compartilhados: contratos de API (DTOs) podem ser tipos TS gerados de OpenAPI — facilita em monorepo
- Familiaridade: solo dev tem mais experiência com layout simples e visualizado completo no editor

## Decision
Monorepo único com layout `apps/`:

```
hub-feat-creators/
├── apps/
│   ├── api/                ← Spring Boot (Maven)
│   └── web/                ← Next.js (pnpm)
├── docs/
├── memory/
├── scripts/
├── .claude/
├── CLAUDE.md
└── README.md
```

- Cada app tem seu próprio gerenciador (Maven na api, pnpm na web)
- Sem workspace pnpm na raiz por enquanto (web é app único, não há packages a compartilhar)
- CI usa matrix por app
- Deploy independente: Railway api + Vercel web (cada um aponta para sua subpasta)
- Versão semântica unificada para o produto (release única na raiz com tag `vX.Y.Z`)

## Alternatives considered

1. **Polyrepo** (`hub-api` + `hub-web` em repos separados)
   - **+** isolamento total, ownership claro futuro
   - **+** CI/CD independente por padrão
   - **−** sincronização cross-stack vira PR coordenado em 2 lugares
   - **−** tipos de API duplicados sem ferramenta extra
   - **−** com 1 dev, fricção desnecessária
   - **−** breakar contrato fica fácil (sem visibilidade no mesmo PR)

2. **Monorepo com workspace pnpm + Maven multi-module**
   - **+** tipos TS compartilhados via package interno
   - **+** comandos uniformes na raiz
   - **−** complexidade extra para 1 dev no MVP
   - **−** Maven multi-module só faz sentido com múltiplos módulos Java (não temos)
   - Reavaliar em Fase 2/3 quando surgir SDK TS, libs compartilhadas etc

3. **Monorepo com `src/api` + `src/web`** (estrutura padrão do blueprint)
   - **+** alinhado 1:1 com blueprint original
   - **−** `src/` mistura conceitos de "código" com "apps"; layout `apps/` é convenção mais clara para multi-stack
   - **−** scripts do blueprint que assumem `src/` → ajustar é trivial

## Consequences

**Positivas**:
- 1 PR cobre mudança cross-stack (contrato + consumo)
- Memory L4 vetoriza ambos juntos (queries semânticas cobrem todo o produto)
- Onboarding simples (1 clone, todo contexto)
- ADRs e specs cobrem ambos sem duplicação

**Negativas**:
- CI precisa caminho-aware (build da api só quando `apps/api` muda) — mitigar com `paths:` no GHA
- Permissões Git de mais granular ficam impossíveis (futuro: CODEOWNERS resolve em parte)

**Riscos**:
- Caso projeto tenha múltiplos clientes (web, mobile, CLI) e times separados, custo de manter monorepo cresce — reavaliar quando time ≥ 5

## Impact on specs
- **devops**: pipelines com `paths:` filter por app
- **versioning**: versão única do produto; API com versionamento próprio (`/api/v1`) independente da versão do produto
- **long-term-memory**: indexar `apps/api/src/` (Java) e `apps/web/` (TS/TSX) — já refletido em `memory/config.yaml`
- **testing-strategy**: matriz CI separa unit/integration por app

## References
- PRD: (n/a — decisão estrutural pré-features)
- Blueprint origem: https://github.com/rtazima/claude-proj-blueprint
- Discussão monorepo vs polyrepo: Hashicorp, Google, Meta engineering blogs

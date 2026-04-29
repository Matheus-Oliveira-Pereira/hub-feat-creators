# Module: Versioning — HUB Feat Creator

Versionamento de produto, API e migrations.

## Semver de produto

- **Formato**: `MAJOR.MINOR.PATCH`
- **MAJOR**: breaking change na API pública ou mudança incompatível de schema/uso
- **MINOR**: nova feature backward-compatible
- **PATCH**: bugfix
- **Pre-release**: `-alpha.N`, `-beta.N`, `-rc.N`
- **Release tool**: **manual no MVP** (criar tag `v1.2.3` + release no GitHub via `/deploy`); avaliar `semantic-release` quando frequência de release > 2/semana
- **Changelog**: `CHANGELOG.md` na raiz, gerado a partir de Conventional Commits via `git-cliff` ou similar

## API versioning

- **Estratégia**: **URL path** (`/api/v1/`, `/api/v2/`)
- **Razão**: caching simples (rota é a chave), debug fácil, ferramentas como Postman/curl triviais
- **Versões simultâneas**: máx **2** (atual + uma deprecada)
- **Política de deprecação**:
  - Notice mínimo: **6 meses** antes de remoção (b2b SaaS — clientes precisam tempo para adaptar integrações)
  - Headers em endpoint deprecado: `Deprecation: true`, `Sunset: <RFC 1123 date>`, `Link: <new-endpoint>; rel="successor-version"`
  - E-mail aos owners de assessoria 3 meses, 1 mês e 1 semana antes do sunset
- **Changelog API**: `docs/api/CHANGELOG.md` + nota em release notes do produto

### Quando bumpa MAJOR (API)
- Remove endpoint
- Remove campo de response
- Renomeia campo
- Muda tipo de campo (string → number)
- Muda comportamento default que clientes possam depender
- Muda formato de erro
- Adiciona campo obrigatório em request

### Quando NÃO bumpa MAJOR
- Adiciona endpoint
- Adiciona campo opcional em response
- Adiciona campo opcional em request
- Adiciona código de erro novo (clientes devem tratar genericamente)

## Database migrations

- **Tool**: **Flyway** (Spring Boot integrado)
- **Localização**: `apps/api/src/main/resources/db/migration/`
- **Naming**: `V<YYYYMMDDHHmm>__<descricao_em_snake_case>.sql`
  - Ex: `V202604291430__create_assessorias.sql`
  - `V202604301015__add_index_prospeccoes_status.sql`
- **Repeatable**: `R__<descricao>.sql` para views, funções, seeds idempotentes
- **Reversibilidade**: opcional — Flyway Open Source não roda automaticamente. Toda destrutiva exige plano de rollback escrito no ADR.

### Regras
1. **Nunca editar V já aplicada em qualquer ambiente** — criar nova migration
2. **Migration destrutiva** (DROP COLUMN, DROP TABLE, ALTER TYPE incompatível) exige:
   - ADR específico
   - Plano de rollback
   - Backup verificado antes
   - Janela comunicada com 48h de antecedência
3. **Testar em staging** antes de prod
4. **Migration que pode demorar > 10s** em prod: usar técnica zero-downtime (ver abaixo)
5. **Nunca inserir dados de teste** em migration de schema (separar `R__seed_test.sql` que só roda em dev/test profile)

### Zero-downtime — pattern Expand-Contract

```
1. Expand   → adicionar coluna nova nullable / nova tabela
2. Backfill → job ou trigger preenche dados (em background, batch)
3. Dual-write → app escreve em ambos por algumas releases
4. Cutover  → app lê do novo
5. Contract → remove código antigo + drop coluna velha (em release seguinte, com ADR)
```

Casos típicos:
- Renomear coluna: `add new` → `dual-write` → `migrate reads` → `drop old`
- Mudar tipo: `add new column` → `cast on write` → `cutover` → `drop old`
- Particionar tabela: `criar particionada` → `dual-write` → `migrar dados` → `swap`

### Backfills
- Batch de 1k linhas com pausa de 100ms entre batches (não saturar DB em horário comercial)
- Idempotente (poder rodar de novo sem duplicar)
- Job em tabela `migrations_backfill_state` registra progresso (resumível)

## Git

- **Estratégia**: **GitHub Flow** (main + feature branches curtas)
- **Razão**: time pequeno, deploy contínuo, sem necessidade de develop branch
- **Branches**:
  - `feature/<descricao>` — feature ou enhancement
  - `fix/<descricao>` — bugfix
  - `docs/<descricao>` — só docs
  - `refactor/<descricao>` — refactor
  - `chore/<descricao>` — config, CI, deps
- **Vida da branch**: ≤ 5 dias; PRs maiores quebram em sub-PRs
- **Commits**: Conventional Commits (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`, `perf:`)
- **Body de commit**: `Não alterou:` listando arquivos/módulos intencionalmente não tocados (convenção do projeto)
- **Tags**: `vMAJOR.MINOR.PATCH` em toda release (assinada via GPG quando viável)
- **Branches protegidas**: `main` — bloqueio direct push, exige PR + 1 review + CI green
- **Merge strategy**: **Squash and merge** (histórico linear; PR vira 1 commit no main)

## Change control

| Tipo | Bumpa | Exige |
|------|-------|-------|
| Breaking change | MAJOR | ADR + migration plan + 6m deprecation se afeta API |
| Nova feature | MINOR | PRD referenciado |
| Bugfix | PATCH | Issue ou descrição clara |
| Mudança de schema destrutiva | MAJOR (API se exposta) | ADR + zero-downtime plan |
| Hotfix produção | PATCH | Pode pular fila — branch `hotfix/...` direto de tag prod, merge → main + cherry-pick |

Changelog auto-gerado de Conventional Commits via `git-cliff`:
```bash
git-cliff -o CHANGELOG.md
```

## Releases
- Toda release tem entry em `CHANGELOG.md`
- GitHub Release criada automaticamente via workflow ao push de tag
- Notas de release em pt-BR, agrupadas por categoria (Features, Fixes, Performance, Breaking)
- Release de produção comunicada em Slack `#releases` e e-mail aos owners de assessoria (apenas mudanças visíveis ao usuário)

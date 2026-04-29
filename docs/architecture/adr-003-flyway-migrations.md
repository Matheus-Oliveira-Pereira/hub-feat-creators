# ADR-003: Migrations com Flyway

## Status
Accepted — 2026-04-29

## Context
Precisamos de uma estratégia de versionamento de schema do PostgreSQL que:
- Seja repetível em dev / CI / staging / prod
- Permita zero-downtime quando viável (expand-contract)
- Seja auditável (schema do dia X é reconstruível)
- Bloqueie edição retroativa de migration aplicada
- Integre nativo ao build Maven e ao boot do Spring

Opções avaliadas: Flyway, Liquibase, jOOQ DDL, scripts SQL manuais com tabela own-rolled.

## Decision

Adotar **Flyway** (Open Source) com:
- **Localização**: `apps/api/src/main/resources/db/migration/`
- **Naming**: `V<YYYYMMDDHHmm>__<descricao_em_snake_case>.sql`
  - Exemplos: `V202604291430__create_assessorias.sql`, `V202605021015__add_index_prospeccoes_status.sql`
- **Repeatable**: `R__<descricao>.sql` para views/funções/seeds idempotentes (rodam sempre que mudam)
- **Profile-aware seed**: `R__seed_dev.sql` em `db/migration/dev/` (incluído só com Spring profile `dev` ou `test`)
- **Rollback**: NÃO usar `U__` automático (Flyway OSS não suporta out-of-the-box). Toda migration destrutiva exige plano de rollback escrito no ADR correspondente.
- **Boot**: Flyway roda automaticamente no startup do Spring Boot (`spring.flyway.enabled=true`)
- **CI**: job dedicado roda `mvn flyway:migrate` em DB Postgres limpo (Testcontainers) e em DB com versão anterior — falha se quebrar
- **Prod**: migrations rodam no boot da nova versão. Em mudanças longas, usar pattern expand-contract (ver `docs/specs/versioning/`)

### Regras invariantes

1. **Nunca editar V já aplicada** em qualquer ambiente — criar nova migration corretiva
2. **Toda destrutiva (DROP, ALTER TYPE incompatível) exige ADR** com plano de rollback
3. **Migration > 10s em prod**: seguir pattern expand-contract (não bloquear deploy do código novo)
4. **Backfill**: scripts separados em `apps/api/src/main/java/.../backfill/` rodando como job, não como migration SQL gigante
5. **Sem dados de teste em V**: separar `R__seed_dev.sql` que só roda em dev/test profile

## Alternatives considered

1. **Liquibase**
   - **+** suporte nativo a rollback (changelog XML/YAML)
   - **+** mais flexível (formatos múltiplos, conditions)
   - **−** verbose; mais cerimônia para mudanças simples
   - **−** menos comum em Spring Boot moderno (Flyway é o default da própria docs)
   - **−** rollback automático é traiçoeiro em prod — migrar com ADR+manual é mais seguro de qualquer forma

2. **jOOQ DDL / migration via Java**
   - **+** type-safe, refactor-friendly
   - **−** atravessar SQL puro vira tradução manual; perda de visibilidade
   - **−** stack adicional (jOOQ é overkill se não usarmos para queries)

3. **Scripts SQL manuais com tabela `migrations` own-rolled**
   - **+** zero dependência
   - **−** reinventa Flyway com bugs
   - **−** sem validação de checksum, sem locking distribuído

4. **Sem migrations (Hibernate `ddl-auto=update`)**
   - **+** zero esforço
   - **−** **inaceitável em prod** — destrutivo silencioso, sem auditoria, sem rollback
   - Descartado de imediato

## Consequences

**Positivas**:
- Schema versionado e auditável (`SELECT * FROM flyway_schema_history`)
- Reprodutibilidade total entre ambientes
- CI pega quebra de migration antes de prod
- Convenção de naming legível (timestamp ordena cronologicamente)

**Negativas**:
- Rollback é manual + ADR — pode atrasar incidentes (mitigação: pattern expand-contract minimiza necessidade de rollback)
- Curva inicial: solo dev precisa internalizar regra de não editar migration aplicada
- Migrations grandes (renomear coluna, particionar tabela) viram 3-4 PRs em sequência (ADR documenta sequência)

**Riscos**:
- Esquecer de gerar migration ao mudar entity → desync schema. Mitigação: hook pre-commit detecta `@Entity` modificado sem nova `V*.sql` correspondente (futuro — adicionar ao `pre-commit-review.sh`)
- Conflito de naming quando 2 PRs criam V no mesmo minuto — improvável com 1 dev; resolver renomeando o segundo

## Impact on specs
- **versioning**: regras de migration (não editar aplicada, expand-contract, etc) detalhadas
- **data-architecture**: schema vive em `apps/api/src/main/resources/db/migration/`
- **devops**: pipeline CI roda Flyway antes de testes; deploy prod aplica migrations no boot
- **testing-strategy**: integration tests usam Testcontainers, que aplica todas migrations limpo

## References
- PRD: n/a (decisão de infra)
- Flyway docs — https://documentation.red-gate.com/fd
- ADR-002 (stack: Postgres 16, Spring Boot 3) — relacionado
- Pattern Expand-Contract — Martin Fowler

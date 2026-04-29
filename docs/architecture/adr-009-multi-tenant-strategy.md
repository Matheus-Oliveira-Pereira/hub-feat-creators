# ADR-009: Estratégia multi-tenant — shared schema com discriminator + Hibernate `@Filter`

## Status
Accepted — 2026-04-29

## Context
HUB Feat Creators é SaaS B2B onde cada **assessoria = um tenant**. Vision exige isolamento estrito: "assessorias não podem ver dados umas das outras em hipótese alguma". PRD-001 exige multi-tenant strict desde o MVP.

Modelos possíveis:
1. **Database-per-tenant** — cada tenant tem DB próprio
2. **Schema-per-tenant** — um DB, um schema por tenant
3. **Shared schema com discriminator** — uma coluna `assessoria_id` em cada tabela tenant

Critérios:
- **Custo no MVP**: Railway Postgres é por instância — um DB por tenant é inviável economicamente para SaaS de baixo ticket inicial
- **Onboarding**: criar tenant deve ser instantâneo (signup auto-serviço)
- **Operação**: migrations, backups, observabilidade simples
- **Isolamento**: vazamento cross-tenant é incidente crítico — modelo precisa ter defesa em profundidade
- **Escalabilidade futura**: tenant grande pode precisar isolamento físico depois (Fase 4)

## Decision

### Modelo: shared schema + discriminator
- Toda tabela com dado tenant tem coluna `assessoria_id UUID NOT NULL` + FK + índice composto `(assessoria_id, ...)` em todas queries quentes
- `assessoria_id` extraído do claim `ass` do JWT (ADR-008) por filtro Spring; injetado em `TenantContext` (ThreadLocal/RequestScope)

### Defesa em profundidade — três camadas

1. **Application layer — Hibernate `@Filter`**
   - Filtro global `tenant_filter` ativo em `OncePerRequestFilter` após autenticação
   - Anotação `@FilterDef(name="tenant_filter", ...)` + `@Filter(name="tenant_filter", condition="assessoria_id = :assessoriaId")` em toda entidade tenant
   - Toda query JPA automaticamente filtra por `assessoria_id`

2. **Service layer — auditoria**
   - DTOs nunca expõem `assessoria_id` em request bodies (fonte da verdade = JWT)
   - `@PreAuthorize` em endpoints com `#tenant.matches(authentication)` quando aplicável
   - Tentativa de acesso cross-tenant retorna **404** (não 403) para não vazar existência do recurso

3. **Database layer — Postgres Row-Level Security (RLS)**
   - Cada tabela tenant tem `ENABLE ROW LEVEL SECURITY` + policy `USING (assessoria_id = current_setting('app.assessoria_id')::uuid)`
   - Pool HikariCP configurado com `connectionInitSql` que aplica `SET app.assessoria_id` por checkout — ou sessão configurada por interceptor JPA
   - **Defense in depth**: se filtro Hibernate falhar (bug, query nativa), Postgres ainda nega
   - Roles: `hub_app` (regular, sujeito a RLS) vs `hub_migrator` (BYPASSRLS, só Flyway)

### Tabelas tenant
Todas exceto: `assessorias`, `flyway_schema_history`, audit log de sistema (não-tenant).

### Testes obrigatórios (PRD-001 acceptance)
- Teste E2E por endpoint: usuário do tenant A recebe **404** em recurso do tenant B
- Teste de integração: query nativa sem filtro retorna zero linhas quando session var setada para outro tenant (valida RLS)
- Teste de regressão: novo `@Entity` sem `@FilterDef` falha CI (ArchUnit)

### Onboarding e signup
- `POST /auth/signup` cria registro em `assessorias` + primeiro `usuario` OWNER em transação atômica
- `slug` único globalmente (URL-friendly: `assessoria-fulana`)

### Migration path para isolamento físico (futuro)
- Quando tenant atinge SLA crítico ou compliance exige (cliente enterprise): extrair para schema dedicado ou DB dedicado
- Modelo atual permite extração sem refactor de código de aplicação — só ops

## Alternatives considered

1. **Schema-per-tenant**
   - + isolamento natural, backup/restore granular
   - − migrations precisam rodar em N schemas (operação O(tenants) em cada deploy)
   - − connection pool fragmentado (uma conexão amarrada a um schema)
   - − criar schema em runtime (signup) exige privilégios elevados
   - Descartado para MVP; reavaliar em Fase 4 se houver demanda enterprise

2. **Database-per-tenant**
   - + isolamento máximo, blast radius mínimo
   - − custo Railway proibitivo (uma instância DB por cliente)
   - − orquestração complexa
   - Descartado

3. **Shared schema sem RLS (só `@Filter`)**
   - + simples
   - − bug em uma query nativa = vazamento cross-tenant (sem rede de segurança)
   - Descartado: defesa em profundidade é não-negociável dado o domínio (LGPD + dados pessoais)

4. **Shared schema com `@TenantId` (Hibernate 6 nativo)**
   - + integração mais limpa que `@Filter`
   - − feature recente, menos material; descobrimento dinâmico do tenant via `CurrentTenantIdentifierResolver` ainda exige boilerplate
   - Avaliar migrar para `@TenantId` quando Hibernate 7 estabilizar (refactor ~baixo)

## Consequences

**Positivas**:
- Custo MVP mínimo (uma instância Postgres)
- Onboarding instantâneo (insert em `assessorias`)
- Backup/observabilidade unificados
- RLS é rede de segurança real contra bugs de aplicação

**Negativas**:
- Coluna `assessoria_id` em quase toda tabela — ruído no schema
- Queries quentes precisam índice composto começando por `assessoria_id` (lembrete em revisão de schema)
- RLS adiciona overhead de planner Postgres (~baixo, mas medir em load test)
- Migration de tenant grande para DB dedicado é trabalho futuro

**Riscos**:
- Dev esquece `assessoria_id` em nova tabela tenant → ArchUnit + migration template `V__create_*.sql.tmpl` reduzem
- Query nativa (`@Query(nativeQuery=true)`) sem `WHERE assessoria_id` → RLS protege; revisão de PR deve flagar
- Vazamento via JOIN com tabela não-tenant — improvável mas testar; cross-tenant test cobre

## Impact on specs
- **security**: documentar três camadas, threat model multi-tenant, testes de isolamento
- **data-architecture**: convenção de coluna `assessoria_id`, índices, role `hub_app` vs `hub_migrator`, template Flyway
- **api**: endpoints derivam tenant do JWT, nunca de path/query (proibido `/api/v1/assessorias/{id}/influenciadores` — apenas `/api/v1/influenciadores`)
- **testing-strategy**: cross-tenant test obrigatório por endpoint (regra ArchUnit + Failsafe IT)
- **observability**: log com `assessoria_id` em todo request autenticado (MDC); métricas particionadas por tenant para detectar abuso
- **scalability**: índices `(assessoria_id, ...)`; particionamento futuro por `assessoria_id` quando tabelas estourarem

## References
- PRD: `docs/product/01-cadastros-mvp.md`
- ADR-002 (stack base — Postgres + Hibernate)
- ADR-008 (JWT — fonte do `assessoria_id`)
- Postgres RLS: https://www.postgresql.org/docs/16/ddl-rowsecurity.html
- Hibernate `@Filter`: https://docs.jboss.org/hibernate/orm/6.5/userguide/html_single/Hibernate_User_Guide.html#pc-filter
- Multi-tenant SaaS patterns: https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/tenancy-models

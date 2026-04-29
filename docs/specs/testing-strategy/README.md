# Module: Testing Strategy — HUB Feat Creator

Estratégia de testes para garantir qualidade e velocidade.

## Pirâmide

```
         /  E2E   \         5%  — Playwright (web)
        /----------\
       / Integration \      25% — JUnit + Testcontainers (api), Vitest c/ MSW (web)
      /----------------\
     /      Unit        \   70% — JUnit + Mockito (api), Vitest (web)
    /--------------------\
```

> Proporção é diretriz, não regra rígida. Cobertura > forma.

## Tools

| Camada | Backend (Java) | Frontend (TS) |
|--------|---------------|---------------|
| Unit | JUnit 5 + Mockito + AssertJ | Vitest + Testing Library |
| Integration | JUnit 5 + Testcontainers (Postgres real) + MockWebServer | Vitest + MSW (mock server worker) |
| E2E | (ver web) | **Playwright** (Chromium + Firefox) |
| Performance | k6 (carga API) | Lighthouse CI |
| Security | OWASP Dependency-Check + Trivy | `pnpm audit` + Trivy |
| Contract | (futuro) Pact | (futuro) Pact |
| Mutation | (futuro) PIT | — |

## Padrões

- **Naming**:
  - Java: `should<ExpectedBehavior>_when<Condition>` (ex: `shouldReturn403_whenAccessingOtherTenantResource`)
  - TS: `it("does X when Y")` em bloco `describe`
- **Estrutura**: AAA (Arrange, Act, Assert) com linhas em branco separando blocos
- **Fixtures**:
  - Java: pattern Builder (ex: `InfluenciadorFixture.umInfluenciadorAtivo().comNome("X").build()`)
  - TS: factories com `@faker-js/faker` quando precisar dado realista
- **Mocks**:
  - Java: Mockito apenas para deps externas (e-mail provider, APIs sociais); **DB nunca mockado** — Testcontainers
  - TS: MSW para HTTP; nunca mockar componentes internos (testar comportamento real)
- **Test data**:
  - Geração programática (factories) > fixtures estáticos > snapshots
  - Snapshots só em casos justificados (output de renderer, ex: e-mail HTML)

## Cobertura

| Métrica | Mínimo |
|---------|--------|
| Global (api + web) | 70% line, 60% branch |
| Delta por PR | 80% das linhas novas cobertas |
| Exclusões permitidas | DTOs sem lógica, configs Spring, `*.module.ts` Next, generated code (OpenAPI client), migrations Flyway |

Tools:
- Java: JaCoCo (relatório em `target/site/jacoco/index.html`)
- TS: c8 nativo do Vitest

> Cobertura é piso, não teto. Métrica isolada engana — review olha **o que está coberto**, não só o número.

## Tipos críticos de teste

### Multi-tenant isolation (obrigatório por endpoint)
Cada endpoint protegido tem teste:
1. User da assessoria A acessa recurso da assessoria A → 200/204
2. User da assessoria A acessa recurso da assessoria B → 403/404 (sem vazar existência)
3. User da assessoria A lista recursos → resposta contém **apenas** A

Sem esse teste, endpoint não passa em code review.

### Idempotência
Endpoints com `Idempotency-Key`:
1. Primeira chamada → cria recurso, retorna 201
2. Segunda chamada com mesma key → retorna mesma resposta, não cria duplicata

### Soft-delete
- DELETE → marca `deleted_at`, não remove
- GET por ID após delete → 404
- Listagens não retornam soft-deleted
- Job de purga existe e tem teste de unit

### Migrations Flyway
- CI roda `flyway:migrate` em DB limpo + `flyway:migrate` em DB com versão anterior
- Falha se migration quebra schema existente

## CI

| Stage | Trigger | Timeout | Required merge |
|-------|---------|---------|----------------|
| Unit api | Cada push | 10 min | Sim |
| Unit web | Cada push | 5 min | Sim |
| Integration api | Cada PR | 15 min | Sim |
| Integration web | Cada PR | 8 min | Sim |
| E2E | PR para main | 20 min | Sim |
| Performance (k6) | Release candidate | 30 min | Não (warning) |
| Security scan | Diário + PR | 10 min | Sim (CVSS ≥ 7) |
| Mutation (futuro) | Semanal | 60 min | Não |

## Ambientes de teste

| Ambiente | Dados | Uso |
|----------|-------|-----|
| Local | Testcontainers + fixtures | Desenvolvimento |
| CI | Containers efêmeros | Validação automatizada |
| Staging | Anonimizados de prod | Validação manual + E2E pré-prod |

## QA manual

- [x] Smoke test pré-deploy prod (5 min — login, criar prospecção, enviar e-mail)
- [x] Auditoria a11y por release (axe-core CI + manual com leitor de tela trimestral)
- [ ] Regression test pré-release (cobertura E2E automatizada deve substituir; reavaliar)
- [ ] Exploratory testing trimestral (a partir de Fase 2)

## Anti-padrões proibidos
- ❌ Mock de DB com Mockito em vez de Testcontainers
- ❌ Teste que depende de ordem de execução (`@Order`, side-effects entre testes)
- ❌ Teste que dorme com `Thread.sleep` ou `setTimeout` (usar awaitility / waitFor)
- ❌ Teste que não falha se código quebra (assertions ausentes ou genéricas demais)
- ❌ Snapshot sem revisão consciente do diff
- ❌ Comentar/skipar teste quebrado sem ticket aberto

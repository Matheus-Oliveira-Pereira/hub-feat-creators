# Module: Scalability — HUB Feat Creator

Performance, caching, filas, escalabilidade.

## Perfil de carga (projeções MVP)

| Métrica | Estimativa MVP (6 meses) | Estimativa 1 ano | 3 anos |
|---------|--------------------------|-------------------|--------|
| Assessorias ativas | 50 | 500 | 5.000 |
| Usuários totais | 200 | 2.000 | 20.000 |
| Usuários concorrentes (peak) | 30 | 200 | 1.500 |
| Requests/segundo (peak) | 20 | 100 | 800 |
| Prospecções totais armazenadas | 50k | 500k | 5M |
| E-mails enviados/dia | 500 | 5.000 | 50.000 |
| Crescimento esperado | +30%/mês inicial | +15%/mês | +5%/mês |

> Premissas devem ser revisadas a cada release maior. Métricas reais (PostHog + Grafana) substituem estimativas a partir de 3 meses pós-launch.

## Estratégia de escala

- [x] **Horizontal** (mais instâncias) — preferida
- [x] **Vertical** (Railway permite resize) — usar quando bottleneck for CPU/memória sem benefício de paralelizar
- [ ] Hybrid sharding — só se atingir > 10k assessorias

### Como escalar horizontalmente
- Backend stateless (sessões/auth via JWT, não em memória)
- Filas e cache externos (Redis)
- Connection pool dimensionado (PgBouncer quando passar de 100 conexões totais)
- Vercel escala frontend automaticamente

## Caching

| Camada | Tool | TTL | Invalidação |
|--------|------|-----|-------------|
| **CDN** | Vercel Edge (frontend estático + ISR) | imutáveis: 1 ano; ISR: por trigger | revalidação on-demand via webhook |
| **Browser** | `Cache-Control` por tipo: imagens 30d immutable; HTML no-store; API responses no-cache | — | versionamento via build hash |
| **Aplicação** | **Redis** (Upstash ou Railway addon) | varia (ver abaixo) | TTL + invalidação explícita por evento |
| **DB query cache** | Postgres shared buffers + pg_prepared statements | — | nativo |

### Caches em Redis (planejados)

| Cache | TTL | Quando invalidar |
|-------|-----|------------------|
| Sessão de usuário (resolução de token → claims) | 5 min | logout, troca de senha |
| Lookup de assessoria por slug | 1h | update de assessoria |
| Lista de templates de e-mail | 10 min | save template |
| Rate limit buckets | 1 min (janela rolling) | — |
| Idempotency-Key cache | 24h | — |
| Refresh token reuse detection | igual TTL do refresh (7d) | logout |

**Anti-padrões**:
- ❌ Cache compartilhado entre tenants sem prefixo (`assessoria:{id}:cache:...` sempre)
- ❌ Invalidar cache via job batch — preferir event-driven (LISTEN/NOTIFY ou pub/sub)

## Filas e processamento assíncrono

- **Sistema MVP**: **Spring `@Async`** + Postgres como queue (LISTEN/NOTIFY) + tabela `jobs`
- **Quando migrar**: > 1k jobs/min sustained, ou requisitos de fanout → **RabbitMQ** ou **Redis Streams**
- **Casos de uso**:
  - Envio de e-mails (sempre async)
  - Geração de relatórios (futuro)
  - Sincronização com APIs sociais (Fase 3)
  - Reindexação de busca
  - Notificações push (Fase 2)
- **DLQ**: sim — após 5 falhas, mover para tabela `jobs_dead_letter` + alerta
- **Retry**: backoff exponencial (1m, 5m, 30m, 2h, 12h)
- **Idempotência**: chave única por job (`idempotency_key` UUID); reprocesso não causa side-effect duplicado
- **Job worker**: pool dedicado dentro da própria instância api no MVP; quando volume crescer, processo separado

## Database

- **Connection pooling**: HikariCP (built-in Spring Boot) — `maximum-pool-size: 20` no MVP, ajustar com base em métricas
- **PgBouncer**: adicionar quando precisar mais que ~100 conexões totais (varia por instância Railway)
- **Read replicas**: não no MVP; avaliar quando read load > 70% e queries de relatório aparecerem
- **Sharding**: não previsto; multi-tenant em DB único é OK até dezenas de milhares de assessorias
- **Partitioning**: tabelas grandes (`emails_enviados`, `audit_log`) → particionar por `created_at` (mensal) quando > 10M linhas
- **Vacuum/Analyze**: autovacuum ON com tuning (`autovacuum_vacuum_scale_factor = 0.05` para tabelas hot)
- **Slow query log**: `log_min_duration_statement = 500ms`; revisar semanal

### Índices críticos
Documentar em ADR ao criar. Diretrizes:
- Toda FK indexada
- Índice composto inicia por `assessoria_id`
- Índice parcial `WHERE deleted_at IS NULL` para soft-deletables
- Índice em campos de filtro/ordenação (`status`, `due_date`, `created_at`)
- `EXPLAIN ANALYZE` antes de adicionar

## Rate limiting

| Endpoint | Limite | Janela | Ação |
|----------|--------|--------|------|
| `POST /auth/login`, `POST /auth/recuperar-senha` | 10 req | 1 min/IP | 429 |
| Endpoints autenticados (geral) | 600 req | 1 min/usuário | 429 |
| `POST /emails/*` | 100 req | 1 min/assessoria | 429 |
| Webhooks recebidos (futuro) | 1000 req | 1 min/origem | 429 |
| API admin (futuro) | 50 req | 1 min/usuário | 429 |

Implementação: bucket token Redis; key = `rate:{scope}:{id}:{endpoint}`.
Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.

## Performance budget (web)

| Métrica | Target |
|---------|--------|
| First Contentful Paint (FCP) | < 1.8s (p75 mobile 4G) |
| Largest Contentful Paint (LCP) | < 2.5s |
| Time to Interactive (TTI) | < 3.5s |
| Cumulative Layout Shift (CLS) | < 0.1 |
| Interaction to Next Paint (INP) | < 200ms |
| Bundle JS inicial (gzipped) | < 200 KB |
| API response p95 (endpoints normais) | < 300ms |
| API response p95 (busca/list) | < 600ms |

Lighthouse CI roda em PRs — falha se score performance < 85.

## Otimizações Next.js
- Server components default; `'use client'` só quando precisa hooks
- ISR (`revalidate`) em páginas que mudam pouco (lista de templates, planos)
- Streaming SSR via Suspense em listas longas
- `next/image` em todas imagens (lazy + AVIF/WebP)
- `next/font` para fontes (zero CLS)
- Code splitting automático por rota; `dynamic()` em modais pesados

## Otimizações backend
- Queries N+1 → JOIN ou `@EntityGraph`/projection DTO; auditor `@performance-auditor` flagga em `/spec-review`
- Pagination cursor-based para listas grandes
- Streaming de respostas grandes (export CSV) via `StreamingResponseBody`
- Compression GZip ativo no Spring (`server.compression.enabled=true`)
- Bean validation antes de hit ao DB

## Custo
- Targets MVP: < R$ 500/mês (Railway api + DB + Redis + Vercel)
- Alerta em > 1.5x do target mensal (ver `observability/`)
- Otimizações antes de upgrade: profiling de queries, redução de polling, ISR no front

## Capacity planning
Revisar trimestralmente. Ações se atingir 70% de qualquer limite:
- DB connections > 70% pool por 7d → adicionar PgBouncer ou aumentar pool
- CPU api > 70% por 7d → escalar horizontal
- Memória > 80% por 7d → profile + heap dump
- Latência p95 +50% vs baseline → investigação obrigatória

# ADR-013: Stack de Observabilidade — OTel + Prometheus + log estruturado JSON

## Status
Accepted — 2026-04-29

## Context

Todos os PRDs (001/002/003/004) prescrevem métricas, e CLAUDE.md exige spec `observability/` ativa. Falta consolidar:
- **Logs**: formato, campos obrigatórios, agregador
- **Métricas**: backend (Prometheus pull? push gateway? OTel collector?)
- **Traces**: instrumentação automática vs manual; backend
- **Alertas**: onde residem regras; canal

Sem decisão, cada feature inventa. Custo MVP precisa ser <$30/mês; runtime é Railway (api) + Vercel (web).

Restrições:
- Stack já tem `micrometer-registry-prometheus` e `opentelemetry` previstos (ADR-002)
- Solo dev no MVP — nada que exija ops 24×7
- Multi-tenant: métricas + logs precisam dimensão `assessoria_id` (com cuidado de cardinalidade)
- LGPD (ADR-011): logs sem PII raw; pseudonimização

## Decision

### Logs

- **Formato**: **JSON estruturado** (via `logstash-logback-encoder` no `apps/api`); Next.js usa `pino` no servidor (web)
- **Campos obrigatórios** todos logs api autenticados:
  - `timestamp` (ISO8601 UTC)
  - `level`
  - `logger`
  - `message`
  - `service` (`api` | `web`)
  - `env` (`dev`|`staging`|`prod`)
  - `request_id` (UUID propagado via `X-Request-Id`)
  - `assessoria_id` (MDC; vazio em endpoints públicos)
  - `usuario_id` (MDC; vazio em endpoints anônimos)
  - `trace_id` / `span_id` (correlação OTel)
- **Pseudonimização** (regra ADR-011): e-mail → `m***@dominio.com`, telefone → `+55**1234`. Util `PiiMasker` em util layer; pre-commit hook flaga raw.
- **Níveis**:
  - `ERROR`: ação humana imediata
  - `WARN`: degradação detectável (retry após 1ª tentativa, quota próxima do limite)
  - `INFO`: evento de negócio (login, criação, envio)
  - `DEBUG`: dev only; bloqueado em prod via env

### Agregador de logs (MVP)
- **Railway log stream** + retenção nativa (7 dias). Suficiente para MVP.
- Próximo upgrade: **Better Stack (Logtail)** ou **Grafana Loki** quando volume justificar (free tier de ambos cobre ~1-3GB/mês)
- Web (Next/Vercel): Vercel logs nativos; pipe a mesmo agregador via integração quando upgradar

### Métricas

- **Backend**: **Micrometer** + **Prometheus registry** expondo `/actuator/prometheus`
- **Scrape**: **Grafana Cloud free tier** (10k séries, 14 dias retenção, dashboards ilimitados) consumindo via remote_write **OU** Grafana hospeda Prometheus pull em endpoint público com basic auth
- **Decisão MVP**: começar com `remote_write` para Grafana Cloud — mais simples que expor endpoint
- **Cardinalidade**:
  - Tags por padrão: `service`, `env`, `instance`
  - **Permitido** `assessoria_id` apenas em métricas de **negócio** (ex: `prospeccoes_criadas_total`, `email_enviado_total`); cap em 1k assessorias antes de migrar p/ exemplars
  - **Proibido** `assessoria_id` em métricas de alta frequência (HTTP request rate por endpoint) — explosão
  - Nomes seguem `{dominio}_{evento}_{unidade}` (ex: `prospeccoes_criadas_total`, `email_envio_duracao_segundos`)

### Traces

- **OpenTelemetry Java agent** (autoinstrumentação) em `apps/api` — instrumenta Spring MVC, JDBC, HTTP client out-of-box
- **Backend**: **Grafana Tempo** (incluído free tier Grafana Cloud) — recebe via OTLP
- Web: `@vercel/otel` + propagação de `traceparent` cross-stack
- **Sampling**: 100% em dev, **10%** em prod (custo + sinal); always-on para erros (`tail-based` quando suportado)
- **Atributos custom**: `assessoria_id`, `usuario_id`, `prospeccao_id` em spans relevantes

### Alertas

- **Onde**: Grafana Cloud Alerting (free tier suporta ~100 regras)
- **Como código**: regras YAML em `apps/api/observability/alerts/*.yaml` + sync via Grafana provisioning ou import manual no MVP
- **Canais**: e-mail OWNER (MVP) → Slack/Discord webhook (Fase 2)
- **Alertas baseline**:
  - `api_up == 0` por > 2min → **PAGE**
  - `http_server_requests_seconds{quantile=0.95} > 0.5` por > 5min → WARN
  - `http_server_requests_seconds_count{status=~"5.."} > 5` em 5min → PAGE
  - `job_pendentes > 1000` por > 5min → WARN (ADR-010)
  - `job_mortos_total > 0` por tipo crítico (ex: `email_send`) → PAGE
  - `email_auth_falha_total{account} > 5` em 1h → WARN (PRD-004)
  - `lgpd_purga_falhou_total > 0` → PAGE
  - `disk_free_bytes < 1GB` (Railway volume — ADR-012) → PAGE

### Healthchecks

- `GET /actuator/health` (público): status agregado
- `GET /actuator/health/liveness` e `/readiness` (Railway probes)
- Indicators: db (já default), flyway, fila (count pendentes), e-mail (1+ conta ativa)

### Web (Next.js)

- **Logs**: `pino` em server components/route handlers; logs cliente NÃO enviados ao backend (sentry para erros UI em fase futura)
- **Métricas Web Vitals**: enviar para `/api/v1/metrics/web-vitals` que repassa a Micrometer (LCP/FID/CLS por rota agregada — sem PII)
- **Sentry para erros UI**: avaliar Fase 2 (free tier 5k events/mês cobre MVP)

### Custos previstos (MVP)
- Grafana Cloud Free: $0
- Sentry (Fase 2): $0 (free tier)
- Railway logs: incluso no plano da api
- **Total**: $0 incremental no MVP. Upgrade decidido por volume real.

## Alternatives considered

1. **Datadog / New Relic / Honeycomb (managed)**
   - + tudo-em-um, ótimo DX
   - − $$$ desproporcional ao MVP solo
   - Reavaliar Fase 3 com receita

2. **ELK self-hosted (Elasticsearch + Kibana)**
   - + free, controle
   - − ops pesado (ES é guloso de RAM); não justifica para MVP
   - Descartado

3. **Só Railway logs nativo, sem métricas externas**
   - + simples
   - − sem dashboards, sem alertas customizados, sem retenção longa
   - Descartado: alertas são parte do produto (saúde do envio de e-mail é crítico)

4. **Prometheus self-hosted + Grafana self-hosted**
   - + controle, free
   - − ops de Prometheus (storage, retenção, alta disponibilidade) é trabalho real
   - Descartado MVP; reavaliar se Grafana Cloud free não bastar

5. **OpenObserve / SigNoz (open-source unificado)**
   - + tudo em um, OSS
   - − ops próprio
   - Reavaliar Fase 3 se Grafana ficar caro

## Consequences

**Positivas**:
- Logs/métricas/traces correlacionáveis via `trace_id` + `request_id`
- Custo $0 no MVP (Grafana Cloud free)
- Padrão da indústria (OTel) — sem lock-in
- Alertas de negócio (queda de envio, jobs mortos) reduzem MTTR

**Negativas**:
- Setup inicial Grafana Cloud + remote_write toma ~1 dia
- 10% sampling de traces pode mascarar bug raro — compensar com 100% em erros
- Cardinalidade de métricas exige disciplina; revisão em PR

**Riscos**:
- `assessoria_id` em métrica errada explode séries → dashboard/alertas inutilizáveis. Mitigar com **lint custom** (regex em PR check) + cap dimensional em Grafana (`max_series_per_query`)
- PII em log apesar do hook (ex: stack trace de exception com input bruto) — mitigar com **`Throwable.toString()` sanitizer** em `GlobalExceptionHandler`
- Log volume alto em prod sem retenção paga → perda de evidência de incidente. Estabelecer escalada antes de exceder free tier

## Impact on specs

- **observability**: spec inteira deriva deste ADR — atualizar `docs/specs/observability/` com regras concretas
- **security**: pseudonimização de PII em logs; `Throwable` sanitizer
- **scalability**: cap de cardinalidade; sampling 10%
- **data-architecture**: campos MDC obrigatórios consumidos por todos handlers
- **api**: header `X-Request-Id` (gerar se ausente); propagar
- **devops**: provisionar Grafana Cloud; secret `GRAFANA_CLOUD_API_KEY` em Railway

## References

- ADR-002 (stack — Micrometer + OTel previstos)
- ADR-009 (multi-tenant — `assessoria_id` em MDC)
- ADR-010 (jobs — métricas de fila)
- ADR-011 (LGPD — pseudonimização de log)
- OpenTelemetry Java: https://opentelemetry.io/docs/languages/java/
- Micrometer: https://micrometer.io/
- Grafana Cloud free tier: https://grafana.com/products/cloud/
- Logback JSON: https://github.com/logfellow/logstash-logback-encoder
- pino: https://getpino.io/

# Module: Observability — HUB Feat Creator

Logs, métricas, traces, alertas.

## 3 pilares

### Logs
- **Formato**: **JSON estruturado** (Logback `logstash-encoder` no Spring; pino no Next.js futuro)
- **Níveis**: DEBUG, INFO, WARN, ERROR
- **Destino**:
  - MVP: `stdout` (Railway/Vercel coletam) → **Better Stack** ou **Grafana Loki** (Railway addon)
  - Futuro escala: vendor único (Datadog ou Grafana Cloud)
- **Retenção**: 90 dias hot, 1 ano cold (compliance + auditoria)
- **Campos obrigatórios em toda linha**:
  - `timestamp` (ISO 8601 UTC)
  - `level`
  - `service` (`api` ou `web`)
  - `traceId` (W3C Trace Context — propagado do request)
  - `spanId`
  - `assessoriaId` (quando autenticado — para filtrar por tenant em incident)
  - `userId` (quando autenticado)
  - `message`
- **Erros**: incluir `stackTrace` + `errorCode`
- **Regras**:
  - Nunca logar senha, token, header `Authorization`, body de `/auth/*`
  - PII (e-mail, telefone): pseudonimizar (hash truncado) — exceto em `audit_log` no DB
  - Sanitizar `\n`, `\r` em input externo antes de logar (anti log injection)
  - Detalhes: `docs/specs/security/#logs-e-dados-não-confiáveis`

### Métricas

- **Coleta**: **Micrometer** no Spring → exposto em `/actuator/prometheus`
- **Scrape/Storage**: Grafana Cloud (free tier MVP) ou Prometheus self-hosted
- **Dashboards**: Grafana

#### Métricas obrigatórias

**Sistema (auto via Micrometer + Spring Actuator)**:
- `http_server_requests_seconds` (histogram, by status, method, uri)
- `http_server_requests_total` (counter)
- `jvm_memory_used_bytes`, `jvm_gc_pause_seconds`
- `hikaricp_connections_*` (pool DB)
- `process_cpu_usage`

**Negócio (custom)**:
- `prospeccoes_criadas_total` (counter, by assessoria_id)
- `prospeccoes_fechadas_total` (counter)
- `prospeccoes_time_to_close_days` (histogram)
- `emails_enviados_total` (counter, by status, template)
- `tarefas_atrasadas_total` (gauge — tarefas com `due_date < now AND status != CONCLUIDA`)
- `assessorias_ativas_wau` (gauge — atualizado a cada hora)

#### SLIs / SLOs

| SLI | SLO | Janela |
|-----|-----|--------|
| Disponibilidade API | 99.5% | 30 dias rolling |
| Latência p95 endpoint padrão | < 300ms | 7 dias |
| Latência p99 endpoint padrão | < 800ms | 7 dias |
| Latência p95 envio de e-mail (queue→sent) | < 30s | 7 dias |
| Error rate 5xx | < 0.5% | 24h |
| Tempo até primeira renderização (web LCP p75) | < 2.5s | 7 dias |

Error budget: 0.5% × 30d = ~3.6h/mês. Se queimar 50% do budget em < 7d, bloquear deploys de feature até causa raiz resolvida.

### Traces

- **Sistema**: **OpenTelemetry** (OTel) → coletor → backend (Grafana Tempo ou Datadog APM)
- **Propagação**: W3C Trace Context (header `traceparent`)
- **Sample rate**:
  - Local: 100%
  - Staging: 100%
  - Produção: 10% padrão; 100% para erros (`error=true`); 100% para endpoints sensíveis (`/auth/*`, `/api/v1/admin/*`)
- **Spans obrigatórios**:
  - HTTP handler (auto pelo OTel Spring auto-instrumentation)
  - Query DB (auto via JDBC instrumentation)
  - Chamada a provedor de e-mail
  - Chamadas futuras a APIs sociais (IG, YouTube, TikTok)
  - Jobs de fila (envio e-mail, alertas tarefas)
- **Atributos custom**: `assessoria.id`, `prospeccao.id`, `template.name` (sem PII)

## Alertas

| Condição | Severidade | Canal | SLA resposta |
|----------|------------|-------|--------------|
| Error rate 5xx > 1% por 5min | Critical | PagerDuty + Slack `#oncall` | 15 min |
| Latência p99 > 1500ms por 10min | High | Slack `#oncall` | 1h |
| DB connections > 80% pool | High | Slack `#oncall` | 1h |
| Disco DB > 80% | Medium | Slack `#alerts` | 24h |
| Falha em job de fila (e-mail) > 10/min | High | Slack `#oncall` | 1h |
| Healthcheck failing 3 ciclos seguidos | Critical | PagerDuty | 15 min |
| CVE crítica em dependency | High | Slack `#security` | 7 dias (ver SLA security) |
| Vazamento cross-tenant detectado | **P0** | PagerDuty + Slack `#security-incident` | imediato |
| Backup diário falhou | High | Slack `#alerts` | 4h |
| Cost (Railway/Vercel) acima de target mensal × 1.5 | Medium | Slack `#alerts` | 24h |

PagerDuty no MVP só se houver oncall humano; senão Slack com mention no canal `#oncall`.

## Dashboards

- [x] **System Health Overview** — req/s, error rate, latency p50/p95/p99, JVM mem, DB pool
- [x] **Business Metrics** — WAU, prospecções criadas/fechadas, time-to-close, e-mails enviados
- [x] **DB Performance** — slow queries, lock waits, cache hit ratio
- [x] **Errors & Exceptions** — top errors, error rate por endpoint, recent stack traces
- [x] **Frontend Web Vitals** — LCP, FID, CLS, INP por rota (Vercel Analytics + Web Vitals API)
- [ ] **Multi-tenant Anomalies** — detecção de queries cruzando tenant (deve ser zero) — criar quando feature de auditoria estiver pronta

## Runbooks

Cada alerta crítico/high tem runbook em `docs/runbooks/`:
- `error-rate-high.md`
- `latency-high.md`
- `db-pool-exhausted.md`
- `cross-tenant-leak.md` (P0 — drop tudo, isolar incidente, comunicar afetados em 24h por LGPD)
- `email-job-failing.md`
- `healthcheck-failing.md`

Template em `docs/runbooks/_template-runbook.md`.

## Correlation
- Header `X-Trace-Id` exposto na resposta de toda chamada → usuário pode reportar erro com ID
- `traceId` aparece em logs + traces + erro de UI (toast com "código de incidente")

## Privacidade nos sinais
- Nunca enviar PII para vendor de observability sem pseudonimização
- DPA assinado com vendor (LGPD requirement) antes de habilitar coletor com dados reais

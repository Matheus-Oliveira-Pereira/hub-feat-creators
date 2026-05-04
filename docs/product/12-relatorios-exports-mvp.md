# PRD-012: Relatórios & Exports — MVP

## Context

Dashboard atual (i9) mostra KPIs em tempo real. Falta: relatórios filtrados por período/assessor/marca, comparativos mês-a-mês, e export (CSV/PDF) para apresentar a sócia/cliente. Sem isso, dado fica no sistema mas não vira artefato compartilhável.
Vision: Fase 2 — maturação dados.
Depende de: PRDs 01-04, 10 (eventos como fonte).

## Objective

Oferecer relatórios pré-definidos (funil prospecção, performance assessor, e-mail/WA performance, tarefas SLA) com filtros, drill-down e export CSV/PDF — usando agregações sobre tabelas operacionais e/ou eventos.

## Scope

### Includes
- [ ] **Relatório Funil de Prospecção**: por status, taxa de conversão entre etapas, tempo médio por etapa, filtros (período, assessor, marca, origem)
- [ ] **Relatório Performance Assessor**: prospecções abertas, ganhas, perdidas, ticket médio (se valor cadastrado), tempo médio fechamento
- [ ] **Relatório E-mail Performance**: envios, taxa abertura, clique, bounce — por template, por assessor, por período
- [ ] **Relatório WhatsApp Performance**: enviados, entregues, lidos, falhados, tempo até primeira resposta
- [ ] **Relatório Tarefas SLA**: % no prazo, % atrasadas, atraso médio, por assessor
- [ ] **Filtros comuns**: período (preset + custom), assessor, marca, influenciador, tag
- [ ] **Comparativo**: período atual vs período anterior (delta absoluto + %)
- [ ] **Drill-down**: clicar em métrica → lista de registros que compõem
- [ ] **Export CSV** (linha de detalhe + cabeçalho com filtros aplicados)
- [ ] **Export PDF** (gráficos + tabelas via headless renderer)
- [ ] **Salvar relatório**: filtros + visualização nomeáveis (`relatorio_salvo`)
- [ ] **Agendar envio**: relatório salvo enviado por e-mail (PRD-004) com cron (ex: toda segunda 08:00)

### Excludes
- [ ] Self-service BI / criar relatórios do zero — Fase 3
- [ ] Conexão com Looker/Metabase — Fase 3 (export bruto basta MVP)
- [ ] Relatórios financeiros (faturamento, comissão) — fora MVP, depende módulo financeiro
- [ ] Forecast / previsão IA — Fase 3

## Not Doing

- **Cubo OLAP / data warehouse** — operacional Postgres atende MVP via materialized views.
- **Dashboards customizáveis arrastando widgets** — relatórios fixos.

## User Stories

- Como OWNER, quero ver funil do trimestre filtrado por assessor
- Como assessora, quero exportar PDF para mandar para minha sócia
- Como OWNER, quero relatório semanal automático no e-mail toda segunda

## Acceptance Criteria

- [ ] **AC-1**: Cada relatório tem endpoint `/api/v1/relatorios/{nome}` aceitando query params padronizados (`from`, `to`, `assessor_id`, ...)
- [ ] **AC-2**: Drill-down retorna IDs/links para entidades reais (respeita RBAC visibility)
- [ ] **AC-3**: Export CSV: cabeçalho com filtros aplicados em comentário (`# from=...`); UTF-8 BOM
- [ ] **AC-4**: Export PDF: layout A4 com logo, título, filtros, gráfico (renderizado server-side via Recharts → image), tabela
- [ ] **AC-5**: Período custom validado: `from <= to`, máx 24 meses
- [ ] **AC-6**: Materialized views refresh diário 03:00 e on-demand pós-mudança crítica (debounced)
- [ ] **AC-7**: Cross-tenant: querystring com `assessor_id` de outra assessoria → 404
- [ ] **AC-8**: Agendamento: cron string validada; envio respeita timezone tenant
- [ ] **AC-9**: Falha no envio agendado: retry + alerta in-app (PRD-009) ao OWNER
- [ ] **AC-NF-1**: Latência relatório p95 < 1s em dataset de 100k prospecções
- [ ] **AC-NF-2**: Export PDF p95 < 5s
- [ ] **AC-NF-3**: Cobertura ≥ 75% em `RelatorioService`, agendador

## Technical Decisions

- **Materialized views**: `mv_funil_prospeccao_diario`, `mv_email_perf_diario`, `mv_wa_perf_diario`, `mv_tarefa_sla_diario` — refresh `CONCURRENTLY` em job
- **Renderer PDF**: headless Chromium (Playwright Java) renderiza rota web `/print/relatorio/X` com query
- **Filtros padronizados**: classe `RelatorioFiltro` com bind Spring; validação cross-field (`from <= to`)
- **Agendamento**: tabela `relatorio_agendamento(id, relatorio_salvo_id, cron, destinatarios, ativo)`; scheduler único por instância (lock advisory Postgres)

### Schema

```sql
relatorios_salvos (id, assessoria_id, usuario_id, nome, relatorio_tipo, filtros JSONB,
  visualizacao JSONB, created_at, updated_at)

relatorio_agendamentos (id, relatorio_salvo_id FK, cron TEXT, timezone TEXT, destinatarios TEXT[],
  formato CHECK ('CSV','PDF'), ativo BOOL, ultima_execucao, proxima_execucao, criado_por_id, created_at)

-- materialized views
CREATE MATERIALIZED VIEW mv_funil_prospeccao_diario AS
SELECT assessoria_id, date_trunc('day', updated_at) AS dia, status,
       count(*) AS qtd, ...
FROM prospeccao
GROUP BY 1,2,3;

CREATE INDEX ... ON mv_funil_prospeccao_diario (assessoria_id, dia);
```

## Impact on Specs

- **Compliance**: relatório com PII respeita anonimização (titulares anonimizados aparecem como "Anônimo #X")
- **Security**: agendamento envia para destinatários — validar e-mails contra opt-out (PRD-007)
- **Scalability**: MVs refresh CONCURRENTLY para não bloquear leitura; particionar quando > 50M linhas
- **Observability**: `relatorio_executado_total{nome}`, `relatorio_export_total{formato}`, latência por relatório
- **API**: `/api/v1/relatorios/*`, `/api/v1/relatorios-salvos`, `/api/v1/agendamentos`
- **Testing**: snapshot de output CSV/PDF; teste de timezone em agendamento (BR sem DST mas dataset multi-zone)

## Rollout

- **Feature flag**: `feature.relatorios.advanced`
- **Migrations**: V0012_*; refresh inicial das MVs em janela
- **Rollback**: flag off; KPIs simples (i9) seguem disponíveis

## Métricas

- ≥ 50% OWNERS usam ao menos 1 relatório por mês
- ≥ 30% têm 1 agendamento ativo
- Tempo p95 < 1s em 95% das execuções

## Open questions

- [ ] PDF com Playwright pesado em Railway? — fallback Apache PDFBox simples sem gráficos se memória apertar
- [ ] Compartilhar relatório por link público (sem login)? — não no MVP (LGPD)

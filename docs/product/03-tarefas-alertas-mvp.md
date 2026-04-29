# PRD-003: Tarefas + Alertas — MVP

## Context

Prospecção (PRD-002) registra **onde** está cada negociação, mas não responde **o que fazer hoje**. Assessora opera em modo "lembro do que tem que fazer" — frágil, escala mal além de 30 contas ativas. Hoje: post-its, lembretes no celular, planilha de "pendências".

Sem tarefas estruturadas:
- Follow-ups esquecidos viram leads perdidos (impacto direto na métrica time-to-close)
- Donas de assessoria não conseguem distribuir trabalho sem cair em microgerência via WhatsApp
- Nada lembra a assessora do que vence hoje quando ela abre o sistema

Vision: `docs/product/vision.md` — Fase 1.
Depende de: [[01-cadastros-mvp]] (usuário, influenciador, marca), [[02-prospeccao-mvp]] (vínculo opcional).

## Objective

Permitir que assessora crie, atribua e acompanhe tarefas com prazo, vinculadas (opcionalmente) a entidades do sistema, recebendo lembretes in-app e digest diário por e-mail das tarefas que vencem hoje ou estão atrasadas.

## Scope

### Includes
- [ ] CRUD de **tarefa** com: título, descrição (markdown leve), prazo (data + opcional hora), prioridade (`BAIXA|MEDIA|ALTA|URGENTE`), status (`TODO|EM_ANDAMENTO|FEITA|CANCELADA`), responsável (FK usuário), criador (FK usuário)
- [ ] **Vínculo opcional** com **uma** entidade: prospecção, influenciador, marca, contato (FK polimórfica via `entidade_tipo` + `entidade_id`)
- [ ] Listagem com filtros: status, responsável, prioridade, prazo (vencidas / hoje / esta semana / futuras), entidade vinculada
- [ ] Visões: **lista** (default), **agenda** (semanal — segunda a domingo)
- [ ] **Minhas tarefas** (atalho com filtro responsável = usuário atual)
- [ ] Marcar como feita: clique único; registra `concluida_em`
- [ ] **Alertas in-app**: badge no menu com contagem de "vencidas + hoje"; toast quando prazo entra em "hoje"
- [ ] **Digest diário por e-mail**: enviado às 08:00 (timezone da assessoria, default `America/Sao_Paulo`) com tarefas vencidas + tarefas de hoje + próximas 3 da semana, agrupado por responsável
- [ ] Soft-delete; audit log (reusa PRD-001)
- [ ] Comentários simples na tarefa (autor + timestamp + texto, imutáveis)

### Excludes
- [ ] Tarefa atribuída ao influenciador — Fase 2 (depende de PRD-005 portal mobile)
- [ ] Recorrência (diária/semanal/mensal) — Fase 1.1 se houver tempo; complexidade de UI + jobs adia
- [ ] Subtarefas/checklist — Fase 2
- [ ] Anexos em tarefa — Fase 2
- [ ] Notificação push (web push / mobile) — Fase 2 (mobile entra com PRD-005)
- [ ] Notificação WhatsApp — Fase 2 (PRD-006)
- [ ] Calendário externo (sync Google/Outlook) — Fase 3
- [ ] Auto-criação de tarefa por evento de prospecção — Fase 2 (rules engine)
- [ ] Tempo trabalhado (timesheet) — fora do produto

## Not Doing (and why)

- **Recorrência** — UI + scheduler aumenta surface de bugs (timezone, DST, "última terça do mês"). Workaround: usuário cria nova tarefa após concluir; reabrir só se houver demanda forte.
- **Subtarefas** — modelagem hierárquica + UI de árvore atrasa MVP. Workaround: descrição markdown com checklist `- [ ]`.
- **Auto-criação por regra** — features de automação são produto separado em SaaS B2B; sem PMF não vale o investimento.
- **Notificação push/WhatsApp** — coberto por PRDs específicos (mobile + WhatsApp); aqui o canal é in-app + e-mail digest.
- **Tempo trabalhado** — assessoria não cobra por hora (cobra % de campanha); irrelevante.

## User Stories

- **Como** assessora, **quero** criar tarefa com prazo e prioridade **para** não esquecer follow-up importante
- **Como** assessora, **quero** vincular tarefa a uma prospecção **para** recuperar contexto ao executar
- **Como** dona da assessoria (OWNER), **quero** atribuir tarefa a outra assessora **para** distribuir trabalho sem WhatsApp
- **Como** assessora, **quero** ver atalho "Minhas tarefas" **para** começar o dia sabendo o que fazer
- **Como** assessora, **quero** receber digest diário por e-mail **para** revisar pendências antes de abrir o sistema
- **Como** assessora, **quero** ver badge no menu com contagem de vencidas **para** ser puxada de volta às pendências
- **Como** assessora, **quero** marcar tarefa feita com um clique **para** não perder fluxo

## Design

- **Flow A — Claude Design**. Tokens em `docs/specs/design-system/`.
- Telas:
  - **Lista** (table com inline-edit de status via checkbox e prazo via picker)
  - **Agenda semanal** (7 colunas = dias da semana; cards menores que kanban)
  - **Minhas tarefas** (lista filtrada; badge "atrasada" em vermelho)
  - **Drawer de detalhe** (descrição + comentários + entidade vinculada com link)
  - **Form de criação rápida** (modal com mínimos: título + prazo + responsável)
- Componente reusado: badge de prioridade (cores por nível) — vai para `docs/specs/design-system/`.

## Acceptance Criteria

### Funcional
- [ ] **AC-1**: Usuária cria tarefa com título e prazo obrigatórios; demais opcionais
- [ ] **AC-2**: Atribuir a outro usuário só permitido a OWNER ou ao próprio (auto-atribuição). ASSESSOR não pode atribuir a colega no MVP
- [ ] **AC-3**: Mudar status para `FEITA` registra `concluida_em` automaticamente; reabrir (`FEITA → TODO`) limpa o campo
- [ ] **AC-4**: Vínculo a entidade valida tipo + existência + mesma assessoria; vínculo cross-tenant retorna 404
- [ ] **AC-5**: Listagem multi-tenant strict (ADR-009)
- [ ] **AC-6**: Filtro "vencidas" retorna tarefas com `prazo < now()` AND status NOT IN (`FEITA`,`CANCELADA`)
- [ ] **AC-7**: Filtro "hoje" usa timezone da assessoria
- [ ] **AC-8**: Badge no header reflete count em tempo real (poll a cada 60s ou WebSocket — implementação livre, mas atualizar ≤ 60s)
- [ ] **AC-9**: Job de digest diário roda via scheduler (Spring `@Scheduled` cron `0 0 8 * * *` por timezone) — envio assíncrono via fila de e-mail (PRD-004)
- [ ] **AC-10**: Digest agrupa por responsável; só envia para usuários com ≥ 1 tarefa relevante; respeita opt-out (preferência do usuário)
- [ ] **AC-11**: Comentários na tarefa são imutáveis (sem PATCH/DELETE no MVP)
- [ ] **AC-12**: Audit log registra criação, edição, mudança de status, atribuição, deleção

### Não-funcional
- [ ] **AC-NF-1**: Latência p95 < 300ms em listagem com até 2000 tarefas
- [ ] **AC-NF-2**: Job de digest processa 100 assessorias em < 5min sem bloquear request thread
- [ ] **AC-NF-3**: Idempotência do digest: rerun no mesmo dia para mesma assessoria não duplica envio (chave: `assessoria_id + data + tipo=digest`)
- [ ] **AC-NF-4**: Cross-tenant test em todos endpoints
- [ ] **AC-NF-5**: WCAG 2.1 AA — checkbox marcar feito acessível via teclado
- [ ] **AC-NF-6**: Cobertura ≥ 80% em controllers + services + scheduler

## Technical Decisions

- **Reusa**:
  - [[adr-001-monorepo]], [[adr-002-stack-base]], [[adr-003-flyway-migrations]]
  - [[adr-008-auth-jwt]] (auth), [[adr-009-multi-tenant-strategy]] (`assessoria_id` em todas tabelas + RLS)
  - [[adr-005-email-smtp-multi-conta]] + [[adr-010-async-jobs-postgres-queue]] (digest enfileira `EMAIL_DIGEST` no `job` Postgres)
  - [[adr-011-lgpd-baseline]] (retenção tarefas + comentários; pseudonimização em log)
  - [[adr-013-observability-stack]] (métricas + alerta de digest falho)
- **Scheduler**: Spring `@Scheduled` em `apps/api` com **ShedLock** (Postgres lock provider — formalizado em ADR-010); cron por timezone da assessoria
- **Vínculo polimórfico**: `entidade_tipo` (enum) + `entidade_id` (UUID, sem FK declarada). Validação em service layer; preferir simplicidade a tabela-por-tipo no MVP
- **Timezone**: armazenar prazos em UTC; converter na borda usando `assessorias.timezone` (default `America/Sao_Paulo`)

### Schema

```sql
tarefas (
  id UUID PK,
  assessoria_id UUID FK,
  titulo TEXT NOT NULL,
  descricao TEXT NULL,
  prazo TIMESTAMPTZ NOT NULL,
  prioridade TEXT NOT NULL CHECK (prioridade IN ('BAIXA','MEDIA','ALTA','URGENTE')) DEFAULT 'MEDIA',
  status TEXT NOT NULL CHECK (status IN ('TODO','EM_ANDAMENTO','FEITA','CANCELADA')) DEFAULT 'TODO',
  responsavel_id UUID FK → usuarios(id),
  criador_id UUID FK → usuarios(id),
  entidade_tipo TEXT NULL CHECK (entidade_tipo IN ('PROSPECCAO','INFLUENCIADOR','MARCA','CONTATO')),
  entidade_id UUID NULL,
  concluida_em TIMESTAMPTZ NULL,
  created_at, updated_at, deleted_at
)

tarefa_comentarios (
  id UUID PK,
  tarefa_id UUID FK,
  assessoria_id UUID FK,
  autor_id UUID FK,
  texto TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

usuario_preferencias (
  usuario_id UUID PK FK → usuarios(id),
  digest_diario_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at TIMESTAMPTZ
)
```

Índices:
- `(assessoria_id, responsavel_id, status, prazo)` para "minhas tarefas + filtro vencidas"
- `(assessoria_id, prazo, status)` para digest diário
- `(assessoria_id, entidade_tipo, entidade_id)` para listar tarefas de uma prospecção
- `(tarefa_id, created_at desc)` em comentários

## Impact on Specs

- **Compliance**: nada novo (sem dado pessoal além do já tratado)
- **Security**: regra de atribuição (AC-2) é controle de autorização — testar
- **Scalability**: scheduler distribuído via ShedLock; digest é batch noturno-equivalente (08:00 BRT)
- **Observability**: métricas `tarefas_criadas_total`, `tarefas_concluidas_total`, `digest_enviado_total`, `digest_falha_total`; gauge `tarefas_vencidas{assessoria_id}`
- **Accessibility**: checkbox + teclado; cores de prioridade não dependem só de cor (icon + texto)
- **API**: `/api/v1/tarefas` + `/api/v1/tarefas/me` (atalho)

## Rollout

- **Feature flag**: `feature.tarefas.enabled` (assessoria-level)
- **Data migration**: schema novo
- **Rollback**: flag off; jobs scheduled checam flag antes de processar
- **Onboarding**: pós-deploy, criar 3 tarefas-exemplo na primeira sessão de cada assessoria (descartáveis) para mostrar valor — adia se travar release

## Métricas de sucesso

- ≥ 60% das assessoras criam ≥ 5 tarefas na primeira semana
- ≥ 50% abrem o digest diário (open rate via PRD-004 tracking)
- Redução de "prospecção parada > 14 dias sem ação" — comparar 30d pré vs 30d pós
- Zero incidente de digest enviado em duplicidade

## Open questions

- [ ] Notificação in-app é poll (60s) ou WebSocket? — começar com poll; WS se latência incomodar
- [ ] Hora default do prazo quando usuária só escolhe data? — `23:59` (fim do dia) — definir antes de implementar
- [ ] Digest agrupa atrasadas há quanto tempo? — propor "qualquer atrasada" no MVP; truncar a 20 itens com link "ver mais"
- [ ] ASSESSOR pode reatribuir tarefa que recebeu? — sim, devolver ao OWNER; não a outro ASSESSOR (evitar pingue-pongue)

# PRD-002: Prospecção — MVP

## Context

Cadastros (PRD-001) substituem planilhas, mas sem **prospecção** o sistema é só agenda — não captura o trabalho-fim da assessora: vender campanhas. Hoje, prospecção vive em planilhas paralelas, e-mails dispersos e cabeças de assessoras. Resultado: leads quentes esfriam, follow-ups perdem timing, ninguém sabe **onde** cada negociação está.

Sem prospecção estruturada, não há funil, não há métrica de conversão, não há "time-to-close" — métrica de sucesso definida na vision.

Vision: `docs/product/vision.md` — Fase 1.
Depende de: [[01-cadastros-mvp]] (marca, influenciador, contato, assessor).

## Objective

Permitir que assessora crie, acompanhe e feche oportunidades de campanha vinculando **marca + influenciador + assessor responsável**, com status fixo, histórico de eventos e métricas básicas de funil — substituindo planilhas de pipeline.

## Scope

### Includes
- [ ] CRUD de **prospecção** com campos: título, marca (FK), influenciador candidato (FK opcional), assessor responsável (FK), valor estimado (BRL), data de abertura, próxima ação (texto + data), observações, tags, motivo de perda (enum)
- [ ] **Status enum fixo** com transições válidas:
  - `NOVA → CONTATADA`
  - `CONTATADA → NEGOCIANDO | FECHADA_PERDIDA`
  - `NEGOCIANDO → FECHADA_GANHA | FECHADA_PERDIDA`
  - `FECHADA_PERDIDA → NOVA` (reabrir; registrar evento)
  - `FECHADA_GANHA` é terminal
- [ ] Listagem com filtros: status, assessor, marca, influenciador, tag, range de data, range de valor
- [ ] Visão **Kanban** read-only por status (drag-and-drop dispara mudança de status, validada pelo backend)
- [ ] Visão **lista** com paginação cursor-based + ordenação por valor / data abertura / próxima ação
- [ ] Detalhe da prospecção com **timeline de eventos** (mudanças de status, comentários, e-mails enviados — integração lazy com PRD-004 quando disponível)
- [ ] Comentários internos (texto livre) no detalhe — autor + timestamp
- [ ] Soft-delete (`deleted_at`)
- [ ] Audit log (reusa tabela do PRD-001)
- [ ] Exportação CSV da listagem filtrada (LGPD portabilidade)
- [ ] Métricas no dashboard: total ativas, total fechadas (mês), taxa de conversão por status, time-to-close médio (dias entre `NOVA` e `FECHADA_GANHA`)

### Excludes
- [ ] Status customizáveis por assessoria — fora do MVP; reavaliar após PMF
- [ ] Múltiplos influenciadores por prospecção — modelagem 1:N adia para Fase 2 (campanha pode ter múltiplos creators)
- [ ] Anexos/contratos na prospecção — Fase 2
- [ ] Sequências automáticas de follow-up — coberto por PRD-004 (e-mail) em fase futura
- [ ] Integração com calendário externo (Google/Outlook) — Fase 3
- [ ] Forecast / previsão de receita — Fase 3
- [ ] Análise de coorte / cohort report — Fase 3
- [ ] Notificação de prospecção parada (sem mudança X dias) — coberto por PRD-003 (alertas)

## Not Doing (and why)

- **Status customizáveis** — adia complexidade (regras de transição, migrations por tenant). Enum fixo permite métricas comparáveis cross-tenant e funil consistente. Reabrir só após PMF e demanda real.
- **Múltiplos influenciadores por prospecção** — campanha real pode ter N creators, mas modelagem M:N triplica complexidade de UI/relatório no MVP. 1:1 cobre 80% dos casos iniciais (assessora pequena/média).
- **Forecast de receita** — exige histórico de pelo menos 6 meses + modelo estatístico; sem dado, é chute caro de construir.
- **Notificação de prospecção parada** — funcionalidade de alerta pertence ao módulo de tarefas (PRD-003), evitando duplicação de canal de notificação.
- **Importação de planilha existente** — utilidade alta mas complexidade de parsing (formatos heterogêneos por assessoria) atrasa release; oferecer migração assistida 1-a-1 no onboarding inicial.

## User Stories

- **Como** assessora, **quero** criar uma prospecção vinculada a marca + (opcionalmente) influenciador candidato **para** registrar oportunidade no momento que ela surge
- **Como** assessora, **quero** ver meu pipeline em kanban **para** identificar visualmente onde está cada negociação
- **Como** assessora, **quero** mover prospecção entre status com drag-and-drop **para** atualizar pipeline em segundos
- **Como** assessora, **quero** registrar próxima ação com data **para** não esquecer follow-up
- **Como** dona da assessoria (OWNER), **quero** filtrar pipeline por assessor **para** acompanhar performance da equipe
- **Como** assessora, **quero** registrar motivo de perda ao fechar perdida **para** entender padrões e ajustar abordagem
- **Como** assessora, **quero** ver timeline de eventos da prospecção **para** retomar contexto rápido após dias sem mexer
- **Como** dona da assessoria, **quero** exportar pipeline em CSV **para** apresentar relatório a sócio ou cliente final

## Design

- **Flow A — Claude Design**. Sem PROMPT.md ainda; tokens em `docs/specs/design-system/` orientam UI inicial.
- Telas envolvidas:
  - **Lista** (table) com filtros laterais e busca textual em título
  - **Kanban** (5 colunas = 5 status; cards com título, marca, valor, próxima ação)
  - **Detalhe** (drawer lateral com tabs: dados / timeline / comentários)
  - **Form** (single column; campos condicionais — motivo de perda só visível se status = `FECHADA_PERDIDA`)
- Mobile-friendly: kanban vira lista colapsável com filtro por status (assessoras podem revisar pipeline no celular).

## Acceptance Criteria

### Funcional
- [ ] **AC-1**: Usuária cria prospecção com marca obrigatória; influenciador, valor e tags opcionais. Status default = `NOVA`
- [ ] **AC-2**: Transição de status só aceita transições válidas listadas em Scope. POST com transição inválida retorna 422
- [ ] **AC-3**: Mover card no kanban via drag-and-drop dispara PATCH e UI reflete (otimista com rollback se 422)
- [ ] **AC-4**: Fechar como `FECHADA_PERDIDA` exige `motivo_perda` (enum: `SEM_FIT`, `ORCAMENTO`, `TIMING`, `CONCORRENTE`, `SEM_RESPOSTA`, `OUTRO` + texto livre se `OUTRO`)
- [ ] **AC-5**: Listagem retorna apenas prospecções da assessoria autenticada (ADR-009 multi-tenant)
- [ ] **AC-6**: Detalhe mostra timeline com mudanças de status (de → para, autor, timestamp) e comentários ordenados por data
- [ ] **AC-7**: Filtro combinado funciona (ex: status=`NEGOCIANDO` AND assessor=X AND valor≥1000)
- [ ] **AC-8**: Soft-delete: registro some das listagens, permanece com `deleted_at` setado; tentativa de PATCH em deletado retorna 404
- [ ] **AC-9**: Audit log registra criação, edição (com diff), mudança de status (campo dedicado), e deleção
- [ ] **AC-10**: Export CSV retorna prospecções não-deletadas com filtros aplicados; máx 10k linhas por export (paginar acima)
- [ ] **AC-11**: Dashboard exibe contadores e taxa de conversão calculados em tempo real para a assessoria atual

### Não-funcional
- [ ] **AC-NF-1**: Latência p95 < 300ms em listagem com até 5000 prospecções
- [ ] **AC-NF-2**: Kanban renderiza < 500ms com até 200 cards visíveis (paginar/virtualizar acima)
- [ ] **AC-NF-3**: Cross-tenant test: usuário tenant A recebe **404** em recurso de B (ADR-009)
- [ ] **AC-NF-4**: WCAG 2.1 AA — kanban acessível via teclado (mover card via setas + atalho); axe-core sem violações `serious`
- [ ] **AC-NF-5**: Cobertura ≥ 80% em controllers + services do módulo
- [ ] **AC-NF-6**: OpenAPI spec gerada e validada via Spectral

## Technical Decisions

- **Reusa**: [[adr-001-monorepo]], [[adr-002-stack-base]], [[adr-003-flyway-migrations]], [[adr-008-auth-jwt]], [[adr-009-multi-tenant-strategy]], [[adr-011-lgpd-baseline]] (retenção/soft-delete), [[adr-013-observability-stack]] (métricas + MDC)
- **Não exige novo ADR**: enum de status é detalhe de domínio; máquina de estados validada em service layer (Java enum + matriz de transições). Documentar no spec do módulo.

### Schema (alto nível — refinar em implementação)

```sql
prospeccoes (
  id UUID PK,
  assessoria_id UUID FK → assessorias(id),
  marca_id UUID FK → marcas(id),
  influenciador_id UUID FK → influenciadores(id) NULL,
  assessor_id UUID FK → usuarios(id),
  titulo TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('NOVA','CONTATADA','NEGOCIANDO','FECHADA_GANHA','FECHADA_PERDIDA')),
  valor_estimado_centavos BIGINT NULL,
  proxima_acao TEXT NULL,
  proxima_acao_em DATE NULL,
  observacoes TEXT NULL,
  tags TEXT[] NOT NULL DEFAULT '{}',
  motivo_perda TEXT NULL,                  -- enum + texto livre se OUTRO
  motivo_perda_detalhe TEXT NULL,
  fechada_em TIMESTAMPTZ NULL,
  created_at, updated_at, deleted_at,
  created_by UUID FK
)

prospeccao_eventos (
  id UUID PK,
  prospeccao_id UUID FK → prospeccoes(id),
  assessoria_id UUID FK,                   -- denormalizado para multi-tenant
  tipo TEXT NOT NULL CHECK (tipo IN ('STATUS_CHANGE','COMMENT','EMAIL_SENT','TASK_LINKED')),
  payload JSONB NOT NULL,                  -- conteúdo varia por tipo
  autor_id UUID FK → usuarios(id) NULL,    -- null = sistema
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)
```

Índices:
- `(assessoria_id, deleted_at)` em `prospeccoes`
- `(assessoria_id, status, deleted_at)` para kanban
- `(assessoria_id, assessor_id, deleted_at)` para filtro por dono
- `(assessoria_id, proxima_acao_em)` para alertas (PRD-003 consome)
- GIN em `tags`
- Trigram em `titulo` para busca
- `(prospeccao_id, created_at desc)` em `prospeccao_eventos`

## Impact on Specs

- **Compliance**: dados pessoais (nome do contato, e-mail no histórico) — base legal "execução de contrato" + "legítimo interesse" da assessoria. Nada novo além do PRD-001.
- **Security**: surface = endpoints `/api/v1/prospeccoes/**`; multi-tenant strict via ADR-009; transição de status valida no backend (não confiar em client).
- **Scalability**: índices compostos começando por `assessoria_id`; kanban paginado por status; export CSV streaming (não carregar 10k em memória).
- **Observability**: métricas `prospeccoes_criadas_total`, `prospeccoes_fechadas_total{resultado=ganha|perdida}`, `prospeccao_status_change_total{de,para}`. Histograma `time_to_close_dias`.
- **Accessibility**: kanban com teclado (setas + Enter); ARIA `aria-grabbed`/`aria-dropeffect`; foco visível.
- **i18n**: pt-BR only (vision).
- **API**: `/api/v1/prospeccoes` segue convenções `docs/specs/api/`; PATCH para mudança de status idempotente.
- **Testing**: máquina de estados testada com tabela parametrizada (todas transições válidas + amostra de inválidas).
- **Versioning**: `/api/v1`.

## Rollout

- **Feature flag**: `feature.prospeccao.enabled` (assessoria-level) — rollout gradual nas primeiras assessorias beta
- **Data migration**: schema novo, sem dados legados (assessorias chegam sem dados de prospecção; importação manual se desejado)
- **Rollback plan**: feature flag off desabilita rotas + esconde menu; dados permanecem no DB. Migration Flyway é forward-only — rollback de schema cria nova migration

## Métricas de sucesso (após release)

- ≥ 70% das assessorias ativas criam ao menos 1 prospecção em 14 dias pós-release
- ≥ 30% das assessorias usam kanban (vs lista) — adoção de UX visual
- Time-to-close médio reportado pelo dashboard (baseline para meta futura)
- Zero incidente de transição inválida aceita pelo backend
- Latência p95 dentro do SLA em load test com 100 tenants × 1000 prospecções

## Open questions

- [ ] Valor estimado: BRL fixo ou multi-moeda? — fixo BRL no MVP; multi-moeda quando entrar marca internacional
- [ ] Comentários: editáveis ou imutáveis? — imutáveis (audit-friendly); permitir delete soft pelo autor
- [ ] Reabrir `FECHADA_GANHA`? — não no MVP (terminal). Se cair, criar nova prospecção
- [ ] Quem pode editar prospecção de outro assessor? — OWNER sim; ASSESSOR só as próprias? Definir antes de implementar

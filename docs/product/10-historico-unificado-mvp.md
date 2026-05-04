# PRD-010: Histórico Unificado — MVP

## Context

Hoje cada módulo tem timeline própria (prospecção, tarefa, e-mail). Quando usuária quer ver "tudo que aconteceu com marca X", precisa abrir múltiplas telas. Decisão de produto e follow-up perdem contexto. Vision: "Histórico unificado por influenciador/marca".
Vision: Fase 2.
Depende de: PRDs 01, 02, 03, 04, 08.

## Objective

Modelar evento canônico de domínio cross-módulo e expor timeline unificada por entidade (influenciador, marca, contato, prospecção) — toda interação visível em ordem cronológica com filtros por tipo.

## Scope

### Includes
- [ ] **Tabela `evento`** central (event sourcing leve, append-only): `tipo`, `entidades_relacionadas` (jsonb com `{tipo, id}` array), `payload`, `autor_id`, `ts`
- [ ] **Tipos catalogados**:
  - Prospecção: `PROSPECCAO_CRIADA`, `PROSPECCAO_STATUS_MUDOU`, `PROSPECCAO_COMENTARIO`, `PROSPECCAO_FECHADA_GANHA`, `PROSPECCAO_FECHADA_PERDIDA`
  - Tarefa: `TAREFA_CRIADA`, `TAREFA_CONCLUIDA`, `TAREFA_REATRIBUIDA`
  - E-mail: `EMAIL_ENVIADO`, `EMAIL_ABERTO`, `EMAIL_CLICADO`, `EMAIL_BOUNCED`
  - WhatsApp: `WA_ENVIADO`, `WA_ENTREGUE`, `WA_LIDO`, `WA_RESPONDIDO`
  - Cadastro: `INFLUENCIADOR_CRIADO`, `MARCA_CRIADA`, `CONTATO_CRIADO`
- [ ] **Publicação**: domain event listeners gravam em `evento` em transação com a operação principal (outbox pattern)
- [ ] **Endpoint `/api/v1/historico?entidade_tipo=&entidade_id=&tipos=&desde=&ate=`** com paginação cursor
- [ ] **Componente web** `<Timeline>` reutilizável: agrupamento por dia, filtro de tipos via chips, expand para ver payload detalhado
- [ ] **Tab "Histórico"** em drawers de influenciador, marca, prospecção, contato (substitui timelines existentes)
- [ ] **Permissão**: visibilidade RBAC respeitada — ASSESSOR vê só eventos de suas prospecções; OWNER vê tudo
- [ ] **Pesquisa textual**: full-text em `payload->>'descricao'` (Postgres `tsvector` em pt-BR) — opcional

### Excludes
- [ ] Editar evento — append-only, exceto correção via novo evento (`EVENTO_CORRIGIDO`)
- [ ] Time travel / replay para projeções — Fase 3 se precisar
- [ ] Eventos custom criados pelo usuário — Fase 3
- [ ] Webhooks externos (saída) — PRD futuro

## Not Doing

- **Refatorar timelines existentes para excluir tabelas legadas**: timelines antigas (ex: `prospeccao_historico`) continuam como write-side legado MVP; novo `evento` é leitura unificada — migração total em Fase 2.
- **CQRS completo**: eventos não rebuildam estado; estado canônico fica nas tabelas atuais.

## User Stories

- Como assessora, quero abrir marca X e ver tudo que rolou (e-mails, WA, tarefas, prospecções)
- Como ASSESSOR, quero ver só meus eventos
- Como OWNER, quero filtrar histórico por tipo para auditar

## Acceptance Criteria

- [ ] **AC-1**: Toda mutação relevante grava `evento` na mesma transação (falha em uma → falha total)
- [ ] **AC-2**: Endpoint retorna eventos por entidade com paginação cursor (ts desc, id desc); page size default 50, máx 200
- [ ] **AC-3**: Filtro `tipos=A,B,C` aplica IN; vazio = todos
- [ ] **AC-4**: RBAC: ASSESSOR consultando entidade fora do seu escopo recebe 404 (não vaza existência)
- [ ] **AC-5**: Cross-tenant: tentativa retorna 404
- [ ] **AC-6**: Indexação `(assessoria_id, entidades_relacionadas, ts desc)` GIN para busca por entidade
- [ ] **AC-7**: Append-only: tabela sem UPDATE/DELETE em queries normais; tentativa via API retorna 405
- [ ] **AC-8**: Pesquisa textual (se ativa): `tsquery` retorna match relevante < 200ms p95 em até 1M eventos
- [ ] **AC-NF-1**: Cobertura ≥ 80% em `EventoService`, listeners
- [ ] **AC-NF-2**: Outbox pattern: se publicação síncrona falha mas operação principal sucesso, evento entra em fila de retry (`evento_outbox` table) — eventual consistency garantida

## Technical Decisions

- **Outbox pattern**: tabela `evento_outbox` recebe insert na transação; daemon publica em `evento` (ou pubsub) e marca processado — garante atomicidade sem 2PC
- **GIN índice**: em `entidades_relacionadas` JSONB para query por `(tipo, id)`
- **Listener style**: Spring `@TransactionalEventListener(phase = AFTER_COMMIT)` — só persiste evento se commit bem-sucedido
- **Schema migração**: timelines legadas (`prospeccao_historico` etc.) backfilled para `evento` em V0010_backfill — mantidas read-only durante transição

### Schema

```sql
eventos (
  id UUID PK,
  assessoria_id UUID FK,
  tipo TEXT NOT NULL,
  entidades_relacionadas JSONB NOT NULL,  -- [{"tipo":"marca","id":"..."},{"tipo":"prospeccao","id":"..."}]
  payload JSONB NOT NULL,
  autor_id UUID FK NULL,                   -- null para eventos sistêmicos
  ts TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

evento_outbox (
  id UUID PK,
  evento_payload JSONB NOT NULL,
  processado_em TIMESTAMPTZ NULL,
  tentativas INT DEFAULT 0,
  created_at
)

CREATE INDEX evt_ent_gin ON eventos USING GIN (entidades_relacionadas);
CREATE INDEX evt_assessoria_ts ON eventos (assessoria_id, ts DESC);
CREATE INDEX evt_assessoria_tipo_ts ON eventos (assessoria_id, tipo, ts DESC);
```

## Impact on Specs

- **Compliance**: payload pode conter PII — sujeito a anonimização DSR (PRD-007); job atualiza eventos com referência ao titular anonimizado
- **Security**: append-only protegido por permissão DB (role app não tem `DELETE`)
- **Scalability**: particionar `eventos` por mês quando passar 50M; arquivar > 5 anos
- **Observability**: `evento_publicado_total{tipo}`, `evento_outbox_pendente`, alerta outbox > 100
- **API**: `/api/v1/historico`, `/api/v1/historico/search`
- **Testing**: testes garantem que cada mutação cobre evento correspondente (pact-style por tipo)

## Rollout

- **Feature flag**: `feature.historico.unified` — em on, drawer mostra unified; em off, mostra timeline antiga
- **Migrations**: V0010_*; backfill rodado em janela de manutenção
- **Rollback**: flag off; eventos continuam sendo gravados (não derruba)

## Métricas

- ≥ 70% das aberturas de drawer usam tab Histórico em 30d
- Latência endpoint p95 < 250ms em 1M eventos
- Outbox pendência mediana < 100ms

## Open questions

- [ ] Eventos podem ter > 1 tenant? — não, sempre `assessoria_id` único
- [ ] Pesquisa textual ativa no MVP? — opcional via flag, default off (custo CPU)
- [ ] Deduplicação de eventos idênticos consecutivos (ex: status mudou A→B→A)? — gravar tudo; UI agrupa visualmente

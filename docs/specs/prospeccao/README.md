# Module: Prospecção (PRD-002)

Pipeline de oportunidades marca↔influenciador com state machine fixa, eventos imutáveis, métricas e visibilidade row-level.

## State machine

Implementada em `ProspeccaoStateMachine.java` (backend) e `lib/prospeccao.ts` (frontend, espelhada).

```
NOVA              → CONTATADA
CONTATADA         → NEGOCIANDO | FECHADA_PERDIDA
NEGOCIANDO        → FECHADA_GANHA | FECHADA_PERDIDA
FECHADA_PERDIDA   → NOVA  (reabrir)
FECHADA_GANHA     → ∅     (terminal)
```

Transição inválida → HTTP `422` (`BusinessException.unprocessable("TRANSICAO_INVALIDA", …)`).
Status idêntico ao atual → `422` (`STATUS_INALTERADO`).

### Regras condicionais

- **`FECHADA_PERDIDA`** exige `motivoPerda` (enum `SEM_FIT | ORCAMENTO | TIMING | CONCORRENTE | SEM_RESPOSTA | OUTRO`).
- Se `motivoPerda = OUTRO` → `motivoPerdaDetalhe` obrigatório.
- Reabrir `FECHADA_PERDIDA → NOVA` limpa `motivoPerda`, `motivoPerdaDetalhe` e `fechadaEm`.
- Fechar (`*_GANHA` ou `*_PERDIDA`) seta `fechadaEm = now()`.

## Endpoints

| Método | Path | Roles | Descrição |
|---|---|---|---|
| GET | `/api/v1/prospeccoes` | `BPRO` | Listagem paginada com filtros (`status`, `assessorId`, `marcaId`, `nome`, `page`, `size`). |
| GET | `/api/v1/prospeccoes/dashboard` | `BREL` | Counts por status + fechadas mês + taxa conversão + time-to-close médio. |
| GET | `/api/v1/prospeccoes/{id}` | `BPRO` | Detalhe single. |
| GET | `/api/v1/prospeccoes/{id}/eventos` | `BPRO` | Timeline ordenada DESC. |
| POST | `/api/v1/prospeccoes` | `CPRO` | Criar (default status `NOVA`, dispara evento). |
| PUT | `/api/v1/prospeccoes/{id}` | `EPRO` | Atualizar (não muda status). |
| PATCH | `/api/v1/prospeccoes/{id}/status` | `EPRO` | Mudar status (state machine + motivo se perda). |
| POST | `/api/v1/prospeccoes/{id}/comentarios` | `EPRO` | Adicionar comentário imutável. |
| DELETE | `/api/v1/prospeccoes/{id}` | `DPRO` | Soft-delete (`deleted_at`). |
| GET | `/api/v1/prospeccoes/export.csv` | `EXPT` | Streaming CSV (BOM UTF-8, max 10k linhas). |

## Visibilidade (row-level)

Definida pelo PRD-005 e implementada em `ProspeccaoService.canSeeAll()` + repo split:

- **OWNER** (`Usuario.role=OWNER` ou role `OWNR` no perfil) → `repo.findAllOwner()` retorna todas da assessoria.
- **ASSESSOR** → `repo.findAllAssessor()` aplica predicate `created_by = :userId OR assessor_responsavel_id = :userId`.

Tentativa de GET/PATCH em prospecção fora desse escopo → `404 PROSPECCAO_NOT_FOUND` (não vaza existência).

## Eventos (timeline)

Tabela `prospeccao_eventos` é **imutável** (sem update/delete via API):

- `STATUS_CHANGE` — auto na criação (`{ para }`) e em mudança de status (`{ de, para, motivo_perda?, motivo_perda_detalhe? }`).
- `COMMENT` — manual via endpoint dedicado (`{ texto }`).
- `EMAIL_SENT` — reservado pra PRD-004 integração.
- `TASK_LINKED` — reservado pra PRD-003 integração.

## Métricas (Micrometer)

| Métrica | Tipo | Tags |
|---|---|---|
| `prospeccoes_criadas_total` | Counter | — |
| `prospeccoes_fechadas_total` | Counter | `resultado` (ganha/perdida) |
| `prospeccao_status_change_total` | Counter | `de`, `para` |
| `prospeccao_time_to_close` | Timer | — (registra duração `created → FECHADA_GANHA`) |
| `auth_permission_denied_total` | Counter | `required`, `mode` (vem do RBAC aspect) |

Time-to-close médio também é exposto via endpoint `/dashboard` para UI.

## Schema

```sql
prospeccoes (
  id UUID PK, assessoria_id UUID FK, marca_id UUID FK NOT NULL,
  influenciador_id UUID FK NULL, assessor_responsavel_id UUID FK NOT NULL,
  titulo TEXT NOT NULL, status TEXT CHECK (...) DEFAULT 'NOVA',
  valor_estimado_centavos BIGINT, proxima_acao TEXT, proxima_acao_em DATE,
  observacoes TEXT, tags TEXT[] NOT NULL DEFAULT '{}',
  motivo_perda TEXT CHECK (...), motivo_perda_detalhe TEXT,
  fechada_em TIMESTAMPTZ, created_at, updated_at, deleted_at, created_by
);

prospeccao_eventos (
  id UUID PK, prospeccao_id UUID FK CASCADE, assessoria_id UUID FK CASCADE,
  tipo TEXT CHECK (...), payload JSONB NOT NULL DEFAULT '{}',
  autor_id UUID FK SET NULL, created_at TIMESTAMPTZ
);
```

Índices: `(assessoria_id, deleted_at)`, `(assessoria_id, status, deleted_at)`,
`(assessoria_id, assessor_responsavel_id, deleted_at)`, `(assessoria_id, proxima_acao_em)`,
GIN em `tags`, trigram em `titulo`.
RLS via `assessoria_id = current_setting('app.assessoria_id')` (ADR-009).

## Frontend

- `app/(app)/prospeccao/page.tsx` — toggle Kanban/Lista, filtros chips, busca debounced.
- `components/app/prospeccao-kanban.tsx` — `@dnd-kit/core` (PointerSensor + KeyboardSensor); colunas alvo iluminam quando transição é válida; drop em FECHADA_PERDIDA dispara `FecharPerdidaModal`.
- `components/app/prospeccao-detail-sheet.tsx` — Sheet drawer com tabs Dados/Timeline/Comentários.
- `components/forms/prospeccao-form-modal.tsx` — create/edit, select de marca + influenciador opcional, valor BRL formatado.
- `components/forms/fechar-perdida-modal.tsx` — motivo + detalhe condicional (zod superRefine).

## Tech debt rastreado

- **Filtro por assessor/marca/data range na UI** — backend aceita; UI só tem chips de status. Adicionar SelectMulti quando demandado.
- **Edit drag-and-drop por teclado** — `@dnd-kit` já tem `KeyboardSensor`; falta UX explícito (instructions visuais + ARIA `aria-grabbed`/`aria-dropeffect`). PRD-002 AC-NF-4.
- **Virtualização kanban** — adia até > 200 cards reais. PRD-002 AC-NF-2.
- **Diff completo no audit UPDATE** — atual loga só `{titulo}`. PRD-002 AC-9 pede diff. Considerar refactor no audit module se demanda crescer.
- **Reabrir FECHADA_GANHA** — Open question PRD: definido NÃO reabrir. Aceito.

## References
- PRD-002 `docs/product/02-prospeccao-mvp.md`
- PRD-005 `docs/product/05-rbac-mvp.md` (visibility)
- ADR-009 (multi-tenant), ADR-013 (observability), ADR-015 (RBAC)

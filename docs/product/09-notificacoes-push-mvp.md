# PRD-009: Notificações in-app + Web Push — MVP

## Context

Tarefas (PRD-003) já tem digest e alertas in-app rudimentares. Sistema cresceu: e-mail bounced, WhatsApp falhou, prospecção mudou status, mention em comentário. Sem central unificada, usuária perde eventos. Web push permite alcance fora da aba.
Vision: Fase 2 — notificações.
Depende de: PRDs 03, 04, 08; preparo para PRD-014 (mobile push).

## Objective

Centralizar eventos do sistema em notificações in-app (sino + drawer) com filtros, lidas/não-lidas, e suportar Web Push (Service Worker + VAPID) como canal opt-in. Estrutura unificada para mobile push em PRD-014.

## Scope

### Includes
- [ ] **Tabela `notificacao`**: tipo, ator, alvo, payload, prioridade, lida_em
- [ ] **Tipos MVP**: `TAREFA_VENCENDO`, `TAREFA_ATRASADA`, `EMAIL_BOUNCED`, `EMAIL_AUTH_FALHOU`, `WHATSAPP_FALHOU`, `PROSPECCAO_MUDOU_STATUS`, `MENTION_COMENTARIO`, `CONVITE_ACEITO`, `DIGEST_DIARIO`
- [ ] **Sino topbar** com badge contador não-lidas; abre drawer com lista paginada
- [ ] **Filtros drawer**: todas / não-lidas / por tipo
- [ ] **Marcar lida**: individual + "marcar todas como lidas"
- [ ] **Preferências usuário**: matriz tipo × canal (in-app sempre on; e-mail/push opt-in granular)
- [ ] **Web Push**: VAPID keypair, registro Service Worker, subscription armazenada por usuário
- [ ] **Push delivery**: enviar push para subs ativas quando notificação criada e canal habilitado
- [ ] **Real-time in-app**: SSE (`/api/v1/notificacoes/stream`) — drawer atualiza sem reload
- [ ] **Throttle**: notificações idênticas (mesmo tipo + alvo) em janela 5min agrupam
- [ ] **Digest diário** opcional (07:00 timezone tenant): resumo de eventos não-lidos do dia anterior por e-mail (reusa PRD-004)

### Excludes
- [ ] Mobile push (FCM/APNs) — PRD-014
- [ ] SMS — fora MVP
- [ ] Notificações para usuário externo (influenciador/marca) — PRD-013 portal
- [ ] Configurar regras custom (workflow) — Fase 3

## Not Doing

- **WebSocket bidirecional** — SSE é suficiente p/ broadcast server→client e mais simples (HTTP/2 + reconexão nativa).
- **Editor de regras** — eventos são fixos no MVP; expansão exige ADR.

## User Stories

- Como assessora, quero ver no sino que tarefa X está vencendo
- Como assessora, quero receber push mesmo com aba fechada
- Como OWNER, quero ser notificada quando conta SMTP cair
- Como usuária, quero desligar push de bouncing por ser ruidoso

## Acceptance Criteria

- [ ] **AC-1**: Evento de domínio (e.g. `TarefaVencendoEvent`) publica via Spring `ApplicationEventPublisher`; listener `NotificacaoFanout` cria notificação(ões) por usuário-alvo
- [ ] **AC-2**: Sino mostra contador exato de não-lidas (cache invalidado por SSE)
- [ ] **AC-3**: SSE fecha após 30min; cliente reconecta automático
- [ ] **AC-4**: Web Push subscription armazenada com `endpoint`, `keys.p256dh`, `keys.auth`; teste envia notificação real (lib `webpush-java`)
- [ ] **AC-5**: Subscription expirada (410 Gone do FCM/APNs) → marcada inativa
- [ ] **AC-6**: Throttle: 2 eventos `TAREFA_VENCENDO` para mesma tarefa em 5min → 1 notificação só, contador de "agrupadas"
- [ ] **AC-7**: Preferências: matriz default (in-app on, push off, email off exceto digest); usuário altera por tipo
- [ ] **AC-8**: Digest 07:00 só dispara se ≥ 1 não-lida; idempotente por dia
- [ ] **AC-9**: Cross-tenant: notificação só visível ao usuário do tenant correto
- [ ] **AC-NF-1**: Latência evento → push entregue p95 < 5s
- [ ] **AC-NF-2**: SSE suporta ≥ 1k conexões simultâneas em uma instância (teste de carga)
- [ ] **AC-NF-3**: Cobertura ≥ 80% em `NotificacaoFanout`, `WebPushSender`

## Technical Decisions

- **VAPID**: lib `webpush-java`; chaves em env `app.webpush.{public,private}`
- **Public key** exposta em `/api/v1/webpush/public-key`
- **SSE**: Spring `SseEmitter`; heartbeat 25s
- **Real-time fan-out**: in-process via `ApplicationEvent` MVP; Redis pubsub Fase 2 quando multi-instância
- **Throttle**: tabela `notificacao_dedupe(key, last_emitted)` com lookup curto
- **Service Worker**: `apps/web/public/sw.js` — registra push handler, abre URL do payload no click

### Schema

```sql
notificacoes (id, assessoria_id, usuario_id, tipo, prioridade CHECK ('LOW','NORMAL','HIGH'),
  titulo, mensagem, payload JSONB, alvo_tipo, alvo_id, agrupadas INT DEFAULT 1,
  lida_em, created_at)

notificacao_preferencias (usuario_id, tipo, canal CHECK ('INAPP','PUSH','EMAIL'), habilitado BOOL,
  PRIMARY KEY (usuario_id, tipo, canal))

webpush_subscriptions (id, usuario_id, endpoint UNIQUE, p256dh, auth_secret, user_agent,
  ativa, created_at, last_used_at)

notificacao_dedupe (key TEXT PK, last_emitted TIMESTAMPTZ)
```

Índices: `(assessoria_id, usuario_id, lida_em NULLS FIRST, created_at DESC)` em notificacoes.

## Impact on Specs

- **Compliance**: notificação contém PII reduzida (nome do alvo); push payload cifrado E2E pelo browser
- **Security**: VAPID assinatura, signature SSE em token JWT
- **Scalability**: SSE single-instance MVP; Redis pubsub quando escalar; índice de não-lidas
- **Observability**: `notificacao_criada_total{tipo}`, `webpush_enviado_total{status}`, `sse_conexoes_ativas`
- **Accessibility**: drawer ARIA roles, foco gerenciado, contador anunciado
- **API**: `/api/v1/notificacoes/{,stream,prefs,read}`, `/api/v1/webpush/{public-key,subscribe,unsubscribe}`

## Rollout

- **Feature flag**: `feature.webpush.enabled`
- **Migrations**: V0009_*; backfill prefs default por usuário existente
- **Rollback**: flag off mantém in-app; push fica latente

## Métricas

- ≥ 60% usuárias ativam push em 30d
- Tempo evento → push p95 < 5s
- Click-through push ≥ 25%
- < 5% notificações duplicadas (proxy: dedupe efficacy)

## Open questions

- [ ] Push para mobile reutiliza este modelo? — sim, `webpush_subscriptions` evolui para `device_subscriptions(canal)` em PRD-014
- [ ] Som/vibração configurável? — fixo no MVP

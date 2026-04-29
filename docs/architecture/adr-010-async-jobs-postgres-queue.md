# ADR-010: Jobs assíncronos com fila Postgres-backed (`SKIP LOCKED`) + ShedLock

## Status
Accepted — 2026-04-29

## Context

Múltiplos PRDs prescrevem trabalho assíncrono ou agendado:
- **PRD-003 (tarefas)**: digest diário às 08:00 BRT por assessoria; alerta in-app
- **PRD-004 (e-mail)**: envio async com retry exponencial (1m/5m/30m/2h × 4); idempotência por chave
- **PRD-001 (cadastros)**: e-mail de notificação LGPD ao criar influenciador
- **Futuro**: purga de soft-delete (LGPD), refresh token cleanup, ingestão de redes sociais (Fase 3)

Sem ADR, cada PRD escolhe mecanismo próprio → divergência, dupla observabilidade, retry inconsistente. Precisa decisão única usada por todos jobs.

Restrições:
- **Stack já decidida** (ADR-002): Spring Boot 3.3 + Postgres 16. Sem Redis no MVP (custo).
- **Multi-replica**: Railway pode ter ≥ 2 instâncias api → job agendado não pode rodar duplicado.
- **Idempotência obrigatória** (PRD-004): retry não pode reenviar.
- **Visibilidade operacional**: jobs falhando precisam métricas + alertas.
- **Custo MVP**: zero infra extra além do Postgres já existente.

## Decision

### Modelo: Postgres como fila (`SELECT ... FOR UPDATE SKIP LOCKED`)

Tabela `job` única, polimórfica via `tipo` + `payload JSONB`:

```sql
job (
  id UUID PK,
  assessoria_id UUID NULL,                     -- multi-tenant aware (NULL = job global)
  tipo TEXT NOT NULL,                          -- 'EMAIL_SEND', 'EMAIL_DIGEST', 'INFLUENCIADOR_LGPD_NOTIFY', ...
  payload JSONB NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('PENDENTE','PROCESSANDO','OK','FALHOU','MORTO')) DEFAULT 'PENDENTE',
  agendado_para TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  proxima_tentativa_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  tentativas INT NOT NULL DEFAULT 0,
  max_tentativas INT NOT NULL DEFAULT 4,
  idempotency_key TEXT NULL,
  ultimo_erro TEXT NULL,
  iniciado_em TIMESTAMPTZ NULL,
  concluido_em TIMESTAMPTZ NULL,
  worker_id TEXT NULL,                         -- hostname:pid da instância que processou
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
)

-- índice principal de polling
CREATE INDEX idx_job_pickup ON job (status, proxima_tentativa_em)
  WHERE status = 'PENDENTE';

-- idempotência scope-by-tenant (NULL allowed, NULLS NOT DISTINCT em Postgres 15+)
CREATE UNIQUE INDEX idx_job_idempotency ON job (assessoria_id, tipo, idempotency_key)
  WHERE idempotency_key IS NOT NULL;
```

### Pickup (worker pool)

Cada worker em loop:
```sql
SELECT * FROM job
 WHERE status = 'PENDENTE'
   AND proxima_tentativa_em <= NOW()
 ORDER BY proxima_tentativa_em
 LIMIT 10
 FOR UPDATE SKIP LOCKED;

-- mark PROCESSANDO + iniciado_em + worker_id, COMMIT;
-- executa handler;
-- success → status=OK, concluido_em
-- fail → tentativas++; se < max → status=PENDENTE + proxima_tentativa_em=now()+backoff; senão MORTO
```

`SKIP LOCKED` permite N workers concorrentes sem deadlock; cada job é pego por exatamente um.

### Retry policy
- Backoff exponencial: `2^tentativas * 60s` capped a 2h (1m, 4m, 16m, 64m, 120m...)
- `max_tentativas` configurável por tipo (e-mail = 4, digest = 1)
- Status `MORTO` (dead-letter): job não retentado; alerta + dashboard de "jobs mortos por tipo"

### Agendamento (cron) — ShedLock

Para jobs **periódicos** (digest diário, purga LGPD), usar **`net.javacrumbs.shedlock`** com `LockProvider` JDBC:

```java
@Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
@SchedulerLock(name = "email_digest_diario", lockAtMostFor = "10m", lockAtLeastFor = "1m")
public void enfileirarDigestDiario() {
  // por assessoria, INSERT em job com tipo=EMAIL_DIGEST
}
```

ShedLock garante que **só uma réplica** roda o `@Scheduled`. Tabela `shedlock` separada (não confundir com `job`).

### Observabilidade

Métricas Micrometer:
- `job_enfileirado_total{tipo}`
- `job_processado_total{tipo,resultado=ok|falhou|morto}`
- `job_duracao_segundos{tipo}` (histograma)
- `job_pendentes` gauge (`SELECT count(*) WHERE status='PENDENTE'`)
- `job_mortos` gauge

Alertas: `job_mortos > 0` por tipo, `job_pendentes > 1000` por > 5min (queue saturada).

### API administrativa
- `GET /api/v1/admin/jobs?status=MORTO&tipo=...` (apenas OWNER global, fora MVP)
- Reenfileirar job morto: PATCH manual via SQL no MVP; UI em fase futura.

### Limites operacionais
- Tabela `job` cresce: purgar `OK` com `concluido_em < now() - 7 days` via job recursivo
- Polling padrão: 5s entre rounds (ajustar via `app.jobs.poll-interval`)
- Pool de threads worker: configurável; default 5 threads

## Alternatives considered

1. **Spring `@Async` + `ThreadPoolTaskExecutor` (in-memory)**
   - + zero infra
   - − jobs em memória se perdem em crash/redeploy
   - − sem retry persistente, sem idempotência cross-restart
   - Descartado para PRD-004 (envio de e-mail não pode perder)

2. **Redis + Bull/RQ-like**
   - + alta performance, ecosistema maduro
   - − exige Redis no MVP (custo + ops)
   - − duas fontes de verdade (Postgres dado, Redis fila) — divergência possível em incidentes
   - Reavaliar quando volume justificar

3. **AWS SQS / Google Cloud Tasks / Railway message queue**
   - + gerenciado, durável, escalável
   - − vendor lock-in cedo demais; custo adicional pequeno mas não-zero
   - − latência de network vs Postgres local
   - Reavaliar Fase 3 se volume crescer

4. **JobRunr** (lib Java, persiste em Postgres)
   - + abstração madura, dashboard pronto
   - + retry, scheduling, recurring jobs out-of-box
   - − dependência adicional, opinion strong sobre schema (suas tabelas próprias)
   - − custo de migrar fora dela é alto se mudarmos
   - **Avaliação séria**: vale benchmark POC. Se cobrir 100% dos casos sem fricção, adotar e simplificar este ADR.
   - **Decisão pragmática MVP**: começar com tabela `job` própria (controle, sem dep nova); migrar p/ JobRunr se complexidade de jobs crescer (recurring complexos, dashboard).

5. **Quartz Scheduler**
   - + maduro, recurring/cron robusto
   - − pesado, schema próprio complexo (12+ tabelas)
   - − overkill para MVP

## Consequences

**Positivas**:
- Zero infra adicional (Postgres já é stack)
- Durabilidade: crash não perde job
- Idempotência DB-enforced (UNIQUE index)
- Multi-replica safe (`SKIP LOCKED` + ShedLock)
- Observabilidade unificada (mesmas métricas Micrometer do app)
- Migrar p/ Redis/SQS no futuro = trocar implementação, payload contracts mantidos

**Negativas**:
- Postgres como fila tem teto: ~1k jobs/s realista; acima precisa partition ou trocar de stack
- Polling 5s adiciona latência mínima (jobs "imediatos" rodam até 5s depois)
- Tabela `job` cresce; purga obrigatória
- Sem dashboard pronto (escrever ou adotar JobRunr quando relevante)

**Riscos**:
- Worker que crasha durante PROCESSANDO deixa job "stuck" — adicionar **stuck job recovery**: rollback para PENDENTE quando `iniciado_em < now() - 30min` AND status=PROCESSANDO (job watchdog)
- Idempotency key colisão entre tenants: index inclui `assessoria_id` → ok
- Long-running handler bloqueia thread do pool → handler com SLA > 1min deve quebrar em sub-jobs

## Impact on specs

- **scalability**: detalhar pool/poll/limites; alertas de saturação
- **observability**: métricas + dashboards de jobs (incluir em `docs/specs/observability/`)
- **data-architecture**: tabela `job` + `shedlock`; índices; purga
- **security**: payload JSONB **nunca contém senha/token** (referenciar por ID; resolver dentro do handler); criptografia campo-a-campo se necessário (raro)
- **testing-strategy**: integration test com Testcontainers + worker em background; assertions sobre status final do job
- **api**: convenção de retorno 202 + Location/job_id para endpoints que enfileiram

## References

- PRD-003 (tarefas + digest)
- PRD-004 (e-mail outbound)
- ADR-002 (stack base — Postgres + Spring)
- ADR-009 (multi-tenant — `assessoria_id` em `job`)
- ShedLock: https://github.com/lukas-krecan/ShedLock
- Postgres `SKIP LOCKED` pattern: https://www.2ndquadrant.com/en/blog/what-is-select-skip-locked-for-in-postgresql-9-5/
- JobRunr (avaliação futura): https://www.jobrunr.io/

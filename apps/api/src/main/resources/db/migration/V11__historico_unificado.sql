-- PRD-010: Histórico Unificado — event store append-only
CREATE TABLE eventos (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id        UUID        NOT NULL,
    tipo                 TEXT        NOT NULL,
    entidades_relacionadas JSONB     NOT NULL DEFAULT '[]',
    payload              JSONB       NOT NULL DEFAULT '{}',
    autor_id             UUID,
    ts                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX evt_ent_gin          ON eventos USING GIN (entidades_relacionadas);
CREATE INDEX evt_assessoria_ts    ON eventos (assessoria_id, ts DESC);
CREATE INDEX evt_assessoria_tipo_ts ON eventos (assessoria_id, tipo, ts DESC);
CREATE INDEX evt_autor            ON eventos (autor_id, ts DESC) WHERE autor_id IS NOT NULL;

-- Outbox placeholder para Fase 2 (eventual consistency com sistemas externos)
CREATE TABLE evento_outbox (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_payload  JSONB       NOT NULL,
    processado_em   TIMESTAMPTZ,
    tentativas      INT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

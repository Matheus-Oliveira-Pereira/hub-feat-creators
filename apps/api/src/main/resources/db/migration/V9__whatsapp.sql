-- V9: WhatsApp Outbound — PRD-008

CREATE TABLE whatsapp_accounts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id    UUID NOT NULL REFERENCES assessorias(id),
    waba_id          TEXT NOT NULL,
    phone_number_id  TEXT NOT NULL,
    phone_e164       TEXT NOT NULL,
    display_name     TEXT NOT NULL,
    access_token_enc BYTEA NOT NULL,
    token_nonce      BYTEA NOT NULL,
    app_secret_enc   BYTEA NOT NULL,
    app_secret_nonce BYTEA NOT NULL,
    status           TEXT NOT NULL DEFAULT 'ATIVO' CHECK (status IN ('ATIVO','INATIVO','ERRO')),
    daily_limit      INT NOT NULL DEFAULT 1000,
    daily_sent       INT NOT NULL DEFAULT 0,
    daily_reset_at   TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,
    UNIQUE (assessoria_id, phone_number_id)
);

CREATE TABLE whatsapp_templates (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id     UUID NOT NULL REFERENCES assessorias(id),
    account_id        UUID NOT NULL REFERENCES whatsapp_accounts(id),
    nome              TEXT NOT NULL,
    idioma            TEXT NOT NULL DEFAULT 'pt_BR',
    categoria         TEXT NOT NULL CHECK (categoria IN ('MARKETING','UTILITY','AUTHENTICATION')),
    corpo             TEXT NOT NULL,
    variaveis         TEXT[] NOT NULL DEFAULT '{}',
    status            TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED','PAUSED')),
    meta_template_id  TEXT,
    motivo_rejeicao   TEXT,
    submetido_em      TIMESTAMPTZ,
    atualizado_em     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (account_id, nome, idioma)
);

CREATE TABLE whatsapp_envios (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id    UUID NOT NULL REFERENCES assessorias(id),
    account_id       UUID NOT NULL REFERENCES whatsapp_accounts(id),
    template_id      UUID REFERENCES whatsapp_templates(id),
    destinatario_e164 TEXT NOT NULL,
    tipo             TEXT NOT NULL CHECK (tipo IN ('TEMPLATE','FREEFORM','MIDIA')),
    payload          JSONB NOT NULL DEFAULT '{}',
    idempotency_key  UUID NOT NULL UNIQUE,
    status           TEXT NOT NULL DEFAULT 'ENFILEIRADO' CHECK (status IN ('ENFILEIRADO','ENVIADO','ENTREGUE','LIDO','FALHOU')),
    wamid            TEXT,
    contexto         JSONB,
    autor_id         UUID NOT NULL REFERENCES usuarios(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at          TIMESTAMPTZ,
    delivered_at     TIMESTAMPTZ,
    read_at          TIMESTAMPTZ,
    failed_at        TIMESTAMPTZ,
    falha_motivo     TEXT
);

CREATE INDEX idx_envios_assessoria_status ON whatsapp_envios(assessoria_id, status);
CREATE INDEX idx_envios_destinatario ON whatsapp_envios(destinatario_e164);
CREATE INDEX idx_envios_wamid ON whatsapp_envios(wamid) WHERE wamid IS NOT NULL;

CREATE TABLE whatsapp_eventos_inbound (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id   UUID NOT NULL REFERENCES assessorias(id),
    account_id      UUID NOT NULL REFERENCES whatsapp_accounts(id),
    from_e164       TEXT NOT NULL,
    wamid           TEXT NOT NULL,
    tipo            TEXT NOT NULL CHECK (tipo IN ('TEXT','MEDIA','REACTION','STATUS')),
    payload         JSONB NOT NULL DEFAULT '{}',
    processado_em   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (wamid)
);

CREATE INDEX idx_inbound_assessoria ON whatsapp_eventos_inbound(assessoria_id, created_at DESC);

CREATE TABLE whatsapp_optouts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id),
    e164          CITEXT NOT NULL,
    motivo        TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (assessoria_id, e164)
);

CREATE TABLE whatsapp_window_cache (
    assessoria_id  UUID NOT NULL REFERENCES assessorias(id),
    e164           TEXT NOT NULL,
    last_inbound_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (assessoria_id, e164)
);

-- PRD-015: OAuth social network accounts, snapshots, posts, consent, quota tracking
CREATE TABLE social_accounts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id    UUID NOT NULL,
    influenciador_id UUID NOT NULL REFERENCES influenciadores (id),
    plataforma       TEXT NOT NULL CHECK (plataforma IN ('INSTAGRAM', 'YOUTUBE', 'TIKTOK')),
    external_user_id TEXT NOT NULL,
    handle           TEXT,
    access_token_enc BYTEA NOT NULL,
    token_nonce      BYTEA NOT NULL,
    refresh_token_enc BYTEA,
    refresh_nonce    BYTEA,
    expires_at       TIMESTAMPTZ,
    status           TEXT NOT NULL DEFAULT 'ATIVA'
        CHECK (status IN ('ATIVA', 'TOKEN_EXPIRADO', 'REVOGADA', 'ERRO')),
    conectado_em     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ultimo_sync_em   TIMESTAMPTZ,
    UNIQUE (plataforma, external_user_id)
);

CREATE TABLE social_snapshots (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    social_account_id UUID NOT NULL REFERENCES social_accounts (id),
    dia               DATE NOT NULL,
    followers         INT,
    following         INT,
    posts_total       INT,
    engagement_rate   NUMERIC(6, 4),
    payload_full      JSONB,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (social_account_id, dia)
);

CREATE TABLE social_posts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    social_account_id UUID NOT NULL REFERENCES social_accounts (id),
    external_post_id  TEXT NOT NULL,
    posted_at         TIMESTAMPTZ,
    tipo              TEXT,
    caption           TEXT,
    likes             INT NOT NULL DEFAULT 0,
    comments          INT NOT NULL DEFAULT 0,
    shares            INT NOT NULL DEFAULT 0,
    views             INT NOT NULL DEFAULT 0,
    url               TEXT,
    payload_full      JSONB,
    UNIQUE (social_account_id, external_post_id)
);

CREATE TABLE social_consentimentos (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    influenciador_id UUID NOT NULL,
    plataforma       TEXT NOT NULL,
    dado_em          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revogado_em      TIMESTAMPTZ,
    prova            JSONB
);

CREATE TABLE quota_uso (
    plataforma    TEXT   NOT NULL,
    dia           DATE   NOT NULL,
    units_usados  BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (plataforma, dia)
);

CREATE INDEX idx_social_acc_influenciador ON social_accounts (influenciador_id);
CREATE INDEX idx_social_acc_assessoria ON social_accounts (assessoria_id);
CREATE INDEX idx_social_acc_status ON social_accounts (status) WHERE status != 'REVOGADA';
CREATE INDEX idx_social_acc_expires ON social_accounts (expires_at) WHERE status = 'ATIVA';
CREATE INDEX idx_social_snap_account_dia ON social_snapshots (social_account_id, dia DESC);
CREATE INDEX idx_social_posts_account ON social_posts (social_account_id);

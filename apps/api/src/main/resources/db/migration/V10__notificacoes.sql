CREATE TABLE notificacoes (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id  UUID         NOT NULL REFERENCES assessorias(id),
    usuario_id     UUID         NOT NULL,
    tipo           VARCHAR(50)  NOT NULL,
    prioridade     VARCHAR(10)  NOT NULL DEFAULT 'NORMAL'
                                CHECK (prioridade IN ('LOW','NORMAL','HIGH')),
    titulo         VARCHAR(255) NOT NULL,
    mensagem       TEXT         NOT NULL,
    payload        JSONB        NOT NULL DEFAULT '{}',
    alvo_tipo      VARCHAR(50),
    alvo_id        UUID,
    agrupadas      INT          NOT NULL DEFAULT 1,
    lida_em        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notificacoes_usuario
    ON notificacoes(assessoria_id, usuario_id, lida_em NULLS FIRST, created_at DESC);

CREATE TABLE notificacao_preferencias (
    usuario_id UUID        NOT NULL,
    tipo       VARCHAR(50) NOT NULL,
    canal      VARCHAR(10) NOT NULL CHECK (canal IN ('INAPP','PUSH','EMAIL')),
    habilitado BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (usuario_id, tipo, canal)
);

CREATE TABLE webpush_subscriptions (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id   UUID         NOT NULL,
    endpoint     TEXT         NOT NULL UNIQUE,
    p256dh       TEXT         NOT NULL,
    auth_secret  TEXT         NOT NULL,
    user_agent   VARCHAR(500),
    ativa        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_webpush_usuario ON webpush_subscriptions(usuario_id, ativa);

CREATE TABLE notificacao_dedupe (
    key          TEXT        PRIMARY KEY,
    last_emitted TIMESTAMPTZ NOT NULL
);

-- PRD-014: device subscriptions for mobile push (FCM/APNs via Expo Push API)
CREATE TABLE device_subscriptions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    user_tipo    TEXT NOT NULL CHECK (user_tipo IN ('CREATOR', 'INTERNO')),
    canal        TEXT NOT NULL CHECK (canal IN ('APNS', 'FCM')),
    token        TEXT NOT NULL,
    plataforma   TEXT,
    ativa        BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_uso   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (canal, token)
);

CREATE INDEX idx_devicesub_user ON device_subscriptions (user_id);
CREATE INDEX idx_devicesub_user_ativa ON device_subscriptions (user_id, ativa);

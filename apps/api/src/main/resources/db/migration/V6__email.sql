-- PRD-004: E-mail Outbound multi-conta

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE email_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    nome TEXT NOT NULL,
    host TEXT NOT NULL,
    port INT NOT NULL,
    username TEXT NOT NULL,
    password_encrypted BYTEA NOT NULL,
    password_nonce BYTEA NOT NULL,
    from_address CITEXT NOT NULL,
    from_name TEXT NOT NULL,
    tls_mode TEXT NOT NULL CHECK (tls_mode IN ('STARTTLS','SSL')),
    daily_quota INT NOT NULL DEFAULT 500,
    status TEXT NOT NULL DEFAULT 'ATIVA' CHECK (status IN ('ATIVA','PAUSADA','FALHA_AUTH')),
    falhas_auth_count INT NOT NULL DEFAULT 0,
    ultima_falha_em TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL,
    UNIQUE (assessoria_id, from_address)
);

CREATE INDEX idx_email_accounts_tenant ON email_accounts(assessoria_id, status) WHERE deleted_at IS NULL;

CREATE TABLE email_layouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE UNIQUE,
    header_html TEXT NOT NULL DEFAULT '',
    footer_html TEXT NOT NULL DEFAULT '<p style="font-size:12px;color:#666">Para não receber mais e-mails desta assessoria, <a href="{{unsubscribe_url}}">clique aqui</a>.</p>',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE email_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    nome TEXT NOT NULL,
    assunto TEXT NOT NULL,
    corpo_html TEXT NOT NULL,
    corpo_texto TEXT NULL,
    variaveis_declaradas TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_email_templates_tenant ON email_templates(assessoria_id) WHERE deleted_at IS NULL;

CREATE TABLE email_envios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES email_accounts(id),
    template_id UUID NULL REFERENCES email_templates(id),
    destinatario_email CITEXT NOT NULL,
    destinatario_nome TEXT NULL,
    assunto TEXT NOT NULL,
    corpo_html_renderizado TEXT NOT NULL,
    contexto JSONB NOT NULL DEFAULT '{}',
    idempotency_key UUID NOT NULL,
    status TEXT NOT NULL DEFAULT 'ENFILEIRADO'
        CHECK (status IN ('ENFILEIRADO','ENVIADO','FALHOU','BOUNCED')),
    smtp_message_id TEXT NULL,
    enviado_em TIMESTAMPTZ NULL,
    falha_motivo TEXT NULL,
    tentativas INT NOT NULL DEFAULT 0,
    tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    autor_id UUID NOT NULL REFERENCES usuarios(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (assessoria_id, idempotency_key)
);

CREATE INDEX idx_email_envios_tenant ON email_envios(assessoria_id, status, created_at DESC);
CREATE INDEX idx_email_envios_account ON email_envios(account_id, created_at DESC);
-- Para verificação de quota diária
CREATE INDEX idx_email_envios_quota ON email_envios(account_id, created_at)
    WHERE status IN ('ENVIADO','ENFILEIRADO');

CREATE TABLE email_eventos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    envio_id UUID NOT NULL REFERENCES email_envios(id) ON DELETE CASCADE,
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    tipo TEXT NOT NULL CHECK (tipo IN ('ABERTO','CLICADO','BOUNCE','COMPLAINT','UNSUBSCRIBE')),
    payload JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_email_eventos_envio ON email_eventos(envio_id, created_at DESC);

CREATE TABLE email_optouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    email CITEXT NOT NULL,
    motivo TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (assessoria_id, email)
);

CREATE INDEX idx_email_optouts_lookup ON email_optouts(assessoria_id, email);

CREATE TABLE email_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    envio_id UUID NOT NULL REFERENCES email_envios(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    content_type TEXT NOT NULL,
    size_bytes INT NOT NULL,
    storage_path TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- PRD-019: E-mail Single Account
-- Cria system_email_config (single-row), migra conta ativa se existir,
-- dropa email_accounts e account_id de email_envios.

CREATE TABLE IF NOT EXISTS system_email_config (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    host              VARCHAR(255) NOT NULL,
    port              INTEGER      NOT NULL DEFAULT 587,
    username          VARCHAR(255) NOT NULL,
    password_enc      BYTEA        NOT NULL,
    password_nonce    BYTEA        NOT NULL,
    from_address      VARCHAR(255) NOT NULL,
    from_name         VARCHAR(255) NOT NULL DEFAULT 'feat. creators',
    tls_mode          VARCHAR(20)  NOT NULL DEFAULT 'STARTTLS',
    daily_quota       INTEGER      NOT NULL DEFAULT 500,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ATIVA',
    falhas_auth_count INTEGER      NOT NULL DEFAULT 0,
    ultima_falha_em   TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Migrar conta ATIVA existente (se houver exatamente 1)
INSERT INTO system_email_config
    (host, port, username, password_enc, password_nonce,
     from_address, from_name, tls_mode, daily_quota, status)
SELECT
    host, port, username, password_encrypted, password_nonce,
    from_address, from_name, tls_mode::VARCHAR, daily_quota, status::VARCHAR
FROM email_accounts
WHERE status = 'ATIVA'
LIMIT 1
ON CONFLICT DO NOTHING;

-- Drop tabela multi-conta
DROP TABLE IF EXISTS email_accounts;

-- account_id em email_envios não tem mais significado
ALTER TABLE email_envios DROP COLUMN IF EXISTS account_id;

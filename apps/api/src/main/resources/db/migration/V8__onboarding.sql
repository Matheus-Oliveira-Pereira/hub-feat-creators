-- PRD-006: Onboarding & Multi-tenant — email verify, lockout, MFA, convites, membros

-- ─── Ampliar acao CHECK no audit_log para eventos auth ────────────────────
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_acao_check;
ALTER TABLE audit_log ADD CONSTRAINT audit_log_acao_check CHECK (
    acao IN ('CREATE','UPDATE','DELETE','RESTORE',
             'LOGIN','LOGIN_FAILED','LOGOUT','EMAIL_VERIFIED',
             'PASSWORD_RESET_REQUEST','PASSWORD_RESET',
             'MFA_ENABLED','MFA_DISABLED','MFA_RECOVERY_USED',
             'INVITE_SENT','INVITE_ACCEPTED',
             'MEMBER_DEACTIVATED','MEMBER_ACTIVATED','MEMBER_REMOVED',
             'SIGNUP','LOCKOUT')
);

-- Adicionar ip + user_agent ao audit_log para rastreabilidade auth (AC-9)
ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS ip TEXT NULL,
    ADD COLUMN IF NOT EXISTS user_agent TEXT NULL;

-- ─── Usuários — campos de onboarding ─────────────────────────────────────
ALTER TABLE usuarios
    RENAME COLUMN mfa_secret TO mfa_secret_enc;

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS email_verificado_em TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS mfa_ativo BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'ATIVO'
        CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADO'));

-- Legados: marcar como verificados e ativos
UPDATE usuarios
SET email_verificado_em = created_at,
    status = 'ATIVO'
WHERE deleted_at IS NULL
  AND email_verificado_em IS NULL;

-- ─── Convites — adicionar perfil_id ──────────────────────────────────────
ALTER TABLE convites
    ADD COLUMN IF NOT EXISTS perfil_id UUID NULL REFERENCES perfis(id) ON DELETE SET NULL;

-- ─── Login lockout ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS login_attempts (
    key TEXT PRIMARY KEY,
    count INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─── Email verification tokens ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS email_verify_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_email_verify_tokens_usuario
    ON email_verify_tokens(usuario_id, used_at);

-- ─── Password reset tokens ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_usuario
    ON password_reset_tokens(usuario_id, used_at);

-- ─── MFA recovery codes ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mfa_recovery_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    code_hash TEXT NOT NULL UNIQUE,
    used_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_mfa_recovery_codes_usuario
    ON mfa_recovery_codes(usuario_id, used_at);

-- RLS for new tables (same pattern as existing)
ALTER TABLE login_attempts ENABLE ROW LEVEL SECURITY;
-- login_attempts keyed by (slug+email) — no tenant column, service layer enforces

ALTER TABLE email_verify_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_reset_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE mfa_recovery_codes ENABLE ROW LEVEL SECURITY;

-- These tables are accessed only by system (no direct user queries needing RLS filter)
CREATE POLICY email_verify_tokens_policy ON email_verify_tokens FOR ALL USING (true);
CREATE POLICY password_reset_tokens_policy ON password_reset_tokens FOR ALL USING (true);
CREATE POLICY mfa_recovery_codes_policy ON mfa_recovery_codes FOR ALL USING (true);

GRANT SELECT, INSERT, UPDATE, DELETE ON login_attempts TO hub_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON email_verify_tokens TO hub_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON password_reset_tokens TO hub_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON mfa_recovery_codes TO hub_app;

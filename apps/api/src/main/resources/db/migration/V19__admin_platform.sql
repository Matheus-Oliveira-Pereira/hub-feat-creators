-- V19: Admin Platform (PRD-017 / ADR-017)
-- Adds tipo=ADM to usuarios, admin_audit_log, platform_feature_flags

-- 1. Add tipo column to usuarios (existing rows = INTERNO)
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS tipo TEXT NOT NULL DEFAULT 'INTERNO';

-- 2. Make assessoria_id nullable (ADM has no assessoria)
ALTER TABLE usuarios ALTER COLUMN assessoria_id DROP NOT NULL;

-- 3. Check constraint: tipo=ADM → assessoria_id IS NULL; tipo=INTERNO → NOT NULL
ALTER TABLE usuarios ADD CONSTRAINT ck_usuario_tipo_assessoria
    CHECK (
        (tipo = 'ADM'     AND assessoria_id IS NULL) OR
        (tipo = 'INTERNO' AND assessoria_id IS NOT NULL)
    );

-- 4. Partial unique index: email must be unique among ADMs
CREATE UNIQUE INDEX IF NOT EXISTS uq_usuarios_adm_email
    ON usuarios (email) WHERE tipo = 'ADM' AND deleted_at IS NULL;

-- 5. Make refresh_tokens.assessoria_id nullable (ADM refresh tokens have no assessoria)
ALTER TABLE refresh_tokens ALTER COLUMN assessoria_id DROP NOT NULL;

-- 6. admin_audit_log (append-only)
CREATE TABLE IF NOT EXISTS admin_audit_log (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id            UUID        NOT NULL REFERENCES usuarios(id),
    acao                TEXT        NOT NULL,
    assessoria_id_afetada UUID      NULL REFERENCES assessorias(id),
    recurso             TEXT        NULL,
    recurso_id          UUID        NULL,
    payload_antes       JSONB       NULL,
    payload_depois      JSONB       NULL,
    ip                  TEXT        NULL,
    user_agent          TEXT        NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_admin_created
    ON admin_audit_log(admin_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_acao
    ON admin_audit_log(acao, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_assessoria
    ON admin_audit_log(assessoria_id_afetada, created_at DESC)
    WHERE assessoria_id_afetada IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_admin_audit_payload
    ON admin_audit_log USING GIN (payload_depois) WHERE payload_depois IS NOT NULL;

-- Immutability triggers
CREATE OR REPLACE FUNCTION admin_audit_log_immutable() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'admin_audit_log is append-only (no UPDATE/DELETE)';
END;
$$;

CREATE TRIGGER admin_audit_log_no_update
    BEFORE UPDATE ON admin_audit_log
    FOR EACH ROW EXECUTE FUNCTION admin_audit_log_immutable();

CREATE TRIGGER admin_audit_log_no_delete
    BEFORE DELETE ON admin_audit_log
    FOR EACH ROW EXECUTE FUNCTION admin_audit_log_immutable();

-- 7. platform_feature_flags
CREATE TABLE IF NOT EXISTS platform_feature_flags (
    key         TEXT        PRIMARY KEY,
    enabled     BOOLEAN     NOT NULL,
    updated_by  UUID        NULL REFERENCES usuarios(id),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed initial flags
INSERT INTO platform_feature_flags (key, enabled) VALUES
    ('FEATURE_PORTAL_ENABLED',    false),
    ('FEATURE_WHATSAPP_ENABLED',  false),
    ('FEATURE_SOCIAL_ENABLED',    false),
    ('FEATURE_AI_MATCH_ENABLED',  false),
    ('FEATURE_MOBILE_ENABLED',    false),
    ('FEATURE_SIGNUP_ENABLED',    true),
    ('FEATURE_IMPORT_ENABLED',    true),
    ('FEATURE_COMPLIANCE_STRICT', true)
ON CONFLICT (key) DO NOTHING;

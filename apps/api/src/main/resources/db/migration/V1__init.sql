-- PRD-001: Initial schema — assessorias, users, cadastros, audit log, jobs, soft-delete

-- Create users/roles
CREATE USER hub_app WITH PASSWORD 'changeme';
CREATE USER hub_migrator WITH PASSWORD 'changeme';

GRANT USAGE ON SCHEMA public TO hub_app;
GRANT CREATE ON SCHEMA public TO hub_migrator;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO hub_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE ON SEQUENCES TO hub_app;

-- Assessorias (workspaces)
CREATE TABLE assessorias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    plano TEXT NOT NULL DEFAULT 'free',
    timezone TEXT NOT NULL DEFAULT 'America/Sao_Paulo',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL
);
CREATE INDEX idx_assessorias_slug ON assessorias(slug);
CREATE INDEX idx_assessorias_deleted_at ON assessorias(deleted_at);

-- Usuarios (team members)
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    email CITEXT NOT NULL,
    senha_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('OWNER', 'ASSESSOR')) DEFAULT 'ASSESSOR',
    mfa_secret TEXT NULL,
    ultimo_login_em TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL,
    UNIQUE(assessoria_id, email)
);
CREATE INDEX idx_usuarios_assessoria_id ON usuarios(assessoria_id, deleted_at);
CREATE INDEX idx_usuarios_email ON usuarios(email);

-- Refresh tokens (ADR-008)
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash TEXT NOT NULL UNIQUE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    replaced_by UUID NULL,
    user_agent TEXT NULL,
    ip TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_usuario_id ON refresh_tokens(usuario_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(usuario_id, family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Invites
CREATE TABLE convites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    email CITEXT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL CHECK (role IN ('OWNER', 'ASSESSOR')) DEFAULT 'ASSESSOR',
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_convites_assessoria_id ON convites(assessoria_id);
CREATE INDEX idx_convites_token ON convites(token);
CREATE INDEX idx_convites_expires_at ON convites(expires_at);

-- Influenciadores
CREATE TABLE influenciadores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    nome TEXT NOT NULL,
    handles JSONB NOT NULL DEFAULT '{}',
    nicho TEXT NULL,
    audiencia_total BIGINT NULL,
    observacoes TEXT NULL,
    tags TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_influenciadores_assessoria_id ON influenciadores(assessoria_id, deleted_at);
CREATE INDEX idx_influenciadores_nome_trigram ON influenciadores USING GIN (nome gin_trgm_ops);
CREATE INDEX idx_influenciadores_tags ON influenciadores USING GIN (tags);
CREATE INDEX idx_influenciadores_nicho ON influenciadores(nicho);

-- Marcas
CREATE TABLE marcas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    nome TEXT NOT NULL,
    segmento TEXT NULL,
    site TEXT NULL,
    observacoes TEXT NULL,
    tags TEXT[] NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL,
    created_by UUID REFERENCES usuarios(id) ON DELETE SET NULL
);
CREATE INDEX idx_marcas_assessoria_id ON marcas(assessoria_id, deleted_at);
CREATE INDEX idx_marcas_nome_trigram ON marcas USING GIN (nome gin_trgm_ops);
CREATE INDEX idx_marcas_tags ON marcas USING GIN (tags);
CREATE INDEX idx_marcas_segmento ON marcas(segmento);

-- Contatos
CREATE TABLE contatos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marca_id UUID NOT NULL REFERENCES marcas(id) ON DELETE CASCADE,
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    nome TEXT NOT NULL,
    email CITEXT NULL,
    telefone TEXT NULL,
    cargo TEXT NULL,
    email_invalido BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL
);
CREATE INDEX idx_contatos_assessoria_id ON contatos(assessoria_id, deleted_at);
CREATE INDEX idx_contatos_marca_id ON contatos(marca_id, deleted_at);
CREATE INDEX idx_contatos_email ON contatos(email);

-- Audit log (immutable, non-tenant or global scope)
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    entidade TEXT NOT NULL,
    entidade_id UUID NOT NULL,
    acao TEXT NOT NULL CHECK (acao IN ('CREATE', 'UPDATE', 'DELETE', 'RESTORE')),
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_assessoria_id ON audit_log(assessoria_id, entidade, entidade_id, created_at DESC);
CREATE INDEX idx_audit_log_entidade_id ON audit_log(entidade_id, created_at DESC);

-- Email opt-outs (LGPD — immutable and perpetual)
CREATE TABLE email_optouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    email CITEXT NOT NULL,
    motivo TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(assessoria_id, email)
);
CREATE INDEX idx_email_optouts_email ON email_optouts(email);

-- Async job queue (ADR-010)
CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID REFERENCES assessorias(id) ON DELETE CASCADE,
    tipo TEXT NOT NULL,
    payload JSONB NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'PROCESSANDO', 'OK', 'MORTO')),
    agendado_para TIMESTAMPTZ NOT NULL DEFAULT now(),
    proxima_tentativa_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    tentativas INT NOT NULL DEFAULT 0,
    max_tentativas INT NOT NULL DEFAULT 4,
    idempotency_key UUID NULL,
    ultimo_erro TEXT NULL,
    iniciado_em TIMESTAMPTZ NULL,
    concluido_em TIMESTAMPTZ NULL,
    worker_id TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(assessoria_id, tipo, idempotency_key) NULLS NOT DISTINCT
);
CREATE INDEX idx_jobs_pickup ON jobs(status, proxima_tentativa_em) WHERE status = 'PENDENTE';
CREATE INDEX idx_jobs_assessoria_id ON jobs(assessoria_id);
CREATE INDEX idx_jobs_tipo ON jobs(tipo);

-- ShedLock (distributed cron locks)
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_at TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

-- Enable trigram for ILIKE search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Enable RLS
ALTER TABLE assessorias ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE convites ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE influenciadores ENABLE ROW LEVEL SECURITY;
ALTER TABLE marcas ENABLE ROW LEVEL SECURITY;
ALTER TABLE contatos ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE email_optouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;

-- RLS policies (ADR-009)
-- Assessorias: users see own assessoria
CREATE POLICY assessoria_policy ON assessorias FOR ALL
    USING (id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Usuarios: see own assessoria
CREATE POLICY usuarios_policy ON usuarios FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Convites: see own assessoria
CREATE POLICY convites_policy ON convites FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Refresh tokens: see own assessoria
CREATE POLICY refresh_tokens_policy ON refresh_tokens FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Influenciadores: see own assessoria
CREATE POLICY influenciadores_policy ON influenciadores FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Marcas: see own assessoria
CREATE POLICY marcas_policy ON marcas FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Contatos: see own assessoria
CREATE POLICY contatos_policy ON contatos FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Audit log: see own assessoria
CREATE POLICY audit_log_policy ON audit_log FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Email optouts: see own assessoria
CREATE POLICY email_optouts_policy ON email_optouts FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Jobs: see own assessoria
CREATE POLICY jobs_policy ON jobs FOR ALL
    USING (assessoria_id IS NULL OR assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Grant permissions to hub_app
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO hub_app;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO hub_app;

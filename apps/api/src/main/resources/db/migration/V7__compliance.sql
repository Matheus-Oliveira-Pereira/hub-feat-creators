-- PRD-007: LGPD Compliance baseline

-- ──────────────────────────────────────────────────────────────────────────────
-- 1. BaseLegal enum type
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TYPE base_legal AS ENUM (
    'CONSENTIMENTO',
    'EXECUCAO_CONTRATO',
    'LEGITIMO_INTERESSE',
    'OBRIGACAO_LEGAL'
);

-- ──────────────────────────────────────────────────────────────────────────────
-- 2. Add base_legal to PII-bearing entities (nullable first, then backfill)
-- ──────────────────────────────────────────────────────────────────────────────
ALTER TABLE influenciadores ADD COLUMN base_legal base_legal NULL;
ALTER TABLE marcas          ADD COLUMN base_legal base_legal NULL;
ALTER TABLE contatos        ADD COLUMN base_legal base_legal NULL;

-- Backfill existing rows (LGPD: retroactively declaring baseline)
UPDATE influenciadores SET base_legal = 'LEGITIMO_INTERESSE' WHERE base_legal IS NULL;
UPDATE marcas          SET base_legal = 'LEGITIMO_INTERESSE' WHERE base_legal IS NULL;
UPDATE contatos        SET base_legal = 'LEGITIMO_INTERESSE' WHERE base_legal IS NULL;

-- Now enforce NOT NULL
ALTER TABLE influenciadores ALTER COLUMN base_legal SET NOT NULL;
ALTER TABLE marcas          ALTER COLUMN base_legal SET NOT NULL;
ALTER TABLE contatos        ALTER COLUMN base_legal SET NOT NULL;

-- ──────────────────────────────────────────────────────────────────────────────
-- 3. ROPA — Registro de Operações de Tratamento (Art. 37)
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE data_processing_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finalidade      TEXT NOT NULL,
    base_legal      base_legal NOT NULL,
    dados_coletados TEXT[] NOT NULL DEFAULT '{}',
    retencao_meses  INT NOT NULL,
    compartilhado_com TEXT[] NOT NULL DEFAULT '{}',
    vigente         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ──────────────────────────────────────────────────────────────────────────────
-- 4. Consentimentos (rastreamento quando base_legal = CONSENTIMENTO)
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE consentimentos (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    titular_tipo  TEXT NOT NULL CHECK (titular_tipo IN ('INFLUENCIADOR','CONTATO','USUARIO')),
    titular_id    UUID NOT NULL,
    finalidade    TEXT NOT NULL,
    dado_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    revogado_em   TIMESTAMPTZ NULL,
    prova         JSONB NOT NULL DEFAULT '{}',
    UNIQUE (titular_tipo, titular_id, finalidade)
);
CREATE INDEX idx_consentimentos_titular ON consentimentos(titular_tipo, titular_id);
CREATE INDEX idx_consentimentos_assessoria ON consentimentos(assessoria_id);

-- ──────────────────────────────────────────────────────────────────────────────
-- 5. DSR — Data Subject Requests (Direitos do Titular, Art. 18)
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE dsr_solicitacoes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id   UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    titular_tipo    TEXT NOT NULL CHECK (titular_tipo IN ('INFLUENCIADOR','CONTATO','USUARIO')),
    titular_id      UUID NOT NULL,
    tipo            TEXT NOT NULL CHECK (tipo IN ('ACESSO','CORRECAO','EXCLUSAO','PORTABILIDADE','OPOSICAO')),
    status          TEXT NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE','EM_ANDAMENTO','CONCLUIDA','REJEITADA')),
    resultado_path  TEXT NULL,
    prazo_legal_em  TIMESTAMPTZ NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    atendido_em     TIMESTAMPTZ NULL
);
CREATE INDEX idx_dsr_assessoria_status ON dsr_solicitacoes(assessoria_id, status);
CREATE INDEX idx_dsr_prazo ON dsr_solicitacoes(prazo_legal_em) WHERE status = 'PENDENTE';

CREATE TABLE dsr_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitacao_id UUID NOT NULL REFERENCES dsr_solicitacoes(id) ON DELETE CASCADE,
    token_hash     TEXT NOT NULL UNIQUE,
    expires_at     TIMESTAMPTZ NOT NULL,
    used_at        TIMESTAMPTZ NULL
);
CREATE INDEX idx_dsr_tokens_hash ON dsr_tokens(token_hash);

-- ──────────────────────────────────────────────────────────────────────────────
-- 6. Policy versions (Política de Privacidade versionada)
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE policy_versions (
    versao     TEXT PRIMARY KEY,
    texto      TEXT NOT NULL,
    hash       TEXT NOT NULL,
    material   BOOLEAN NOT NULL DEFAULT FALSE,
    vigente_de TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE policy_aceites (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    versao     TEXT NOT NULL REFERENCES policy_versions(versao),
    aceito_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip         TEXT NULL,
    user_agent TEXT NULL,
    UNIQUE (user_id, versao)
);
CREATE INDEX idx_policy_aceites_user ON policy_aceites(user_id);

-- ──────────────────────────────────────────────────────────────────────────────
-- 7. PII access log (audit de acesso a dados pessoais)
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE pii_access_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NULL,
    user_id     UUID NULL,
    recurso     TEXT NOT NULL,
    recurso_id  UUID NOT NULL,
    acao        TEXT NOT NULL,
    ts          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_pii_access_log_user ON pii_access_log(user_id, ts DESC);
CREATE INDEX idx_pii_access_log_recurso ON pii_access_log(recurso, recurso_id);

-- ──────────────────────────────────────────────────────────────────────────────
-- 8. Retention runs (log do job de purge diário)
-- ──────────────────────────────────────────────────────────────────────────────
CREATE TABLE retention_runs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    data          DATE NOT NULL,
    tabela        TEXT NOT NULL,
    anonimizados  INT NOT NULL DEFAULT 0,
    purgados      INT NOT NULL DEFAULT 0,
    duracao_ms    BIGINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_retention_runs_data ON retention_runs(data DESC);

-- ──────────────────────────────────────────────────────────────────────────────
-- 9. Audit log hash chain columns (append-only, anti-tampering)
-- ──────────────────────────────────────────────────────────────────────────────
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS prev_hash TEXT NULL;
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS chain_hash TEXT NULL;

-- ──────────────────────────────────────────────────────────────────────────────
-- 10. Seed ROPA entries
-- ──────────────────────────────────────────────────────────────────────────────
INSERT INTO data_processing_records (finalidade, base_legal, dados_coletados, retencao_meses, compartilhado_com) VALUES
('Gestão de relacionamento com influenciadores', 'LEGITIMO_INTERESSE',
 ARRAY['nome','handles_redes_sociais','nicho','audiencia','observacoes'], 60,
 ARRAY['Railway (hosting)','Vercel (frontend)']),
('Comunicação comercial com marcas', 'LEGITIMO_INTERESSE',
 ARRAY['nome_contato','email','telefone','cargo'], 60,
 ARRAY['Railway (hosting)','Vercel (frontend)','SMTP relay da assessoria']),
('Envio de e-mails via SMTP', 'LEGITIMO_INTERESSE',
 ARRAY['email_destinatario','nome_destinatario','conteudo_email'], 60,
 ARRAY['Provedor SMTP externo da assessoria']),
('Autenticação e acesso ao sistema', 'EXECUCAO_CONTRATO',
 ARRAY['email','senha_hash','ip_login'], 12,
 ARRAY['Railway (hosting)']);

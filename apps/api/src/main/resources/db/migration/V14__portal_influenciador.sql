-- Portal do Influenciador (PRD-013)

-- Creator users (separate from internal usuarios)
CREATE TABLE creator_users (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  influenciador_id UUID NOT NULL UNIQUE REFERENCES influenciadores(id),
  assessoria_id    UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
  email            CITEXT NOT NULL UNIQUE,
  senha_hash       TEXT NOT NULL,
  email_verificado_em TIMESTAMPTZ,
  status           TEXT NOT NULL DEFAULT 'ATIVO'
                     CHECK (status IN ('ATIVO','INATIVO','BLOQUEADO')),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_creator_users_assessoria ON creator_users(assessoria_id);

-- Invites sent by assessoras to influenciadores
CREATE TABLE creator_invites (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  influenciador_id UUID NOT NULL REFERENCES influenciadores(id),
  assessoria_id    UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
  email            CITEXT NOT NULL,
  token_hash       TEXT NOT NULL UNIQUE,
  expires_at       TIMESTAMPTZ NOT NULL,
  aceito_em        TIMESTAMPTZ,
  criado_por_id    UUID NOT NULL REFERENCES usuarios(id),
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_creator_invites_assessoria ON creator_invites(assessoria_id);
CREATE INDEX idx_creator_invites_token      ON creator_invites(token_hash);

-- Entregáveis de campanhas
CREATE TABLE creator_entregaveis (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tarefa_id        UUID NOT NULL REFERENCES tarefas(id),
  creator_user_id  UUID NOT NULL REFERENCES creator_users(id),
  assessoria_id    UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
  arquivo_path     TEXT NOT NULL,
  filename         TEXT NOT NULL,
  content_type     TEXT NOT NULL,
  size_bytes       BIGINT NOT NULL,
  status           TEXT NOT NULL DEFAULT 'ENVIADO'
                     CHECK (status IN ('ENVIADO','EM_REVISAO','APROVADO','SOLICITADA_REVISAO')),
  feedback         TEXT,
  criado_em        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  atualizado_em    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_creator_entregaveis_tarefa     ON creator_entregaveis(tarefa_id);
CREATE INDEX idx_creator_entregaveis_assessoria ON creator_entregaveis(assessoria_id);

-- Branding por assessoria
CREATE TABLE assessoria_branding (
  assessoria_id UUID PRIMARY KEY REFERENCES assessorias(id) ON DELETE CASCADE,
  logo_url      TEXT,
  cor_primaria  TEXT,
  atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add visivel_para_creator flag to tarefas
ALTER TABLE tarefas ADD COLUMN visivel_para_creator BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_tarefas_visivel_creator
  ON tarefas(assessoria_id, visivel_para_creator)
  WHERE visivel_para_creator = TRUE;

-- Add interno + autor_tipo to tarefa_comentarios
ALTER TABLE tarefa_comentarios
  ADD COLUMN interno         BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN autor_tipo      TEXT    NOT NULL DEFAULT 'USUARIO'
               CHECK (autor_tipo IN ('USUARIO','CREATOR')),
  ADD COLUMN creator_user_id UUID REFERENCES creator_users(id);

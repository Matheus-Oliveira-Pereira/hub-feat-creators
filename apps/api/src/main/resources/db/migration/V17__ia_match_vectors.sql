-- PRD-016: IA Match — pgvector, briefings, creator features, suggestions, feedback
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE briefings (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL,
    prospeccao_id UUID UNIQUE REFERENCES prospeccoes (id) ON DELETE CASCADE,
    vertical      TEXT,
    objetivo      TEXT CHECK (objetivo IN ('AWARENESS', 'CONVERSAO', 'LANCAMENTO', 'RETENCAO')),
    audiencia     JSONB,
    formato       TEXT CHECK (formato IN ('REELS', 'VIDEO', 'POST', 'STORIES', 'QUALQUER')),
    budget_min    BIGINT,
    budget_max    BIGINT,
    texto         TEXT,
    embedding     vector(384),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE creator_profile_features (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    influenciador_id UUID UNIQUE NOT NULL REFERENCES influenciadores (id) ON DELETE CASCADE,
    assessoria_id    UUID NOT NULL,
    vertical_inferido TEXT,
    top_temas        TEXT[],
    audience_geo     JSONB,
    audience_demo    JSONB,
    engagement_30d   NUMERIC(8, 4),
    freq_post_30d    NUMERIC(6, 2),
    embedding        vector(384),
    atualizado_em    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE match_sugestoes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id    UUID NOT NULL,
    prospeccao_id    UUID NOT NULL REFERENCES prospeccoes (id) ON DELETE CASCADE,
    influenciador_id UUID NOT NULL REFERENCES influenciadores (id),
    score            NUMERIC(6, 4) NOT NULL,
    razoes           JSONB NOT NULL DEFAULT '[]',
    modelo_versao    TEXT NOT NULL,
    gerado_em        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (prospeccao_id, influenciador_id, modelo_versao)
);

CREATE TABLE match_feedback (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sugestao_id UUID NOT NULL REFERENCES match_sugestoes (id),
    autor_id    UUID NOT NULL,
    sinal       TEXT NOT NULL CHECK (sinal IN ('BOA', 'OK', 'RUIM')),
    comentario  TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE match_model_versions (
    versao        TEXT PRIMARY KEY,
    pesos         JSONB NOT NULL,
    descricao     TEXT,
    ativada_em    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    desativada_em TIMESTAMPTZ
);

CREATE TABLE creator_match_optout (
    influenciador_id UUID PRIMARY KEY REFERENCES influenciadores (id) ON DELETE CASCADE,
    motivo           TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- HNSW vector indexes for fast cosine similarity search
CREATE INDEX briefings_emb_hnsw ON briefings USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
CREATE INDEX creator_features_emb_hnsw ON creator_profile_features USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Supporting indexes
CREATE INDEX idx_briefings_assessoria ON briefings (assessoria_id);
CREATE INDEX idx_briefings_prospeccao ON briefings (prospeccao_id);
CREATE INDEX idx_creator_features_assessoria ON creator_profile_features (assessoria_id);
CREATE INDEX idx_match_sugestoes_prospeccao ON match_sugestoes (prospeccao_id);
CREATE INDEX idx_match_sugestoes_assessoria ON match_sugestoes (assessoria_id);
CREATE INDEX idx_match_feedback_sugestao ON match_feedback (sugestao_id);

-- Seed initial model version
INSERT INTO match_model_versions (versao, pesos, descricao)
VALUES ('v1.0', '{"w1":0.40,"w2":0.25,"w3":0.20,"w4":0.15}',
        'Pesos iniciais heurísticos: w1=vetor, w2=categoria, w3=histórico, w4=saúde canal');

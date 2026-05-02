-- PRD-002: Prospecção MVP — pipeline marca↔influenciador com state machine

CREATE TABLE prospeccoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    marca_id UUID NOT NULL REFERENCES marcas(id),
    influenciador_id UUID NULL REFERENCES influenciadores(id),
    assessor_responsavel_id UUID NOT NULL REFERENCES usuarios(id),
    titulo TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'NOVA'
        CHECK (status IN ('NOVA','CONTATADA','NEGOCIANDO','FECHADA_GANHA','FECHADA_PERDIDA')),
    valor_estimado_centavos BIGINT NULL,
    proxima_acao TEXT NULL,
    proxima_acao_em DATE NULL,
    observacoes TEXT NULL,
    tags TEXT[] NOT NULL DEFAULT '{}',
    motivo_perda TEXT NULL
        CHECK (motivo_perda IS NULL
               OR motivo_perda IN ('SEM_FIT','ORCAMENTO','TIMING','CONCORRENTE','SEM_RESPOSTA','OUTRO')),
    motivo_perda_detalhe TEXT NULL,
    fechada_em TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL,
    created_by UUID NULL REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_prospeccoes_tenant ON prospeccoes(assessoria_id, deleted_at);
CREATE INDEX idx_prospeccoes_status ON prospeccoes(assessoria_id, status, deleted_at);
CREATE INDEX idx_prospeccoes_assessor ON prospeccoes(assessoria_id, assessor_responsavel_id, deleted_at);
CREATE INDEX idx_prospeccoes_proxima_acao ON prospeccoes(assessoria_id, proxima_acao_em);
CREATE INDEX idx_prospeccoes_tags ON prospeccoes USING GIN (tags);
CREATE INDEX idx_prospeccoes_titulo_trigram ON prospeccoes USING GIN (titulo gin_trgm_ops);

ALTER TABLE prospeccoes ENABLE ROW LEVEL SECURITY;
CREATE POLICY prospeccoes_policy ON prospeccoes FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Eventos (timeline imutável)
CREATE TABLE prospeccao_eventos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prospeccao_id UUID NOT NULL REFERENCES prospeccoes(id) ON DELETE CASCADE,
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    tipo TEXT NOT NULL
        CHECK (tipo IN ('STATUS_CHANGE','COMMENT','EMAIL_SENT','TASK_LINKED')),
    payload JSONB NOT NULL DEFAULT '{}',
    autor_id UUID NULL REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_prospeccao_eventos_prospeccao ON prospeccao_eventos(prospeccao_id, created_at DESC);
CREATE INDEX idx_prospeccao_eventos_tenant ON prospeccao_eventos(assessoria_id, created_at DESC);

ALTER TABLE prospeccao_eventos ENABLE ROW LEVEL SECURITY;
CREATE POLICY prospeccao_eventos_policy ON prospeccao_eventos FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

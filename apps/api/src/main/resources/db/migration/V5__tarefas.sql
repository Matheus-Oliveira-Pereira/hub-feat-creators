-- PRD-003: Tarefas + Alertas MVP

CREATE TABLE tarefas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    titulo TEXT NOT NULL,
    descricao TEXT NULL,
    prazo TIMESTAMPTZ NOT NULL,
    prioridade TEXT NOT NULL DEFAULT 'MEDIA'
        CHECK (prioridade IN ('BAIXA','MEDIA','ALTA','URGENTE')),
    status TEXT NOT NULL DEFAULT 'TODO'
        CHECK (status IN ('TODO','EM_ANDAMENTO','FEITA','CANCELADA')),
    responsavel_id UUID NOT NULL REFERENCES usuarios(id),
    criador_id UUID NOT NULL REFERENCES usuarios(id),
    entidade_tipo TEXT NULL
        CHECK (entidade_tipo IS NULL OR entidade_tipo IN ('PROSPECCAO','INFLUENCIADOR','MARCA','CONTATO')),
    entidade_id UUID NULL,
    concluida_em TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_tarefas_tenant ON tarefas(assessoria_id, deleted_at);
CREATE INDEX idx_tarefas_responsavel ON tarefas(assessoria_id, responsavel_id, status, prazo)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_tarefas_digest ON tarefas(assessoria_id, prazo, status)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_tarefas_entidade ON tarefas(assessoria_id, entidade_tipo, entidade_id)
    WHERE deleted_at IS NULL AND entidade_id IS NOT NULL;

ALTER TABLE tarefas ENABLE ROW LEVEL SECURITY;
CREATE POLICY tarefas_policy ON tarefas FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Comentários imutáveis (sem UPDATE/DELETE no MVP)
CREATE TABLE tarefa_comentarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tarefa_id UUID NOT NULL REFERENCES tarefas(id) ON DELETE CASCADE,
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    autor_id UUID NOT NULL REFERENCES usuarios(id),
    texto TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tarefa_comentarios_tarefa ON tarefa_comentarios(tarefa_id, created_at DESC);

ALTER TABLE tarefa_comentarios ENABLE ROW LEVEL SECURITY;
CREATE POLICY tarefa_comentarios_policy ON tarefa_comentarios FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- Preferências por usuário (digest opt-out)
CREATE TABLE usuario_preferencias (
    usuario_id UUID PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    digest_diario_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

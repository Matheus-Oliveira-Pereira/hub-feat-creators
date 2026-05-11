CREATE TABLE import_jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  assessoria_id UUID NOT NULL,
  usuario_id UUID NOT NULL,
  entidade TEXT NOT NULL CHECK (entidade IN ('INFLUENCIADOR','MARCA','CONTATO')),
  arquivo_path TEXT NOT NULL,
  arquivo_nome TEXT NOT NULL,
  mapeamento JSONB NOT NULL DEFAULT '{}',
  base_legal TEXT,
  dedup_strategy TEXT NOT NULL DEFAULT 'SKIP'
    CHECK (dedup_strategy IN ('SKIP','UPDATE','DUPLICATE')),
  status TEXT NOT NULL DEFAULT 'UPLOADADO'
    CHECK (status IN ('UPLOADADO','VALIDANDO','PRONTO_DRY_RUN','EXECUTANDO','CONCLUIDO','CANCELADO','FALHOU')),
  total_linhas INT,
  processadas INT NOT NULL DEFAULT 0,
  sucesso INT NOT NULL DEFAULT 0,
  falha INT NOT NULL DEFAULT 0,
  cancelable_batch_id UUID,
  iniciado_em TIMESTAMPTZ,
  concluido_em TIMESTAMPTZ,
  relatorio_path TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_import_jobs_assessoria ON import_jobs (assessoria_id, status);
CREATE INDEX idx_import_jobs_usuario ON import_jobs (usuario_id, status);

CREATE TABLE import_job_linhas (
  job_id UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
  linha INT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('OK','ERRO','SKIP','UPDATE')),
  entidade_id UUID,
  erros JSONB NOT NULL DEFAULT '[]',
  PRIMARY KEY (job_id, linha)
);

CREATE TABLE import_templates (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  assessoria_id UUID NOT NULL,
  nome TEXT NOT NULL,
  entidade TEXT NOT NULL CHECK (entidade IN ('INFLUENCIADOR','MARCA','CONTATO')),
  mapeamento JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_import_templates_assessoria ON import_templates (assessoria_id, entidade)
  WHERE deleted_at IS NULL;

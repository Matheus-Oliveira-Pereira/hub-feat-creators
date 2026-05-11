-- Relatórios salvos (PRD-012)
CREATE TABLE relatorios_salvos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
  usuario_id UUID NOT NULL REFERENCES usuarios(id),
  nome TEXT NOT NULL,
  relatorio_tipo TEXT NOT NULL CHECK (relatorio_tipo IN ('FUNIL','PERFORMANCE_ASSESSOR','TAREFAS_SLA')),
  filtros JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_relatorios_salvos_assessoria ON relatorios_salvos (assessoria_id, relatorio_tipo);

-- Materialized views (refresh diário 03h00 via RefreshMvScheduler)
CREATE MATERIALIZED VIEW mv_funil_prospeccao AS
SELECT
  assessoria_id,
  date_trunc('day', created_at AT TIME ZONE 'America/Sao_Paulo') AS dia,
  status,
  COUNT(*)                                  AS qtd,
  COALESCE(SUM(valor_estimado_centavos), 0) AS valor_total_centavos
FROM prospeccoes
WHERE deleted_at IS NULL
GROUP BY 1, 2, 3;

CREATE UNIQUE INDEX mv_funil_uniq ON mv_funil_prospeccao (assessoria_id, dia, status);
CREATE INDEX mv_funil_assessoria_dia ON mv_funil_prospeccao (assessoria_id, dia);

CREATE MATERIALIZED VIEW mv_tarefa_sla AS
SELECT
  assessoria_id,
  date_trunc('day', created_at AT TIME ZONE 'America/Sao_Paulo') AS dia,
  responsavel_id,
  COUNT(*)                                                                        AS total,
  COUNT(*) FILTER (WHERE status = 'FEITA' AND concluida_em <= prazo)             AS no_prazo,
  COUNT(*) FILTER (WHERE status NOT IN ('FEITA','CANCELADA') AND prazo < NOW())  AS atrasadas,
  COALESCE(
    AVG(
      EXTRACT(EPOCH FROM (COALESCE(concluida_em, NOW()) - prazo)) / 3600.0
    ) FILTER (WHERE prazo < COALESCE(concluida_em, NOW())),
    0
  ) AS atraso_medio_horas
FROM tarefas
WHERE deleted_at IS NULL
GROUP BY 1, 2, 3;

CREATE UNIQUE INDEX mv_tarefa_sla_uniq ON mv_tarefa_sla (assessoria_id, dia, responsavel_id);
CREATE INDEX mv_tarefa_sla_assessoria_dia ON mv_tarefa_sla (assessoria_id, dia);

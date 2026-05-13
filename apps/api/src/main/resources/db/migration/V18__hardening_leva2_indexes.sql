-- Hardening Leva 2 — missing indexes

-- match_sugestoes: lookup by influenciador_id used in getReverse + countHistoricalDeals
CREATE INDEX IF NOT EXISTS idx_match_sugestoes_influenciador
    ON match_sugestoes (influenciador_id);

-- whatsapp_accounts: webhook hot path — lookup by phone_number_id without knowing assessoria_id
CREATE INDEX IF NOT EXISTS idx_wa_accounts_phone_number_id
    ON whatsapp_accounts (phone_number_id)
    WHERE deleted_at IS NULL;

-- whatsapp_templates: status-based poll query (PENDING templates)
CREATE INDEX IF NOT EXISTS idx_wa_templates_status_pending
    ON whatsapp_templates (status)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_wa_templates_assessoria
    ON whatsapp_templates (assessoria_id);

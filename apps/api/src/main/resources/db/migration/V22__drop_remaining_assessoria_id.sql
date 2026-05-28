-- V22: Drop assessoria_id from tables missed by V20
ALTER TABLE creator_entregaveis   DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE pii_access_log        DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE whatsapp_eventos_inbound DROP COLUMN IF EXISTS assessoria_id CASCADE;

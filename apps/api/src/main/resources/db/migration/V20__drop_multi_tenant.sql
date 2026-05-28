-- V20: Single-tenant migration (ADR-018 / PRD-018)
-- Removes multi-tenancy infrastructure: assessoria_id columns, assessorias table,
-- admin_audit_log, tipo=ADM, and related constraints.

-- 1. Drop RLS policies (if any exist)
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_policies WHERE tablename = 'influenciadores') THEN
    ALTER TABLE influenciadores DISABLE ROW LEVEL SECURITY;
  END IF;
END $$;

-- 2. Remove admin_audit_log (no longer needed)
DROP TABLE IF EXISTS admin_audit_log CASCADE;

-- 3. Remove CHECK constraint and unique index added by V19
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS ck_usuario_tipo_assessoria;
DROP INDEX IF EXISTS uq_usuarios_adm_email;

-- 4. Drop tipo column from usuarios
ALTER TABLE usuarios DROP COLUMN IF EXISTS tipo;

-- 5. Drop assessoria_id from usuarios + make email UNIQUE globally
ALTER TABLE usuarios DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_email UNIQUE (email);

-- 6. Drop assessoria_id from refresh_tokens
ALTER TABLE refresh_tokens DROP COLUMN IF EXISTS assessoria_id CASCADE;

-- 7. Drop assessoria_id from all tenant tables
ALTER TABLE influenciadores DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE marcas DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE contatos DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE prospeccoes DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE prospeccao_eventos DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE tarefas DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE tarefa_comentarios DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE notificacoes DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE notificacao_dedupe DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE notificacao_preferencias DROP COLUMN IF EXISTS assessoria_id CASCADE;
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'notificacao_digest_log') THEN
    ALTER TABLE notificacao_digest_log DROP COLUMN IF EXISTS assessoria_id CASCADE;
  END IF;
END $$;
ALTER TABLE convites DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE email_accounts DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE email_envios DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE email_templates DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE email_layouts DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE email_optouts DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE email_eventos DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE whatsapp_accounts DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE whatsapp_envios DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE whatsapp_templates DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE whatsapp_optouts DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE whatsapp_window_cache DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE social_accounts DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE import_jobs DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE import_templates DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE jobs DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE audit_log DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE eventos DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE relatorios_salvos DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE match_sugestoes DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE creator_profile_features DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE briefings DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE assessoria_branding DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE creator_users DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE creator_invites DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE dsr_solicitacoes DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE consentimentos DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE perfis DROP COLUMN IF EXISTS assessoria_id CASCADE;
ALTER TABLE device_subscriptions DROP COLUMN IF EXISTS assessoria_id CASCADE;

-- 8. Drop assessorias table (no longer needed)
DROP TABLE IF EXISTS assessorias CASCADE;

-- 9. Seed default perfis if they don't exist (single-tenant has one set of perfis)
INSERT INTO perfis (id, nome, descricao, roles, is_system, created_at, updated_at)
SELECT gen_random_uuid(), 'Owner', 'Acesso total', ARRAY['OWNR','INFL','INFR','INFCR','MARCA','MARR','MARCR','CONT','CONR','PROW','PROR','TSKW','TSKR','EMAL','EMLR','WHAR','WHAW','SOCC','SOCR','RPTR','RPTW','MTCR','MTCW','BHIS','EXPT','IMPW','IMPR','DVCR','FLAG','USRW','USRR'], true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM perfis WHERE nome = 'Owner' AND is_system = true);

INSERT INTO perfis (id, nome, descricao, roles, is_system, created_at, updated_at)
SELECT gen_random_uuid(), 'Assessor', 'Operacional padrão', ARRAY['INFL','INFR','MARCA','MARR','CONT','CONR','PROW','PROR','TSKW','TSKR','BHIS'], true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM perfis WHERE nome = 'Assessor' AND is_system = true);

INSERT INTO perfis (id, nome, descricao, roles, is_system, created_at, updated_at)
SELECT gen_random_uuid(), 'Leitor', 'Somente leitura', ARRAY['INFR','MARR','CONR','PROR','TSKR'], true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM perfis WHERE nome = 'Leitor' AND is_system = true);

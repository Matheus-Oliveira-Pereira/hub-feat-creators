-- PRD-005 + ADR-015: RBAC (perfis + roles 4-letter) + assessor_responsavel em influenciador

-- ─── Perfis ───────────────────────────────────────────────────────────────
CREATE TABLE perfis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessoria_id UUID NOT NULL REFERENCES assessorias(id) ON DELETE CASCADE,
    nome TEXT NOT NULL,
    descricao TEXT NULL,
    roles TEXT[] NOT NULL DEFAULT '{}',
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ NULL,
    UNIQUE (assessoria_id, nome)
);
CREATE INDEX idx_perfis_assessoria_id ON perfis(assessoria_id, deleted_at);
CREATE INDEX idx_perfis_roles_gin ON perfis USING GIN (roles);

ALTER TABLE perfis ENABLE ROW LEVEL SECURITY;
CREATE POLICY perfis_policy ON perfis FOR ALL
    USING (assessoria_id = (SELECT current_setting('app.assessoria_id')::uuid));

-- FK perfil -> usuario
ALTER TABLE usuarios ADD COLUMN profile_id UUID NULL REFERENCES perfis(id);
CREATE INDEX idx_usuarios_profile_id ON usuarios(profile_id);

-- assessor responsável -> influenciador
ALTER TABLE influenciadores
    ADD COLUMN assessor_responsavel_id UUID NULL REFERENCES usuarios(id) ON DELETE SET NULL;
CREATE INDEX idx_influenciadores_assessor_responsavel
    ON influenciadores(assessoria_id, assessor_responsavel_id, deleted_at);

-- ─── Backfill seeds: Owner / Assessor / Leitor ────────────────────────────
-- Cria 3 perfis seed por assessoria; atribui aos usuários conforme role coarse
DO $$
DECLARE
    a_id UUID;
    owner_id UUID;
    assessor_id UUID;
    leitor_id UUID;
    owner_roles TEXT[] := ARRAY[
        'OWNR','INVT','EXPT','BLLG',
        'BPRO','CPRO','EPRO','DPRO',
        'BMAR','CMAR','EMAR','DMAR',
        'BINF','CINF','EINF','DINF',
        'BCON','CCON','ECON','DCON',
        'BUSU','CUSU','EUSU','DUSU',
        'BPRF','CPRF','EPRF','DPRF',
        'BREL','BTAR','CTAR','ETAR','DTAR',
        'BEML','CEML','EEML','DEML'
    ];
    assessor_roles TEXT[] := ARRAY[
        'BPRO','CPRO','EPRO',
        'BMAR','BINF',
        'BCON','CCON','ECON',
        'BUSU',
        'BREL','BTAR','CTAR','ETAR',
        'BEML','CEML','EEML'
    ];
    leitor_roles TEXT[] := ARRAY[
        'BPRO','BMAR','BINF','BCON','BUSU','BREL','BTAR','BEML'
    ];
BEGIN
    FOR a_id IN SELECT id FROM assessorias WHERE deleted_at IS NULL LOOP
        INSERT INTO perfis (assessoria_id, nome, descricao, roles, is_system)
            VALUES (a_id, 'Owner', 'Acesso total à assessoria', owner_roles, TRUE)
            RETURNING id INTO owner_id;
        INSERT INTO perfis (assessoria_id, nome, descricao, roles, is_system)
            VALUES (a_id, 'Assessor', 'Operacional padrão de prospecção e cadastros', assessor_roles, TRUE)
            RETURNING id INTO assessor_id;
        INSERT INTO perfis (assessoria_id, nome, descricao, roles, is_system)
            VALUES (a_id, 'Leitor', 'Somente leitura', leitor_roles, TRUE)
            RETURNING id INTO leitor_id;

        UPDATE usuarios SET profile_id = owner_id
            WHERE assessoria_id = a_id AND role = 'OWNER' AND profile_id IS NULL;
        UPDATE usuarios SET profile_id = assessor_id
            WHERE assessoria_id = a_id AND role = 'ASSESSOR' AND profile_id IS NULL;
    END LOOP;
END $$;

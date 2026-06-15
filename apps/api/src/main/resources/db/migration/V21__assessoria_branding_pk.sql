-- V21: Add UUID PK to assessoria_branding (assessoria_id was PK before V20 dropped it)
ALTER TABLE assessoria_branding ADD COLUMN id UUID DEFAULT gen_random_uuid();
UPDATE assessoria_branding SET id = gen_random_uuid() WHERE id IS NULL;
ALTER TABLE assessoria_branding ALTER COLUMN id SET NOT NULL;
ALTER TABLE assessoria_branding ADD PRIMARY KEY (id);

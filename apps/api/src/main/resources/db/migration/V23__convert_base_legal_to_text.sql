-- V23: Convert base_legal PostgreSQL ENUM type to TEXT
-- Hibernate 6 + Spring Boot 3 cannot bind String to PostgreSQL custom ENUM type.
-- Constraint is enforced at Java enum level; DB stores plain text.
ALTER TABLE influenciadores ALTER COLUMN base_legal TYPE TEXT USING base_legal::TEXT;
ALTER TABLE marcas ALTER COLUMN base_legal TYPE TEXT USING base_legal::TEXT;
ALTER TABLE contatos ALTER COLUMN base_legal TYPE TEXT USING base_legal::TEXT;
ALTER TABLE data_processing_records ALTER COLUMN base_legal TYPE TEXT USING base_legal::TEXT;
DROP TYPE IF EXISTS base_legal;

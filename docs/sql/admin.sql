-- ============================================================================
-- Administrador do sistema
-- Admins não são criados pela API: são inseridos direto no banco por este script.
-- ============================================================================

-- 1) Executar UMA vez por banco já existente.
--    O Hibernate (ddl-auto=update) não atualiza a CHECK do enum, então ela ainda
--    não aceita 'ADMIN'. Em banco novo a restrição já nasce com ADMIN e este passo
--    é inofensivo.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_usertype_check;
ALTER TABLE users ADD CONSTRAINT users_usertype_check
    CHECK (usertype IN ('CUSTOMER', 'PROVIDER', 'ADMIN'));

-- 2) Extensão usada para gerar o hash BCrypt da senha (compatível com o
--    BCryptPasswordEncoder do Spring).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 3) Inserir o admin. Troque email, senha e nome antes de executar.
WITH newuser AS (
    INSERT INTO users (email, password, usertype, active, createdat, updatedat)
    VALUES ('admin@sich.com', crypt('TroqueEstaSenha123', gen_salt('bf', 10)), 'ADMIN', true, now(), now())
    RETURNING id
)
INSERT INTO admins (userid, name, createdat, updatedat)
SELECT id, 'Administrador', now(), now() FROM newuser;

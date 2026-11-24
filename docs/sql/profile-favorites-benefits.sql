-- ============================================================================
-- Foto de perfil (userphotos), favoritos (favoriteproviders) e cashback
-- (appointments.cashbackused / appointments.cashbackearned)
-- O Hibernate (ddl-auto=update) cria as tabelas, as colunas (com DEFAULT 0) e a
-- UNIQUE dos favoritos, mas não as CHECKs nem os índices abaixo.
-- Executar depois que a aplicação subir uma vez com as novas entidades.
-- ============================================================================

-- Garante as colunas de cashback mesmo se o banco for atualizado antes da aplicação subir.
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS cashbackused numeric(12,2) NOT NULL DEFAULT 0;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS cashbackearned numeric(12,2) NOT NULL DEFAULT 0;

-- Desconto e cashback nunca negativos, e o desconto nunca maior que o total.
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_cashback_check;
ALTER TABLE appointments ADD CONSTRAINT appointments_cashback_check
    CHECK (cashbackused >= 0 AND cashbackearned >= 0 AND cashbackused <= totalprice);

-- Saldo de cashback e cartão fidelidade somam os agendamentos do cliente por status e fim.
CREATE INDEX IF NOT EXISTS appointments_customer_status_end_idx
    ON appointments (customerid, status, endat);

-- "Favoritado por" e a exclusão em cascata do prestador filtram por providerid
-- (a UNIQUE (customerid, providerid) já atende as consultas pelo cliente).
CREATE INDEX IF NOT EXISTS favoriteproviders_provider_idx
    ON favoriteproviders (providerid);

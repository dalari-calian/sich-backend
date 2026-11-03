-- ============================================================================
-- Bloqueios da agenda (providertimeblocks)
-- O Hibernate (ddl-auto=update) cria a tabela, mas não a CHECK nem o índice.
-- Executar depois que a aplicação subir uma vez com a entidade TimeBlockEntity.
-- ============================================================================

ALTER TABLE providertimeblocks DROP CONSTRAINT IF EXISTS providertimeblocks_period_check;
ALTER TABLE providertimeblocks ADD CONSTRAINT providertimeblocks_period_check
    CHECK (endat > startat);

-- Consultas da agenda, da disponibilidade e do resumo filtram por prestador e período.
CREATE INDEX IF NOT EXISTS providertimeblocks_provider_period_idx
    ON providertimeblocks (providerid, startat, endat);

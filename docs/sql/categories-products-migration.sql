-- ============================================================================
-- Migração: catálogo em árvore -> categorias (admin) + produtos do prestador
--
-- A estrutura anterior (products em árvore com parentid/sku e a tabela de vínculo
-- providerproducts) foi substituída. O Hibernate (ddl-auto=update) não remove
-- tabelas/colunas antigas, então elas precisam ser apagadas manualmente.
-- Depois de executar, suba a aplicação: o Hibernate cria categories e products.
--
-- ATENÇÃO: apaga os dados dessas duas tabelas.
-- ============================================================================

DROP TABLE IF EXISTS providerproducts;
DROP TABLE IF EXISTS products;

-- ============================================================================
-- A duração dos produtos passou a ser múltipla de 15 minutos (grade da agenda).
-- 1) Confira os produtos já cadastrados fora da grade.
-- 2) Se houver, arredonde para cima até o próximo múltiplo de 15.
-- ============================================================================

SELECT id, providerid, name, durationminutes
FROM products
WHERE durationminutes % 15 <> 0;

UPDATE products
SET durationminutes = ((durationminutes + 14) / 15) * 15,
    updatedat = now()
WHERE durationminutes % 15 <> 0;

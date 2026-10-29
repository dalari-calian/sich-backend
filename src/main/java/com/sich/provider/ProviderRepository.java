package com.sich.provider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ProviderRepository extends JpaRepository<ProviderEntity, Long> {

    Optional<ProviderEntity> findByUserId(Long userId);

    /** Trava a linha do prestador até o fim da transação: as reservas da agenda dele acontecem uma de cada vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProviderEntity p where p.id = :id")
    Optional<ProviderEntity> findByIdForUpdate(@Param("id") Long id);

    /**
     * Prestadores com pelo menos um produto ativo cujo nome do prestador ou de um
     * produto ativo contenha {@code term} (já em minúsculo, com {@code %}). Com
     * {@code anyCategory = false}, só os que têm produto ativo em uma das categorias.
     */
    @Query("""
            select p from ProviderEntity p
            where exists (select 1 from ProductEntity pa where pa.provider = p and pa.active = true)
              and (lower(p.name) like :term
                   or exists (select 1 from ProductEntity pn where pn.provider = p and pn.active = true
                              and lower(pn.name) like :term))
              and (:anyCategory = true
                   or exists (select 1 from ProductEntity pc where pc.provider = p and pc.active = true
                              and pc.category.id in :categoryIds))
            order by p.name""")
    List<ProviderEntity> search(@Param("term") String term,
            @Param("anyCategory") boolean anyCategory,
            @Param("categoryIds") Collection<Long> categoryIds);
}

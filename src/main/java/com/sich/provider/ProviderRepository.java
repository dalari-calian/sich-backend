package com.sich.provider;

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

    List<ProviderEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    /** Prestadores com pelo menos um produto ativo na categoria. */
    @Query("""
            select distinct p from ProductEntity pr join pr.provider p
            where pr.active = true and pr.category.id = :categoryId
              and lower(p.name) like lower(concat('%', :name, '%'))
            order by p.name""")
    List<ProviderEntity> searchByNameAndCategory(@Param("name") String name, @Param("categoryId") Long categoryId);
}

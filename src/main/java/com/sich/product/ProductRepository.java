package com.sich.product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    List<ProductEntity> findByParentIsNull();

    List<ProductEntity> findByParentId(Long parentId);

    List<ProductEntity> findBySkuIn(Collection<String> skus);

    List<ProductEntity> findBySkuStartingWith(String skuPrefix);

    Optional<ProductEntity> findBySku(String sku);

    boolean existsByParentId(Long parentId);

    /**
     * Busca o pai travando a linha, para que dois filhos criados ao mesmo tempo
     * não recebam a mesma SKU.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.id = :id")
    Optional<ProductEntity> findByIdForUpdate(Long id);
}

package com.sich.product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {

    List<ProductImageEntity> findByProductIdOrderByIdAsc(Long productId);

    List<ProductImageEntity> findByProductIdInOrderByIdAsc(Collection<Long> productIds);

    Optional<ProductImageEntity> findByIdAndProductId(Long id, Long productId);

    long countByProductId(Long productId);
}

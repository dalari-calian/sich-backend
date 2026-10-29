package com.sich.product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findByProviderIdOrderByNameAsc(Long providerId);

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findByIdAndProviderId(Long id, Long providerId);

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findByProviderIdAndActiveTrueOrderByNameAsc(Long providerId);

    List<ProductEntity> findByIdInAndProviderId(Collection<Long> ids, Long providerId);

    boolean existsByCategoryId(Long categoryId);
}

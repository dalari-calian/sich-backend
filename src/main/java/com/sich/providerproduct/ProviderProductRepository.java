package com.sich.providerproduct;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderProductRepository extends JpaRepository<ProviderProductEntity, Long> {

    @EntityGraph(attributePaths = "product")
    List<ProviderProductEntity> findByProviderIdOrderByIdAsc(Long providerId);

    @EntityGraph(attributePaths = "product")
    Optional<ProviderProductEntity> findByIdAndProviderId(Long id, Long providerId);

    boolean existsByProviderIdAndProductId(Long providerId, Long productId);

    boolean existsByProductId(Long productId);
}

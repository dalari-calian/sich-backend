package com.sich.product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findByProviderIdOrderByNameAsc(Long providerId);

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findByIdAndProviderId(Long id, Long providerId);

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findByProviderIdAndActiveTrueOrderByNameAsc(Long providerId);

    List<ProductEntity> findByIdInAndProviderId(Collection<Long> ids, Long providerId);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            select new com.sich.product.ProviderMinPrice(p.provider.id, min(p.price))
            from ProductEntity p
            where p.active = true and p.provider.id in :providerIds
            group by p.provider.id""")
    List<ProviderMinPrice> findMinActivePriceByProviderIds(@Param("providerIds") Collection<Long> providerIds);

    @Query("""
            select distinct new com.sich.product.ProviderCategoryName(p.provider.id, c.name)
            from ProductEntity p join p.category c
            where p.active = true and p.provider.id in :providerIds
            order by c.name""")
    List<ProviderCategoryName> findActiveCategoryNamesByProviderIds(@Param("providerIds") Collection<Long> providerIds);
}

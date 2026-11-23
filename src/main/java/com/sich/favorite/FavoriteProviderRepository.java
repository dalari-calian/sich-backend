package com.sich.favorite;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteProviderRepository extends JpaRepository<FavoriteProviderEntity, Long> {

    boolean existsByCustomerIdAndProviderId(Long customerId, Long providerId);

    Optional<FavoriteProviderEntity> findByCustomerIdAndProviderId(Long customerId, Long providerId);

    /** Ids dos prestadores favoritos do cliente, do favoritado mais recentemente para o mais antigo. */
    @Query("""
            select f.provider.id from FavoriteProviderEntity f
            where f.customer.id = :customerId
            order by f.createdAt desc, f.id desc""")
    List<Long> findProviderIdsByCustomerId(@Param("customerId") Long customerId);

    /** Quais dos prestadores informados são favoritos do cliente dono do usuário (uma consulta para a lista toda). */
    @Query("""
            select f.provider.id from FavoriteProviderEntity f
            where f.customer.user.id = :userId and f.provider.id in :providerIds""")
    List<Long> findFavoriteProviderIds(@Param("userId") Long userId, @Param("providerIds") Collection<Long> providerIds);
}

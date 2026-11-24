package com.sich.profile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPhotoRepository extends JpaRepository<UserPhotoEntity, Long> {

    Optional<UserPhotoEntity> findByUserId(Long userId);

    /** Fotos dos prestadores (pelo id do prestador) em uma consulta; quem não tem foto não aparece. */
    @Query("""
            select new com.sich.profile.OwnerPhoto(p.id, ph.image)
            from ProviderEntity p join UserPhotoEntity ph on ph.user = p.user
            where p.id in :providerIds""")
    List<OwnerPhoto> findProviderPhotos(@Param("providerIds") Collection<Long> providerIds);

    /** Fotos dos clientes (pelo id do cliente) em uma consulta; quem não tem foto não aparece. */
    @Query("""
            select new com.sich.profile.OwnerPhoto(c.id, ph.image)
            from CustomerEntity c join UserPhotoEntity ph on ph.user = c.user
            where c.id in :customerIds""")
    List<OwnerPhoto> findCustomerPhotos(@Param("customerIds") Collection<Long> customerIds);
}

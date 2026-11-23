package com.sich.profile;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.util.ImageDataUrl;
import com.sich.user.UserEntity;

import lombok.RequiredArgsConstructor;

/**
 * Foto de perfil dos usuários. Nas listagens (cards de prestador, agenda), as fotos
 * de todos vêm em uma única consulta, nunca uma por item.
 */
@Service
@RequiredArgsConstructor
public class UserPhotoService {

    /** A foto aparece em listas (busca, favoritos, agenda), então é menor que a dos produtos. */
    static final int MAX_PHOTO_BYTES = 1024 * 1024;

    private final UserPhotoRepository userPhotoRepository;

    /** Data URL da foto do usuário, ou {@code null} quando ele não tem foto. */
    @Transactional(readOnly = true)
    public String findByUser(UserEntity user) {
        return userPhotoRepository.findByUserId(user.getId()).map(UserPhotoEntity::getImage).orElse(null);
    }

    /** Cria ou substitui a foto do usuário. */
    @Transactional
    public String save(UserEntity user, String image) {
        ImageDataUrl.validate(image, MAX_PHOTO_BYTES);
        UserPhotoEntity photo = userPhotoRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserPhotoEntity created = new UserPhotoEntity();
            created.setUser(user);
            return created;
        });
        photo.setImage(image);
        return userPhotoRepository.save(photo).getImage();
    }

    /** Remove a foto do usuário; sem foto, não faz nada. */
    @Transactional
    public void delete(UserEntity user) {
        userPhotoRepository.findByUserId(user.getId()).ifPresent(userPhotoRepository::delete);
    }

    /** Fotos por id do prestador; prestadores sem foto ficam fora do mapa. */
    @Transactional(readOnly = true)
    public Map<Long, String> findByProviderIds(Collection<Long> providerIds) {
        if (providerIds.isEmpty()) {
            return Map.of();
        }
        return toMap(userPhotoRepository.findProviderPhotos(providerIds));
    }

    /** Fotos por id do cliente; clientes sem foto ficam fora do mapa. */
    @Transactional(readOnly = true)
    public Map<Long, String> findByCustomerIds(Collection<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return toMap(userPhotoRepository.findCustomerPhotos(customerIds));
    }

    private static Map<Long, String> toMap(Collection<OwnerPhoto> photos) {
        return photos.stream().collect(Collectors.toMap(OwnerPhoto::ownerId, OwnerPhoto::image, (first, second) -> first));
    }
}

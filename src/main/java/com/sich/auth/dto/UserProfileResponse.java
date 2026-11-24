package com.sich.auth.dto;

import java.time.Instant;

import com.sich.admin.AdminEntity;
import com.sich.common.base.PersonalData;
import com.sich.common.enums.UserType;
import com.sich.user.UserEntity;

/**
 * Perfil do usuário autenticado. {@code photo} é a foto de perfil em data URL (ou
 * nulo, sem foto) e {@code createdAt} alimenta o "Cliente desde".
 */
public record UserProfileResponse(
        String email,
        UserType userType,
        String name,
        String phone,
        String cnpjCpf,
        String city,
        String state,
        String street,
        String complementAdress,
        String neighborhood,
        String cep,
        String numberAdress,
        String photo,
        Instant createdAt) {

    /** Perfil de cliente ou prestador. */
    public static UserProfileResponse of(UserEntity user, PersonalData person, String photo) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getUserType(),
                person.getName(),
                person.getPhone(),
                person.getCnpjCpf(),
                person.getCity(),
                person.getState(),
                person.getStreet(),
                person.getComplementAdress(),
                person.getNeighborhood(),
                person.getCep(),
                person.getNumberAdress(),
                photo,
                user.getCreatedAt());
    }

    public static UserProfileResponse of(UserEntity user, AdminEntity admin, String photo) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getUserType(),
                admin.getName(),
                null, null, null, null, null, null, null, null, null,
                photo,
                user.getCreatedAt());
    }
}

package com.sich.auth.dto;

import java.time.Instant;

import com.sich.admin.AdminEntity;
import com.sich.common.enums.UserType;
import com.sich.customer.CustomerEntity;
import com.sich.provider.ProviderEntity;
import com.sich.user.UserEntity;

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
        Instant createdAt) {

    public static UserProfileResponse of(UserEntity user, CustomerEntity customer) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getUserType(),
                customer.getName(),
                customer.getPhone(),
                customer.getCnpjCpf(),
                customer.getCity(),
                customer.getState(),
                customer.getStreet(),
                customer.getComplementAdress(),
                customer.getNeighborhood(),
                customer.getCep(),
                customer.getNumberAdress(),
                user.getCreatedAt());
    }

    public static UserProfileResponse of(UserEntity user, ProviderEntity provider) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getUserType(),
                provider.getName(),
                provider.getPhone(),
                provider.getCnpjCpf(),
                provider.getCity(),
                provider.getState(),
                provider.getStreet(),
                provider.getComplementAdress(),
                provider.getNeighborhood(),
                provider.getCep(),
                provider.getNumberAdress(),
                user.getCreatedAt());
    }

    public static UserProfileResponse of(UserEntity user, AdminEntity admin) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getUserType(),
                admin.getName(),
                null, null, null, null, null, null, null, null, null,
                user.getCreatedAt());
    }
}

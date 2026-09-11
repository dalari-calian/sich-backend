package com.sich.auth.dto;

import com.sich.common.enums.UserType;
import com.sich.customer.CustomerEntity;
import com.sich.provider.ProviderEntity;
import com.sich.user.UserEntity;

public record UserProfileResponse(
        Long id,
        String email,
        UserType userType,
        boolean active,
        String name,
        String phone,
        String cnpjCpf,
        String city,
        String state,
        String street,
        String complementAdress) {

    public static UserProfileResponse of(UserEntity user, CustomerEntity customer) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getUserType(),
                user.isActive(),
                customer.getName(),
                customer.getPhone(),
                customer.getCnpjCpf(),
                customer.getCity(),
                customer.getState(),
                customer.getStreet(),
                customer.getComplementAdress());
    }

    public static UserProfileResponse of(UserEntity user, ProviderEntity provider) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getUserType(),
                user.isActive(),
                provider.getName(),
                provider.getPhone(),
                null,
                null,
                null,
                null,
                null);
    }
}

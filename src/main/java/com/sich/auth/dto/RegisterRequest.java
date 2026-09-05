package com.sich.auth.dto;

import com.sich.common.enums.UserType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String phone,
        String cnpjCpf,
        @NotBlank @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres") String password,
        @NotNull UserType userType) {
}

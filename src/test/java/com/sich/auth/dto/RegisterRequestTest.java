package com.sich.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sich.common.enums.UserType;

class RegisterRequestTest {

    @Test
    void cep_shouldKeepOnlyDigits_whenValueIsFormatted() {
        RegisterRequest request = new RegisterRequest(
                "John Doe", "john@doe.com", "11999999999", "12345678900", "password123", UserType.CUSTOMER,
                "São Paulo", "SP", "Rua A", "Apto 1", "Centro", "01310-100", "123");

        assertThat(request.cep()).isEqualTo("01310100");
    }

    @Test
    void cep_shouldRemoveAnyNonDigitCharacter() {
        RegisterRequest request = new RegisterRequest(
                "John Doe", "john@doe.com", "11999999999", "12345678900", "password123", UserType.CUSTOMER,
                "São Paulo", "SP", "Rua A", "Apto 1", "Centro", "01.310-100 ", "123");

        assertThat(request.cep()).isEqualTo("01310100");
    }

    @Test
    void cep_shouldRemainNull_whenNotProvided() {
        RegisterRequest request = new RegisterRequest(
                "John Doe", "john@doe.com", "11999999999", "12345678900", "password123", UserType.CUSTOMER,
                "São Paulo", "SP", "Rua A", "Apto 1", "Centro", null, "123");

        assertThat(request.cep()).isNull();
    }
}

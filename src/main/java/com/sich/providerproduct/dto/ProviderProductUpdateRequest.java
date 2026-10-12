package com.sich.providerproduct.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProviderProductUpdateRequest(
        @Schema(description = "Como o prestador realiza o serviço", example = "Cutilagem, lixamento e esmaltação em gel")
        String description,

        @Schema(description = "Preço cobrado pelo prestador", example = "80.00")
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal price) {
}

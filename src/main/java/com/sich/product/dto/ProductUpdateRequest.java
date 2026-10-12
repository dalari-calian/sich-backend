package com.sich.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @Schema(description = "Nome do produto/categoria", example = "Unha")
        @NotBlank @Size(max = 120) String name,

        @Schema(description = "Descrição do produto", example = "Serviços de manicure e pedicure")
        String description) {
}

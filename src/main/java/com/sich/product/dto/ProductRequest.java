package com.sich.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @Schema(description = "Nome do produto/categoria", example = "Unha")
        @NotBlank @Size(max = 120) String name,

        @Schema(description = "Descrição do produto", example = "Serviços de manicure e pedicure")
        String description,

        @Schema(description = "Id do produto pai (nível acima). Vazio = produto raiz", example = "1", nullable = true)
        Long parentId) {
}

package com.sich.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @Schema(description = "Nome da categoria", example = "Unha")
        @NotBlank @Size(max = 120) String name,

        @Schema(description = "Descrição da categoria", example = "Serviços de manicure e pedicure")
        String description) {
}

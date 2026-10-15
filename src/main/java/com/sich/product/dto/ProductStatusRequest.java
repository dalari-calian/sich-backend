package com.sich.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(
        @Schema(description = "true = ativo, false = inativo", example = "false")
        @NotNull Boolean active) {
}

package com.sich.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ProductImageRequest(
        @Schema(description = "Imagem em base64 no formato data URL (png, jpeg ou webp, até 2 MB)",
                example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
        @NotBlank String image) {
}

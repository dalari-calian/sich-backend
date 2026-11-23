package com.sich.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PhotoRequest(
        @Schema(description = "Foto em base64 no formato data URL (png, jpeg ou webp, até 1 MB). "
                + "Recorte/reduza no front antes de enviar: a foto aparece em listas",
                example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
        @NotBlank String image) {
}

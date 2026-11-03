package com.sich.timeblock.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TimeBlockRequest(
        @Schema(description = "Início do bloqueio (hora local, múltiplo de 15 minutos)", example = "2026-10-20T14:00")
        @NotNull LocalDateTime startAt,

        @Schema(description = "Fim do bloqueio, exclusivo (hora local, múltiplo de 15 minutos)", example = "2026-10-20T16:00")
        @NotNull LocalDateTime endAt,

        @Schema(description = "Motivo, visível só para o prestador", example = "Consulta médica")
        @Size(max = 200) String reason) {
}

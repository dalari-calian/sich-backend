package com.sich.workinghours.dto;

import com.sich.common.util.TimeGrid;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record WorkingIntervalRequest(
        @Schema(description = "Dia da semana: 1 = segunda ... 7 = domingo", example = "1")
        @NotNull @Min(1) @Max(7) Integer weekday,

        @Schema(description = "Início do intervalo (HH:MM, múltiplo de 15 minutos)", example = "08:00")
        @NotBlank @Pattern(regexp = TimeGrid.TIME_PATTERN, message = "deve estar no formato HH:MM (ex.: 08:00)")
        String start,

        @Schema(description = "Fim do intervalo (HH:MM, múltiplo de 15 minutos)", example = "12:00")
        @NotBlank @Pattern(regexp = TimeGrid.TIME_PATTERN, message = "deve estar no formato HH:MM (ex.: 12:00)")
        String end) {
}

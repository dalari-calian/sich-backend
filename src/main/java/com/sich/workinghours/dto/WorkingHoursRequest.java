package com.sich.workinghours.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record WorkingHoursRequest(
        @Schema(description = "Todos os intervalos da semana; substituem os atuais. Lista vazia = sem expediente")
        @NotNull List<@Valid @NotNull WorkingIntervalRequest> intervals) {
}

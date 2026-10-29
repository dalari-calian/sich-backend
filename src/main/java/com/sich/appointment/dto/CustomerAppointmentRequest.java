package com.sich.appointment.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerAppointmentRequest(
        @Schema(description = "Id do prestador", example = "12")
        @NotNull Long providerId,

        @Schema(description = "Início do atendimento (um dos horários de /providers/{id}/availability)", example = "2026-10-19T15:00")
        @NotNull LocalDateTime startAt,

        @Schema(description = "Serviços do prestador, na ordem em que serão prestados", example = "[1, 2]")
        @NotEmpty List<@NotNull Long> productIds,

        @Schema(description = "Observação para o prestador", example = "Prefiro esmalte nude")
        @Size(max = 400) String notes) {
}

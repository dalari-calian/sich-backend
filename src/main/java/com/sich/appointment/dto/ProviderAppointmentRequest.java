package com.sich.appointment.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProviderAppointmentRequest(
        @Schema(description = "Início do atendimento (hora local, múltiplo de 15 minutos)", example = "2026-10-19T15:00")
        @NotNull LocalDateTime startAt,

        @Schema(description = "Serviços do próprio prestador, na ordem em que serão prestados", example = "[1, 2]")
        @NotEmpty List<@NotNull Long> productIds,

        @Schema(description = "Observação sobre o atendimento", example = "Cliente prefere esmalte nude")
        @Size(max = 400) String notes,

        @Schema(description = "Id do cliente cadastrado (veja /providers/me/customers/lookup)", example = "40")
        Long customerId,

        @Schema(description = "Nome do cliente sem cadastro; usado só quando customerId não vier", example = "Júlia Prado")
        @Size(max = 120) String customerName) {
}

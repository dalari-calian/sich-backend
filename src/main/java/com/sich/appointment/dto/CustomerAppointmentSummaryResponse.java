package com.sich.appointment.dto;

/**
 * Contadores do cliente para o perfil: atendimentos já realizados, próximos e
 * cancelados.
 */
public record CustomerAppointmentSummaryResponse(
        long completedCount,
        long upcomingCount,
        long canceledCount) {
}

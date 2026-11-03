package com.sich.appointment.dto;

import com.sich.provider.dto.ProviderSummaryResponse;

/** Card de "Profissionais em alta": o prestador e quantos agendamentos recebeu no período recente. */
public record TrendingProviderResponse(
        ProviderSummaryResponse provider,
        long recentAppointments) {
}

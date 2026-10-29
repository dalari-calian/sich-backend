package com.sich.appointment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Números da agenda do prestador nos dias {@code from} a {@code to}: cards de
 * confirmados e cancelamentos, atendimentos de hoje e ocupação da semana.
 * {@code freeMinutes} é o expediente menos o tempo agendado (nunca negativo).
 */
public record AgendaSummaryResponse(
        LocalDate from,
        LocalDate to,
        long scheduledCount,
        long canceledCount,
        long workingMinutes,
        long bookedMinutes,
        long freeMinutes,
        BigDecimal expectedRevenue) {
}

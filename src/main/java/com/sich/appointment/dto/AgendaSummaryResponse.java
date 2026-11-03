package com.sich.appointment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Números da agenda do prestador nos dias {@code from} a {@code to}: cards de
 * confirmados e cancelamentos, atendimentos de hoje e ocupação da semana.
 * {@code blockedMinutes} conta só a parte dos bloqueios dentro do expediente, e
 * {@code freeMinutes} é o expediente menos o tempo agendado e bloqueado (nunca negativo).
 */
public record AgendaSummaryResponse(
        LocalDate from,
        LocalDate to,
        long scheduledCount,
        long canceledCount,
        long blockedCount,
        long workingMinutes,
        long bookedMinutes,
        long blockedMinutes,
        long freeMinutes,
        BigDecimal expectedRevenue) {
}

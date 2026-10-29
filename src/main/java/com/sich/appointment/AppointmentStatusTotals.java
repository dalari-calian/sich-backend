package com.sich.appointment;

import java.math.BigDecimal;

/** Totais dos agendamentos de um status em um período (resultado de consulta agregada). */
public record AppointmentStatusTotals(
        AppointmentStatus status,
        Long count,
        Long minutes,
        BigDecimal revenue) {
}

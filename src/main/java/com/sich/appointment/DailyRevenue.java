package com.sich.appointment;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Atendimentos realizados e faturamento de um dia (resultado de consulta agregada). */
public record DailyRevenue(LocalDate date, Long count, BigDecimal gross, BigDecimal cashback) {
}

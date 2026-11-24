package com.sich.financial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um ponto do gráfico de faturamento: atendimentos realizados no dia e quanto renderam (bruto e líquido de cashback). */
public record DailyRevenueResponse(
        LocalDate date,
        long completedCount,
        BigDecimal grossRevenue,
        BigDecimal netRevenue) {
}

package com.sich.financial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Financeiro do prestador nos agendamentos que começam nos dias {@code from} a
 * {@code to}.
 * <ul>
 * <li>Realizados (não cancelados e já terminados): {@code grossRevenue} é a soma dos
 * preços, {@code cashbackDiscount} o desconto pago com cashback dos clientes e
 * {@code netRevenue} o que os clientes efetivamente pagaram; {@code averageTicket} é
 * o líquido dividido pela quantidade;</li>
 * <li>Previstos (não cancelados que ainda não terminaram): {@code expectedRevenue}
 * líquido de cashback;</li>
 * <li>Cancelados: {@code canceledRevenue} é o valor que deixou de entrar;</li>
 * <li>{@code daily}: um item por dia do período (inclusive dias zerados), para o gráfico;</li>
 * <li>{@code topProducts}: até 10 serviços que mais faturaram nos realizados.</li>
 * </ul>
 */
public record FinancialSummaryResponse(
        LocalDate from,
        LocalDate to,
        long completedCount,
        BigDecimal grossRevenue,
        BigDecimal cashbackDiscount,
        BigDecimal netRevenue,
        BigDecimal averageTicket,
        long upcomingCount,
        BigDecimal expectedRevenue,
        long canceledCount,
        BigDecimal canceledRevenue,
        List<DailyRevenueResponse> daily,
        List<ProductRevenueResponse> topProducts) {
}

package com.sich.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.appointment.AppointmentRepository;
import com.sich.appointment.AppointmentStatus;
import com.sich.appointment.DailyRevenue;
import com.sich.appointment.RevenueTotals;
import com.sich.common.exception.BusinessRuleException;
import com.sich.financial.dto.DailyRevenueResponse;
import com.sich.financial.dto.FinancialSummaryResponse;
import com.sich.financial.dto.ProductRevenueResponse;
import com.sich.provider.ProviderService;
import com.sich.user.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Tela "Financeiro" do prestador, montada só com consultas agregadas (seis no total,
 * qualquer que seja o período). Realizado = agendamento não cancelado que já
 * terminou, o mesmo critério do resumo do cliente.
 */
@Service
@RequiredArgsConstructor
public class FinancialService {

    /** Maior período (em dias) consultado de uma vez: um ano. */
    public static final int MAX_FINANCIAL_DAYS = 366;

    /** Quantidade de serviços no ranking de faturamento. */
    public static final int TOP_PRODUCTS = 10;

    private final AppointmentRepository appointmentRepository;
    private final ProviderService providerService;
    private final UserService userService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FinancialSummaryResponse forProvider(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        Long providerId = providerService.findByUser(userService.getCurrentlyAuthenticatedUser()).getId();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now(clock);
        AppointmentStatus scheduled = AppointmentStatus.SCHEDULED;

        RevenueTotals finished = appointmentRepository.sumFinishedRevenue(providerId, scheduled, start, end, now);
        RevenueTotals upcoming = appointmentRepository.sumUpcomingRevenue(providerId, scheduled, start, end, now);
        RevenueTotals canceled = appointmentRepository.sumRevenue(providerId, AppointmentStatus.CANCELED, start, end);
        Map<LocalDate, DailyRevenue> byDay = appointmentRepository
                .sumFinishedRevenueByDay(providerId, scheduled, start, end, now).stream()
                .collect(Collectors.toMap(DailyRevenue::date, Function.identity()));
        List<ProductRevenueResponse> topProducts = appointmentRepository
                .sumFinishedRevenueByProduct(providerId, scheduled, start, end, now, PageRequest.of(0, TOP_PRODUCTS)).stream()
                .map(product -> new ProductRevenueResponse(product.productName(), product.count(), money(product.revenue())))
                .toList();

        return new FinancialSummaryResponse(
                from,
                to,
                finished.count(),
                money(finished.gross()),
                money(finished.cashback()),
                money(finished.net()),
                averageTicket(finished),
                upcoming.count(),
                money(upcoming.net()),
                canceled.count(),
                money(canceled.gross()),
                from.datesUntil(to.plusDays(1)).map(date -> daily(date, byDay.get(date))).toList(),
                topProducts);
    }

    static void validatePeriod(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("A data final deve ser igual ou posterior à data inicial");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_FINANCIAL_DAYS) {
            throw new BusinessRuleException("O período consultado deve ter no máximo " + MAX_FINANCIAL_DAYS + " dias");
        }
    }

    private static DailyRevenueResponse daily(LocalDate date, DailyRevenue revenue) {
        if (revenue == null) {
            return new DailyRevenueResponse(date, 0, money(BigDecimal.ZERO), money(BigDecimal.ZERO));
        }
        RevenueTotals totals = new RevenueTotals(revenue.count(), revenue.gross(), revenue.cashback());
        return new DailyRevenueResponse(date, totals.count(), money(totals.gross()), money(totals.net()));
    }

    private static BigDecimal averageTicket(RevenueTotals totals) {
        if (totals.count() == 0) {
            return money(BigDecimal.ZERO);
        }
        return totals.net().divide(BigDecimal.valueOf(totals.count()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}

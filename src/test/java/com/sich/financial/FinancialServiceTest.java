package com.sich.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.sich.appointment.AppointmentRepository;
import com.sich.appointment.AppointmentStatus;
import com.sich.appointment.DailyRevenue;
import com.sich.appointment.ProductRevenue;
import com.sich.appointment.RevenueTotals;
import com.sich.common.exception.BusinessRuleException;
import com.sich.financial.dto.DailyRevenueResponse;
import com.sich.financial.dto.FinancialSummaryResponse;
import com.sich.financial.dto.ProductRevenueResponse;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 14, 0);
    private static final LocalDate FROM = LocalDate.of(2026, 11, 16);
    private static final LocalDate TO = LocalDate.of(2026, 11, 20);
    private static final LocalDateTime START = FROM.atStartOfDay();
    private static final LocalDateTime END = TO.plusDays(1).atStartOfDay();
    private static final AppointmentStatus SCHEDULED = AppointmentStatus.SCHEDULED;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProviderService providerService;

    @Mock
    private UserService userService;

    private FinancialService financialService;

    @BeforeEach
    void setUp() {
        financialService = new FinancialService(appointmentRepository, providerService, userService,
                Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));
    }

    private void authenticatedAsProvider() {
        UserEntity user = new UserEntity();
        ProviderEntity provider = new ProviderEntity();
        provider.setId(10L);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);
    }

    private static RevenueTotals totals(long count, String gross, String cashback) {
        return new RevenueTotals(count, new BigDecimal(gross), new BigDecimal(cashback));
    }

    @Test
    void forProvider_shouldSummarizeRevenueDailySeriesAndTopProducts() {
        authenticatedAsProvider();
        when(appointmentRepository.sumFinishedRevenue(10L, SCHEDULED, START, END, NOW)).thenReturn(totals(3, "240.00", "16.00"));
        when(appointmentRepository.sumUpcomingRevenue(10L, SCHEDULED, START, END, NOW)).thenReturn(totals(2, "130.00", "10.00"));
        when(appointmentRepository.sumRevenue(10L, AppointmentStatus.CANCELED, START, END)).thenReturn(totals(1, "50.00", "0"));
        when(appointmentRepository.sumFinishedRevenueByDay(10L, SCHEDULED, START, END, NOW)).thenReturn(List.of(
                new DailyRevenue(FROM, 2L, new BigDecimal("160.00"), new BigDecimal("16.00")),
                new DailyRevenue(FROM.plusDays(2), 1L, new BigDecimal("80.00"), null)));
        when(appointmentRepository.sumFinishedRevenueByProduct(10L, SCHEDULED, START, END, NOW,
                PageRequest.of(0, FinancialService.TOP_PRODUCTS))).thenReturn(List.of(
                        new ProductRevenue("Esmaltação em gel", 3L, new BigDecimal("150")),
                        new ProductRevenue("Manicure", 3L, new BigDecimal("90.00"))));

        FinancialSummaryResponse result = financialService.forProvider(FROM, TO);

        assertThat(result.from()).isEqualTo(FROM);
        assertThat(result.to()).isEqualTo(TO);
        assertThat(result.completedCount()).isEqualTo(3);
        assertThat(result.grossRevenue()).isEqualByComparingTo("240.00");
        assertThat(result.cashbackDiscount()).isEqualByComparingTo("16.00");
        assertThat(result.netRevenue()).isEqualByComparingTo("224.00");
        assertThat(result.averageTicket()).isEqualByComparingTo("74.67");
        assertThat(result.upcomingCount()).isEqualTo(2);
        assertThat(result.expectedRevenue()).isEqualByComparingTo("120.00");
        assertThat(result.canceledCount()).isEqualTo(1);
        assertThat(result.canceledRevenue()).isEqualByComparingTo("50.00");
        assertThat(result.daily()).hasSize(5);
        assertThat(result.daily().get(0)).isEqualTo(
                new DailyRevenueResponse(FROM, 2, new BigDecimal("160.00"), new BigDecimal("144.00")));
        assertThat(result.daily().get(1)).isEqualTo(
                new DailyRevenueResponse(FROM.plusDays(1), 0, new BigDecimal("0.00"), new BigDecimal("0.00")));
        assertThat(result.daily().get(2).netRevenue()).isEqualByComparingTo("80.00");
        assertThat(result.topProducts()).containsExactly(
                new ProductRevenueResponse("Esmaltação em gel", 3, new BigDecimal("150.00")),
                new ProductRevenueResponse("Manicure", 3, new BigDecimal("90.00")));
    }

    @Test
    void forProvider_shouldReturnZeroAverageTicket_whenNothingWasCompleted() {
        authenticatedAsProvider();
        when(appointmentRepository.sumFinishedRevenue(10L, SCHEDULED, START, END, NOW))
                .thenReturn(new RevenueTotals(null, null, null));
        when(appointmentRepository.sumUpcomingRevenue(10L, SCHEDULED, START, END, NOW))
                .thenReturn(new RevenueTotals(0L, null, null));
        when(appointmentRepository.sumRevenue(10L, AppointmentStatus.CANCELED, START, END))
                .thenReturn(new RevenueTotals(0L, null, null));

        FinancialSummaryResponse result = financialService.forProvider(FROM, TO);

        assertThat(result.completedCount()).isZero();
        assertThat(result.averageTicket()).isEqualByComparingTo("0");
        assertThat(result.netRevenue()).isEqualByComparingTo("0");
        assertThat(result.daily()).extracting(DailyRevenueResponse::completedCount).containsOnly(0L);
        assertThat(result.topProducts()).isEmpty();
    }

    @Test
    void forProvider_shouldReject_whenEndIsBeforeStart() {
        assertThatThrownBy(() -> financialService.forProvider(TO, FROM))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A data final deve ser igual ou posterior à data inicial");
        verify(appointmentRepository, never()).sumFinishedRevenue(any(), any(), any(), any(), any());
    }

    @Test
    void forProvider_shouldReject_whenPeriodIsLongerThanOneYear() {
        LocalDate from = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> financialService.forProvider(from, from.plusDays(FinancialService.MAX_FINANCIAL_DAYS)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O período consultado deve ter no máximo 366 dias");
    }

    @Test
    void forProvider_shouldAcceptPeriodOfExactlyOneYear() {
        authenticatedAsProvider();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(FinancialService.MAX_FINANCIAL_DAYS - 1);
        when(appointmentRepository.sumFinishedRevenue(any(), any(), any(), any(), any())).thenReturn(new RevenueTotals(0L, null, null));
        when(appointmentRepository.sumUpcomingRevenue(any(), any(), any(), any(), any())).thenReturn(new RevenueTotals(0L, null, null));
        when(appointmentRepository.sumRevenue(any(), any(), any(), any())).thenReturn(new RevenueTotals(0L, null, null));

        assertThat(financialService.forProvider(from, to).daily()).hasSize(FinancialService.MAX_FINANCIAL_DAYS);
    }
}

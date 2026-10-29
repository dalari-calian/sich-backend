package com.sich.appointment;

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

import com.sich.appointment.dto.AgendaSummaryResponse;
import com.sich.appointment.dto.CustomerAppointmentSummaryResponse;
import com.sich.common.exception.BusinessRuleException;
import com.sich.customer.CustomerEntity;
import com.sich.customer.CustomerService;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;
import com.sich.workinghours.WorkingHoursService;

@ExtendWith(MockitoExtension.class)
class AppointmentSummaryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 9, 7);
    private static final LocalDate MONDAY = NOW.toLocalDate();
    private static final LocalDate SUNDAY = MONDAY.plusDays(6);

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private WorkingHoursService workingHoursService;

    @Mock
    private ProviderService providerService;

    @Mock
    private CustomerService customerService;

    @Mock
    private UserService userService;

    private AppointmentSummaryService summaryService;

    private final UserEntity user = new UserEntity();

    @BeforeEach
    void setUp() {
        summaryService = new AppointmentSummaryService(appointmentRepository, workingHoursService, providerService,
                customerService, userService, Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));
    }

    private void authenticatedAsProvider() {
        ProviderEntity provider = new ProviderEntity();
        provider.setId(10L);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);
    }

    @Test
    void forProvider_shouldCombineTotalsByStatusWithWorkingMinutes() {
        authenticatedAsProvider();
        when(appointmentRepository.sumByStatus(10L, MONDAY.atStartOfDay(), SUNDAY.plusDays(1).atStartOfDay())).thenReturn(List.of(
                new AppointmentStatusTotals(AppointmentStatus.SCHEDULED, 6L, 540L, new BigDecimal("480.00")),
                new AppointmentStatusTotals(AppointmentStatus.CANCELED, 1L, 60L, new BigDecimal("50.00"))));
        when(workingHoursService.workingMinutesBetween(10L, MONDAY, SUNDAY)).thenReturn(3000L);

        AgendaSummaryResponse result = summaryService.forProvider(MONDAY, SUNDAY);

        assertThat(result).isEqualTo(new AgendaSummaryResponse(
                MONDAY, SUNDAY, 6, 1, 3000, 540, 2460, new BigDecimal("480.00")));
    }

    @Test
    void forProvider_shouldReturnZeros_whenNoAppointments() {
        authenticatedAsProvider();
        when(appointmentRepository.sumByStatus(10L, MONDAY.atStartOfDay(), MONDAY.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());
        when(workingHoursService.workingMinutesBetween(10L, MONDAY, MONDAY)).thenReturn(600L);

        AgendaSummaryResponse result = summaryService.forProvider(MONDAY, MONDAY);

        assertThat(result.scheduledCount()).isZero();
        assertThat(result.canceledCount()).isZero();
        assertThat(result.freeMinutes()).isEqualTo(600);
        assertThat(result.expectedRevenue()).isEqualByComparingTo("0");
    }

    @Test
    void forProvider_shouldNotReturnNegativeFreeMinutes_whenBookedOutsideWorkingHours() {
        authenticatedAsProvider();
        when(appointmentRepository.sumByStatus(10L, MONDAY.atStartOfDay(), MONDAY.plusDays(1).atStartOfDay())).thenReturn(List.of(
                new AppointmentStatusTotals(AppointmentStatus.SCHEDULED, 1L, 90L, new BigDecimal("80.00"))));
        when(workingHoursService.workingMinutesBetween(10L, MONDAY, MONDAY)).thenReturn(0L);

        assertThat(summaryService.forProvider(MONDAY, MONDAY).freeMinutes()).isZero();
    }

    @Test
    void forProvider_shouldThrow_whenPeriodIsInvalid() {
        assertThatThrownBy(() -> summaryService.forProvider(MONDAY, MONDAY.minusDays(1)))
                .isInstanceOf(BusinessRuleException.class);
        verify(appointmentRepository, never()).sumByStatus(any(), any(), any());
    }

    @Test
    void forCustomer_shouldCountCompletedUpcomingAndCanceled() {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(40L);
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(customerService.findByUser(user)).thenReturn(customer);
        when(appointmentRepository.countByCustomerIdAndStatusAndEndAtLessThanEqual(40L, AppointmentStatus.SCHEDULED, NOW))
                .thenReturn(12L);
        when(appointmentRepository.countByCustomerIdAndStatusAndEndAtGreaterThan(40L, AppointmentStatus.SCHEDULED, NOW))
                .thenReturn(2L);
        when(appointmentRepository.countByCustomerIdAndStatus(40L, AppointmentStatus.CANCELED)).thenReturn(1L);

        assertThat(summaryService.forCustomer()).isEqualTo(new CustomerAppointmentSummaryResponse(12, 2, 1));
    }
}

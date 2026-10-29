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

import com.sich.appointment.dto.AvailabilityResponse;
import com.sich.common.exception.ResourceNotFoundException;
import com.sich.common.util.TimeRange;
import com.sich.product.ProductEntity;
import com.sich.product.ProductService;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserEntity;
import com.sich.user.UserService;
import com.sich.workinghours.WorkingHoursService;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    /** Segunda-feira, 19/10/2026, 09:07. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 9, 7);
    private static final LocalDate TODAY = NOW.toLocalDate();
    private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 20);
    private static final List<Long> PRODUCT_IDS = List.of(1L, 2L);

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProductService productService;

    @Mock
    private WorkingHoursService workingHoursService;

    @Mock
    private ProviderService providerService;

    @Mock
    private UserService userService;

    private AvailabilityService availabilityService;

    private ProviderEntity provider;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(appointmentRepository, productService, workingHoursService,
                providerService, userService, Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));
        provider = new ProviderEntity();
        provider.setId(10L);
    }

    private static ProductEntity product(Long id, int minutes) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setPrice(BigDecimal.TEN);
        product.setDurationMinutes(minutes);
        return product;
    }

    private void productsLasting(int firstMinutes, int secondMinutes) {
        when(productService.findActiveForBooking(10L, PRODUCT_IDS))
                .thenReturn(List.of(product(1L, firstMinutes), product(2L, secondMinutes)));
    }

    private static AppointmentEntity booked(LocalDateTime start, LocalDateTime end) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setStartAt(start);
        appointment.setEndAt(end);
        return appointment;
    }

    @Test
    void forProvider_shouldReturnStartsWhereServicesFitAroundLunchAndBookings() {
        when(providerService.findById(10L)).thenReturn(provider);
        productsLasting(60, 30);
        when(workingHoursService.findIntervals(10L, TUESDAY)).thenReturn(List.of(
                new TimeRange(TUESDAY.atTime(8, 0), TUESDAY.atTime(12, 0)),
                new TimeRange(TUESDAY.atTime(13, 0), TUESDAY.atTime(19, 0))));
        when(appointmentRepository.findProviderOverlapping(10L, AppointmentStatus.SCHEDULED,
                TUESDAY.atStartOfDay(), TUESDAY.plusDays(1).atStartOfDay())).thenReturn(List.of(
                booked(TUESDAY.atTime(9, 0), TUESDAY.atTime(10, 30)),
                booked(TUESDAY.atTime(14, 0), TUESDAY.atTime(15, 0))));

        AvailabilityResponse result = availabilityService.forProvider(10L, TUESDAY, PRODUCT_IDS);

        assertThat(result.date()).isEqualTo(TUESDAY);
        assertThat(result.duration()).isEqualTo("01:30");
        assertThat(result.durationMinutes()).isEqualTo(90);
        assertThat(result.startTimes()).containsExactly(
                "10:30", "15:00", "15:15", "15:30", "15:45", "16:00", "16:15", "16:30", "16:45", "17:00", "17:15", "17:30");
    }

    @Test
    void forCurrentProvider_shouldStartTodayAfterNowRoundedToNextSlot() {
        UserEntity user = new UserEntity();
        when(userService.getCurrentlyAuthenticatedUser()).thenReturn(user);
        when(providerService.findByUser(user)).thenReturn(provider);
        productsLasting(30, 30);
        when(workingHoursService.findIntervals(10L, TODAY))
                .thenReturn(List.of(new TimeRange(TODAY.atTime(8, 0), TODAY.atTime(11, 0))));
        when(appointmentRepository.findProviderOverlapping(10L, AppointmentStatus.SCHEDULED,
                TODAY.atStartOfDay(), TODAY.plusDays(1).atStartOfDay())).thenReturn(List.of());

        AvailabilityResponse result = availabilityService.forCurrentProvider(TODAY, PRODUCT_IDS);

        assertThat(result.startTimes()).containsExactly("09:15", "09:30", "09:45", "10:00");
    }

    @Test
    void forProvider_shouldReturnNoStarts_whenDateIsInThePast() {
        when(providerService.findById(10L)).thenReturn(provider);
        productsLasting(60, 30);

        AvailabilityResponse result = availabilityService.forProvider(10L, TODAY.minusDays(1), PRODUCT_IDS);

        assertThat(result.startTimes()).isEmpty();
        assertThat(result.duration()).isEqualTo("01:30");
        verify(workingHoursService, never()).findIntervals(any(), any());
    }

    @Test
    void forProvider_shouldReturnNoStarts_whenDateIsBeyondMaximumDaysAhead() {
        when(providerService.findById(10L)).thenReturn(provider);
        productsLasting(60, 30);

        AvailabilityResponse result = availabilityService.forProvider(10L,
                TODAY.plusDays(AppointmentService.MAX_DAYS_AHEAD + 1), PRODUCT_IDS);

        assertThat(result.startTimes()).isEmpty();
        verify(appointmentRepository, never()).findProviderOverlapping(any(), any(), any(), any());
    }

    @Test
    void forProvider_shouldThrow_whenProviderNotFound() {
        when(providerService.findById(10L)).thenThrow(new ResourceNotFoundException("Prestador não encontrado"));

        assertThatThrownBy(() -> availabilityService.forProvider(10L, TUESDAY, PRODUCT_IDS))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productService, never()).findActiveForBooking(any(), any());
    }
}

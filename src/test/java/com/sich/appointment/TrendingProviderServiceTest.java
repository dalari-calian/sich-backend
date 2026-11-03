package com.sich.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.sich.appointment.dto.TrendingProviderResponse;
import com.sich.provider.ProviderService;
import com.sich.provider.dto.ProviderSummaryResponse;

@ExtendWith(MockitoExtension.class)
class TrendingProviderServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");
    private static final Instant SINCE = NOW.minus(Duration.ofDays(TrendingProviderService.TRENDING_DAYS));

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ProviderService providerService;

    private TrendingProviderService trendingProviderService;

    @BeforeEach
    void setUp() {
        trendingProviderService = new TrendingProviderService(appointmentRepository, providerService,
                Clock.fixed(NOW, ZoneId.of("America/Sao_Paulo")));
    }

    private static ProviderSummaryResponse summary(Long id, String name) {
        return new ProviderSummaryResponse(id, name, "São Paulo", "SP", "Jardins", new BigDecimal("95.00"), List.of("Manicure"));
    }

    @Test
    void find_shouldReturnMostBookedProvidersInOrderWithTheirCounts() {
        when(appointmentRepository.countByProviderSince(AppointmentStatus.SCHEDULED, SINCE, PageRequest.of(0, 10))).thenReturn(List.of(
                new ProviderAppointmentCount(11L, 8L),
                new ProviderAppointmentCount(10L, 5L)));
        when(providerService.findSummariesByIds(List.of(11L, 10L)))
                .thenReturn(List.of(summary(11L, "Carla Souza"), summary(10L, "Ana Beatriz")));

        List<TrendingProviderResponse> result = trendingProviderService.find(10);

        assertThat(result).containsExactly(
                new TrendingProviderResponse(summary(11L, "Carla Souza"), 8),
                new TrendingProviderResponse(summary(10L, "Ana Beatriz"), 5));
    }

    @Test
    void find_shouldClampLimit() {
        trendingProviderService.find(500);
        trendingProviderService.find(0);

        verify(appointmentRepository).countByProviderSince(AppointmentStatus.SCHEDULED, SINCE,
                PageRequest.of(0, TrendingProviderService.MAX_TRENDING));
        verify(appointmentRepository).countByProviderSince(AppointmentStatus.SCHEDULED, SINCE, PageRequest.of(0, 1));
    }

    @Test
    void find_shouldReturnEmptyWithoutLoadingProviders_whenNoAppointments() {
        when(appointmentRepository.countByProviderSince(AppointmentStatus.SCHEDULED, SINCE, PageRequest.of(0, 5)))
                .thenReturn(List.of());

        assertThat(trendingProviderService.find(5)).isEmpty();
        verify(providerService, never()).findSummariesByIds(any());
    }
}

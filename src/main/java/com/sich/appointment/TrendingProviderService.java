package com.sich.appointment;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.appointment.dto.TrendingProviderResponse;
import com.sich.provider.ProviderService;

import lombok.RequiredArgsConstructor;

/**
 * "Profissionais em alta": prestadores com produto ativo que mais receberam
 * agendamentos (não cancelados) nos últimos {@value #TRENDING_DAYS} dias.
 */
@Service
@RequiredArgsConstructor
public class TrendingProviderService {

    public static final int TRENDING_DAYS = 30;

    public static final int MAX_TRENDING = 20;

    private final AppointmentRepository appointmentRepository;
    private final ProviderService providerService;
    private final Clock clock;

    /** Até {@code limit} prestadores (de 1 a {@value #MAX_TRENDING}), em quatro consultas no total. */
    @Transactional(readOnly = true)
    public List<TrendingProviderResponse> find(int limit) {
        List<ProviderAppointmentCount> counts = appointmentRepository.countByProviderSince(AppointmentStatus.SCHEDULED,
                clock.instant().minus(Duration.ofDays(TRENDING_DAYS)), PageRequest.of(0, Math.clamp(limit, 1, MAX_TRENDING)));
        if (counts.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> countById = counts.stream()
                .collect(Collectors.toMap(ProviderAppointmentCount::providerId, ProviderAppointmentCount::count));
        return providerService.findSummariesByIds(counts.stream().map(ProviderAppointmentCount::providerId).toList()).stream()
                .map(summary -> new TrendingProviderResponse(summary, countById.get(summary.id())))
                .toList();
    }
}

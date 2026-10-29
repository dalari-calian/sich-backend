package com.sich.appointment;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.appointment.dto.AvailabilityResponse;
import com.sich.common.util.DurationFormat;
import com.sich.common.util.TimeGrid;
import com.sich.common.util.TimeRange;
import com.sich.product.ProductEntity;
import com.sich.product.ProductService;
import com.sich.provider.ProviderService;
import com.sich.user.UserService;
import com.sich.workinghours.WorkingHoursService;

import lombok.RequiredArgsConstructor;

/**
 * Horários de início livres de um prestador em uma data para os serviços escolhidos.
 * A duração vem dos produtos no banco, nunca da requisição.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AppointmentRepository appointmentRepository;
    private final ProductService productService;
    private final WorkingHoursService workingHoursService;
    private final ProviderService providerService;
    private final UserService userService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AvailabilityResponse forProvider(Long providerId, LocalDate date, List<Long> productIds) {
        return compute(providerService.findById(providerId).getId(), date, productIds);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse forCurrentProvider(LocalDate date, List<Long> productIds) {
        Long providerId = providerService.findByUser(userService.getCurrentlyAuthenticatedUser()).getId();
        return compute(providerId, date, productIds);
    }

    /** Datas passadas ou além de {@link AppointmentService#MAX_DAYS_AHEAD} dias não têm horários. */
    private AvailabilityResponse compute(Long providerId, LocalDate date, List<Long> productIds) {
        int durationMinutes = productService.findActiveForBooking(providerId, productIds).stream()
                .mapToInt(ProductEntity::getDurationMinutes)
                .sum();
        LocalDateTime now = LocalDateTime.now(clock);
        List<LocalDateTime> starts = List.of();
        if (!date.isBefore(now.toLocalDate()) && !date.isAfter(now.toLocalDate().plusDays(AppointmentService.MAX_DAYS_AHEAD))) {
            List<TimeRange> busy = appointmentRepository.findProviderOverlapping(providerId, AppointmentStatus.SCHEDULED,
                    date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream()
                    .map(appointment -> new TimeRange(appointment.getStartAt(), appointment.getEndAt()))
                    .toList();
            starts = SlotCalculator.availableStarts(workingHoursService.findIntervals(providerId, date), busy,
                    durationMinutes, TimeGrid.ceil(now));
        }
        return new AvailabilityResponse(
                date,
                DurationFormat.format(durationMinutes),
                durationMinutes,
                starts.stream().map(start -> TimeGrid.formatTime(start.toLocalTime())).toList());
    }
}

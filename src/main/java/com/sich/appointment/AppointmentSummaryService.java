package com.sich.appointment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.appointment.dto.AgendaSummaryResponse;
import com.sich.appointment.dto.CustomerAppointmentSummaryResponse;
import com.sich.common.util.TimeRange;
import com.sich.customer.CustomerService;
import com.sich.provider.ProviderService;
import com.sich.timeblock.TimeBlockService;
import com.sich.user.UserService;
import com.sich.workinghours.WorkingHoursService;

import lombok.RequiredArgsConstructor;

/**
 * Números da agenda calculados no banco (consultas agregadas), para as telas de resumo
 * não precisarem carregar todos os agendamentos só para contar.
 */
@Service
@RequiredArgsConstructor
public class AppointmentSummaryService {

    private static final AppointmentStatusTotals NONE = new AppointmentStatusTotals(null, 0L, 0L, BigDecimal.ZERO);

    private final AppointmentRepository appointmentRepository;
    private final WorkingHoursService workingHoursService;
    private final ProviderService providerService;
    private final CustomerService customerService;
    private final TimeBlockService timeBlockService;
    private final UserService userService;
    private final Clock clock;

    /** Resumo dos agendamentos do prestador autenticado que começam nos dias {@code from} a {@code to}. */
    @Transactional(readOnly = true)
    public AgendaSummaryResponse forProvider(LocalDate from, LocalDate to) {
        AppointmentService.validatePeriod(from, to);
        Long providerId = providerService.findByUser(userService.getCurrentlyAuthenticatedUser()).getId();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<AppointmentStatusTotals> totals = appointmentRepository.sumByStatus(providerId, start, end);
        AppointmentStatusTotals scheduled = totalsOf(totals, AppointmentStatus.SCHEDULED);
        AppointmentStatusTotals canceled = totalsOf(totals, AppointmentStatus.CANCELED);
        List<TimeRange> working = workingHoursService.findIntervalsBetween(providerId, from, to);
        List<TimeRange> blocks = timeBlockService.findOverlapping(providerId, start, end).stream()
                .map(block -> new TimeRange(block.getStartAt(), block.getEndAt()))
                .toList();
        long workingMinutes = working.stream().mapToLong(TimeRange::minutes).sum();
        // Os bloqueios não se sobrepõem, então somar a interseção de cada um não conta minuto em dobro.
        long blockedMinutes = working.stream()
                .mapToLong(interval -> blocks.stream().mapToLong(interval::overlapMinutes).sum())
                .sum();
        return new AgendaSummaryResponse(
                from,
                to,
                scheduled.count(),
                canceled.count(),
                blocks.size(),
                workingMinutes,
                scheduled.minutes(),
                blockedMinutes,
                Math.max(0, workingMinutes - scheduled.minutes() - blockedMinutes),
                scheduled.revenue());
    }

    /** Atendimentos já realizados, próximos e cancelados do cliente autenticado. */
    @Transactional(readOnly = true)
    public CustomerAppointmentSummaryResponse forCustomer() {
        Long customerId = customerService.findByUser(userService.getCurrentlyAuthenticatedUser()).getId();
        LocalDateTime now = LocalDateTime.now(clock);
        return new CustomerAppointmentSummaryResponse(
                appointmentRepository.countByCustomerIdAndStatusAndEndAtLessThanEqual(customerId, AppointmentStatus.SCHEDULED, now),
                appointmentRepository.countByCustomerIdAndStatusAndEndAtGreaterThan(customerId, AppointmentStatus.SCHEDULED, now),
                appointmentRepository.countByCustomerIdAndStatus(customerId, AppointmentStatus.CANCELED));
    }

    private static AppointmentStatusTotals totalsOf(List<AppointmentStatusTotals> totals, AppointmentStatus status) {
        return totals.stream().filter(total -> total.status() == status).findFirst().orElse(NONE);
    }
}

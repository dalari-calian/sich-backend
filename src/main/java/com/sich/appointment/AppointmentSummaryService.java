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
import com.sich.customer.CustomerService;
import com.sich.provider.ProviderService;
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
    private final UserService userService;
    private final Clock clock;

    /** Resumo dos agendamentos do prestador autenticado que começam nos dias {@code from} a {@code to}. */
    @Transactional(readOnly = true)
    public AgendaSummaryResponse forProvider(LocalDate from, LocalDate to) {
        AppointmentService.validatePeriod(from, to);
        Long providerId = providerService.findByUser(userService.getCurrentlyAuthenticatedUser()).getId();
        List<AppointmentStatusTotals> totals = appointmentRepository.sumByStatus(providerId,
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        AppointmentStatusTotals scheduled = totalsOf(totals, AppointmentStatus.SCHEDULED);
        AppointmentStatusTotals canceled = totalsOf(totals, AppointmentStatus.CANCELED);
        long workingMinutes = workingHoursService.workingMinutesBetween(providerId, from, to);
        return new AgendaSummaryResponse(
                from,
                to,
                scheduled.count(),
                canceled.count(),
                workingMinutes,
                scheduled.minutes(),
                Math.max(0, workingMinutes - scheduled.minutes()),
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

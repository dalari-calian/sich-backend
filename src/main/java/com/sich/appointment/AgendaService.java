package com.sich.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.appointment.dto.AgendaDayResponse;
import com.sich.appointment.dto.AgendaIntervalResponse;
import com.sich.appointment.dto.AppointmentResponse;
import com.sich.common.util.TimeRange;
import com.sich.provider.ProviderService;
import com.sich.timeblock.TimeBlockEntity;
import com.sich.timeblock.TimeBlockService;
import com.sich.timeblock.dto.TimeBlockResponse;
import com.sich.user.UserService;
import com.sich.workinghours.WorkingHoursService;

import lombok.RequiredArgsConstructor;

/**
 * Grade da agenda do prestador (visões de dia, semana e mês) montada com três
 * consultas, qualquer que seja o período: expediente, agendamentos e bloqueios.
 */
@Service
@RequiredArgsConstructor
public class AgendaService {

    private final AppointmentRepository appointmentRepository;
    private final WorkingHoursService workingHoursService;
    private final TimeBlockService timeBlockService;
    private final ProviderService providerService;
    private final UserService userService;
    private final AppointmentMapper appointmentMapper;

    /** Um item por dia de {@code from} a {@code to} (inclusive), mesmo nos dias de folga. */
    @Transactional(readOnly = true)
    public List<AgendaDayResponse> forProvider(LocalDate from, LocalDate to) {
        AppointmentService.validatePeriod(from, to);
        Long providerId = providerService.findByUser(userService.getCurrentlyAuthenticatedUser()).getId();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        Map<LocalDate, List<TimeRange>> workingByDate = workingHoursService.findIntervalsBetween(providerId, from, to).stream()
                .collect(Collectors.groupingBy(interval -> interval.start().toLocalDate()));
        Map<LocalDate, List<AppointmentResponse>> appointmentsByDate = appointmentMapper.toResponses(appointmentRepository
                .findByProviderIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(providerId, start, end)).stream()
                .collect(Collectors.groupingBy(appointment -> appointment.startAt().toLocalDate()));
        List<TimeBlockEntity> blocks = timeBlockService.findOverlapping(providerId, start, end);

        return from.datesUntil(to.plusDays(1))
                .map(date -> {
                    List<TimeRange> working = workingByDate.getOrDefault(date, List.of());
                    TimeRange day = new TimeRange(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
                    return new AgendaDayResponse(
                            date,
                            date.getDayOfWeek().getValue(),
                            working.stream().map(AgendaIntervalResponse::of).toList(),
                            breaks(working).stream().map(AgendaIntervalResponse::of).toList(),
                            blocks.stream()
                                    .filter(block -> day.overlaps(new TimeRange(block.getStartAt(), block.getEndAt())))
                                    .map(TimeBlockResponse::of)
                                    .toList(),
                            appointmentsByDate.getOrDefault(date, List.of()));
                })
                .toList();
    }

    /** Espaços entre intervalos de trabalho seguidos do mesmo dia (já em ordem de início). */
    static List<TimeRange> breaks(List<TimeRange> working) {
        List<TimeRange> breaks = new ArrayList<>();
        for (int i = 1; i < working.size(); i++) {
            LocalDateTime previousEnd = working.get(i - 1).end();
            LocalDateTime nextStart = working.get(i).start();
            if (previousEnd.isBefore(nextStart)) {
                breaks.add(new TimeRange(previousEnd, nextStart));
            }
        }
        return breaks;
    }
}

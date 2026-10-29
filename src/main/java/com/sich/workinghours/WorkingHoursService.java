package com.sich.workinghours;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sich.common.exception.BusinessRuleException;
import com.sich.common.util.TimeGrid;
import com.sich.common.util.TimeRange;
import com.sich.provider.ProviderEntity;
import com.sich.provider.ProviderService;
import com.sich.user.UserService;
import com.sich.workinghours.dto.WorkingHoursRequest;
import com.sich.workinghours.dto.WorkingIntervalRequest;
import com.sich.workinghours.dto.WorkingIntervalResponse;

import lombok.RequiredArgsConstructor;

/**
 * Expediente semanal do prestador autenticado, salvo como intervalos de trabalho por
 * dia da semana.
 */
@Service
@RequiredArgsConstructor
public class WorkingHoursService {

    private static final Comparator<WorkingHoursEntity> BY_DAY_AND_START = Comparator
            .comparing(WorkingHoursEntity::getWeekday)
            .thenComparing(WorkingHoursEntity::getStartTime);

    private final WorkingHoursRepository workingHoursRepository;
    private final ProviderService providerService;
    private final UserService userService;

    /**
     * Substitui a semana inteira. Agendamentos já marcados não são cancelados, mesmo
     * que fiquem fora do novo expediente.
     */
    @Transactional
    public List<WorkingIntervalResponse> replaceWeek(WorkingHoursRequest request) {
        ProviderEntity provider = currentProvider();
        List<WorkingHoursEntity> intervals = request.intervals().stream()
                .map(interval -> toEntity(provider, interval))
                .sorted(BY_DAY_AND_START)
                .toList();
        validateNoOverlap(intervals);
        workingHoursRepository.deleteByProviderId(provider.getId());
        return workingHoursRepository.saveAll(intervals).stream()
                .map(WorkingIntervalResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkingIntervalResponse> findAll() {
        return workingHoursRepository.findByProviderIdOrderByWeekdayAscStartTimeAsc(currentProvider().getId()).stream()
                .map(WorkingIntervalResponse::of)
                .toList();
    }

    /** Intervalos de trabalho do prestador na data, em ordem de início. */
    @Transactional(readOnly = true)
    public List<TimeRange> findIntervals(Long providerId, LocalDate date) {
        return workingHoursRepository
                .findByProviderIdAndWeekdayOrderByStartTimeAsc(providerId, date.getDayOfWeek().getValue()).stream()
                .map(interval -> new TimeRange(date.atTime(interval.getStartTime()), date.atTime(interval.getEndTime())))
                .toList();
    }

    /** Minutos de expediente do prestador somados dia a dia de {@code from} a {@code to} (inclusive). */
    @Transactional(readOnly = true)
    public long workingMinutesBetween(Long providerId, LocalDate from, LocalDate to) {
        Map<Integer, Long> minutesByWeekday = workingHoursRepository.findByProviderIdOrderByWeekdayAscStartTimeAsc(providerId)
                .stream()
                .collect(Collectors.groupingBy(WorkingHoursEntity::getWeekday,
                        Collectors.summingLong(interval -> Duration.between(interval.getStartTime(), interval.getEndTime()).toMinutes())));
        return from.datesUntil(to.plusDays(1))
                .mapToLong(date -> minutesByWeekday.getOrDefault(date.getDayOfWeek().getValue(), 0L))
                .sum();
    }

    private WorkingHoursEntity toEntity(ProviderEntity provider, WorkingIntervalRequest request) {
        LocalTime start = TimeGrid.parseTime(request.start());
        LocalTime end = TimeGrid.parseTime(request.end());
        if (!TimeGrid.isAligned(start) || !TimeGrid.isAligned(end)) {
            throw new BusinessRuleException("Os horários do expediente devem ser múltiplos de 15 minutos");
        }
        if (!start.isBefore(end)) {
            throw new BusinessRuleException("O início do intervalo deve ser antes do fim");
        }
        WorkingHoursEntity interval = new WorkingHoursEntity();
        interval.setProvider(provider);
        interval.setWeekday(request.weekday());
        interval.setStartTime(start);
        interval.setEndTime(end);
        return interval;
    }

    /** Os intervalos chegam ordenados por dia e início; encostar (12:00 e 12:00) é permitido. */
    private static void validateNoOverlap(List<WorkingHoursEntity> intervals) {
        for (int i = 1; i < intervals.size(); i++) {
            WorkingHoursEntity previous = intervals.get(i - 1);
            WorkingHoursEntity current = intervals.get(i);
            if (previous.getWeekday().equals(current.getWeekday())
                    && current.getStartTime().isBefore(previous.getEndTime())) {
                throw new BusinessRuleException("Os intervalos do expediente não podem se sobrepor no mesmo dia");
            }
        }
    }

    private ProviderEntity currentProvider() {
        return providerService.findByUser(userService.getCurrentlyAuthenticatedUser());
    }
}

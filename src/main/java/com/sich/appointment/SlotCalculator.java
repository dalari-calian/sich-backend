package com.sich.appointment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.sich.common.util.TimeGrid;
import com.sich.common.util.TimeRange;

/**
 * Calcula os horários de início livres de um dia. Função pura: recebe o expediente,
 * os períodos ocupados, a duração e o primeiro horário permitido, e não acessa banco.
 */
public final class SlotCalculator {

    private SlotCalculator() {
    }

    /**
     * Inícios, de 15 em 15 minutos, em que {@code durationMinutes} cabe inteiro dentro
     * de um intervalo de trabalho sem sobrepor nenhum período ocupado.
     */
    public static List<LocalDateTime> availableStarts(List<TimeRange> workIntervals, List<TimeRange> busy,
            int durationMinutes, LocalDateTime notBefore) {
        List<LocalDateTime> starts = new ArrayList<>();
        for (TimeRange interval : workIntervals) {
            for (LocalDateTime start = interval.start();
                    !start.plusMinutes(durationMinutes).isAfter(interval.end());
                    start = start.plusMinutes(TimeGrid.SLOT_MINUTES)) {
                TimeRange candidate = new TimeRange(start, start.plusMinutes(durationMinutes));
                if (!start.isBefore(notBefore) && busy.stream().noneMatch(candidate::overlaps)) {
                    starts.add(start);
                }
            }
        }
        return starts;
    }
}

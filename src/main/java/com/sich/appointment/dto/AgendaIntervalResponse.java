package com.sich.appointment.dto;

import com.sich.common.util.TimeGrid;
import com.sich.common.util.TimeRange;

/** Trecho de um dia da agenda, em {@code HH:MM} ({@code end} exclusivo). */
public record AgendaIntervalResponse(
        String start,
        String end) {

    public static AgendaIntervalResponse of(TimeRange range) {
        return new AgendaIntervalResponse(
                TimeGrid.formatTime(range.start().toLocalTime()),
                TimeGrid.formatTime(range.end().toLocalTime()));
    }
}

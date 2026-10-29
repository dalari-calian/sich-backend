package com.sich.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sich.common.util.TimeRange;

class SlotCalculatorTest {

    private static final LocalDateTime MIDNIGHT = LocalDateTime.of(2026, 10, 20, 0, 0);

    private static LocalDateTime at(String time) {
        String[] parts = time.split(":");
        return MIDNIGHT.withHour(Integer.parseInt(parts[0])).withMinute(Integer.parseInt(parts[1]));
    }

    private static TimeRange range(String start, String end) {
        return new TimeRange(at(start), at(end));
    }

    @Test
    void availableStarts_shouldSkipLunchAndBusyPeriods() {
        List<TimeRange> work = List.of(range("08:00", "12:00"), range("13:00", "19:00"));
        List<TimeRange> busy = List.of(range("09:00", "10:30"), range("14:00", "15:00"));

        List<LocalDateTime> starts = SlotCalculator.availableStarts(work, busy, 90, MIDNIGHT);

        assertThat(starts).containsExactly(
                at("10:30"),
                at("15:00"), at("15:15"), at("15:30"), at("15:45"), at("16:00"), at("16:15"),
                at("16:30"), at("16:45"), at("17:00"), at("17:15"), at("17:30"));
    }

    @Test
    void availableStarts_shouldAllowStartingWhenAnotherAppointmentEnds() {
        List<LocalDateTime> starts = SlotCalculator.availableStarts(
                List.of(range("08:00", "11:00")), List.of(range("09:00", "10:00")), 60, MIDNIGHT);

        assertThat(starts).containsExactly(at("08:00"), at("10:00"));
    }

    @Test
    void availableStarts_shouldIgnoreStartsBeforeNotBefore() {
        List<LocalDateTime> starts = SlotCalculator.availableStarts(
                List.of(range("08:00", "10:00")), List.of(), 60, at("08:30"));

        assertThat(starts).containsExactly(at("08:30"), at("08:45"), at("09:00"));
    }

    @Test
    void availableStarts_shouldReturnEmpty_whenServiceDoesNotFitInAnyInterval() {
        assertThat(SlotCalculator.availableStarts(List.of(range("08:00", "09:00")), List.of(), 75, MIDNIGHT)).isEmpty();
        assertThat(SlotCalculator.availableStarts(List.of(), List.of(), 15, MIDNIGHT)).isEmpty();
    }
}

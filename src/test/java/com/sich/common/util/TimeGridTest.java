package com.sich.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class TimeGridTest {

    @Test
    void isAligned_shouldAcceptOnlyMultiplesOf15Minutes() {
        assertThat(TimeGrid.isAligned(0)).isTrue();
        assertThat(TimeGrid.isAligned(15)).isTrue();
        assertThat(TimeGrid.isAligned(90)).isTrue();
        assertThat(TimeGrid.isAligned(10)).isFalse();
        assertThat(TimeGrid.isAligned(61)).isFalse();
    }

    @Test
    void isAligned_shouldCheckMinutesSecondsAndNanosOfTime() {
        assertThat(TimeGrid.isAligned(LocalTime.of(8, 0))).isTrue();
        assertThat(TimeGrid.isAligned(LocalTime.of(8, 45))).isTrue();
        assertThat(TimeGrid.isAligned(LocalTime.of(8, 7))).isFalse();
        assertThat(TimeGrid.isAligned(LocalTime.of(8, 15, 30))).isFalse();
        assertThat(TimeGrid.isAligned(LocalTime.of(8, 15, 0, 1))).isFalse();
    }

    @Test
    void ceil_shouldRoundUpToNextSlot() {
        assertThat(TimeGrid.ceil(LocalDateTime.of(2026, 10, 19, 14, 7))).isEqualTo(LocalDateTime.of(2026, 10, 19, 14, 15));
        assertThat(TimeGrid.ceil(LocalDateTime.of(2026, 10, 19, 14, 15))).isEqualTo(LocalDateTime.of(2026, 10, 19, 14, 15));
        assertThat(TimeGrid.ceil(LocalDateTime.of(2026, 10, 19, 14, 15, 30))).isEqualTo(LocalDateTime.of(2026, 10, 19, 14, 30));
        assertThat(TimeGrid.ceil(LocalDateTime.of(2026, 10, 19, 23, 50))).isEqualTo(LocalDateTime.of(2026, 10, 20, 0, 0));
    }

    @Test
    void parseAndFormatTime_shouldUseHoursAndMinutes() {
        assertThat(TimeGrid.parseTime("08:30")).isEqualTo(LocalTime.of(8, 30));
        assertThat(TimeGrid.formatTime(LocalTime.of(8, 30))).isEqualTo("08:30");
    }

    @Test
    void timePattern_shouldAcceptOnlyValidTimesOfDay() {
        assertThat("00:00").matches(TimeGrid.TIME_PATTERN);
        assertThat("08:15").matches(TimeGrid.TIME_PATTERN);
        assertThat("23:59").matches(TimeGrid.TIME_PATTERN);
        assertThat("24:00").doesNotMatch(TimeGrid.TIME_PATTERN);
        assertThat("8:00").doesNotMatch(TimeGrid.TIME_PATTERN);
        assertThat("08:60").doesNotMatch(TimeGrid.TIME_PATTERN);
    }
}

package com.sich.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class TimeRangeTest {

    private static TimeRange range(int startHour, int startMinute, int endHour, int endMinute) {
        return new TimeRange(LocalDateTime.of(2026, 10, 20, startHour, startMinute),
                LocalDateTime.of(2026, 10, 20, endHour, endMinute));
    }

    @Test
    void overlaps_shouldDetectSharedTime() {
        assertThat(range(9, 0, 10, 30).overlaps(range(10, 0, 11, 0))).isTrue();
        assertThat(range(10, 0, 11, 0).overlaps(range(9, 0, 10, 30))).isTrue();
        assertThat(range(9, 0, 12, 0).overlaps(range(10, 0, 11, 0))).isTrue();
    }

    @Test
    void overlaps_shouldAllowTouchingPeriods() {
        assertThat(range(9, 0, 10, 0).overlaps(range(10, 0, 11, 0))).isFalse();
        assertThat(range(10, 0, 11, 0).overlaps(range(9, 0, 10, 0))).isFalse();
    }

    @Test
    void contains_shouldRequireWholePeriodInside() {
        assertThat(range(8, 0, 12, 0).contains(range(8, 0, 12, 0))).isTrue();
        assertThat(range(8, 0, 12, 0).contains(range(10, 30, 12, 0))).isTrue();
        assertThat(range(8, 0, 12, 0).contains(range(11, 0, 12, 30))).isFalse();
        assertThat(range(8, 0, 12, 0).contains(range(7, 45, 9, 0))).isFalse();
    }

    @Test
    void minutes_shouldReturnLength() {
        assertThat(range(8, 0, 12, 30).minutes()).isEqualTo(270);
    }

    @Test
    void overlapMinutes_shouldReturnSharedLength() {
        assertThat(range(8, 0, 12, 0).overlapMinutes(range(11, 0, 13, 0))).isEqualTo(60);
        assertThat(range(11, 0, 13, 0).overlapMinutes(range(8, 0, 12, 0))).isEqualTo(60);
        assertThat(range(8, 0, 12, 0).overlapMinutes(range(9, 0, 9, 45))).isEqualTo(45);
        assertThat(range(8, 0, 12, 0).overlapMinutes(range(12, 0, 13, 0))).isZero();
    }
}

package com.sich.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DurationFormatTest {

    @Test
    void toMinutes_shouldConvertHoursAndMinutes() {
        assertThat(DurationFormat.toMinutes("01:30")).isEqualTo(90);
        assertThat(DurationFormat.toMinutes("0:45")).isEqualTo(45);
        assertThat(DurationFormat.toMinutes("99:59")).isEqualTo(5999);
        assertThat(DurationFormat.toMinutes("00:00")).isZero();
    }

    @Test
    void format_shouldPadHoursAndMinutes() {
        assertThat(DurationFormat.format(90)).isEqualTo("01:30");
        assertThat(DurationFormat.format(5)).isEqualTo("00:05");
        assertThat(DurationFormat.format(5999)).isEqualTo("99:59");
    }

    @Test
    void pattern_shouldAcceptOnlyHoursAndMinutesFormat() {
        assertThat("01:30").matches(DurationFormat.PATTERN);
        assertThat("9:05").matches(DurationFormat.PATTERN);
        assertThat("99:59").matches(DurationFormat.PATTERN);
        assertThat("100:00").doesNotMatch(DurationFormat.PATTERN);
        assertThat("01:60").doesNotMatch(DurationFormat.PATTERN);
        assertThat("1:5").doesNotMatch(DurationFormat.PATTERN);
        assertThat("90").doesNotMatch(DurationFormat.PATTERN);
    }
}

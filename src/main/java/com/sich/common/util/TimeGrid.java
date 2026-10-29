package com.sich.common.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Grade de horários da agenda: expediente, início dos agendamentos e duração dos
 * serviços são sempre múltiplos de {@value #SLOT_MINUTES} minutos.
 */
public final class TimeGrid {

    public static final int SLOT_MINUTES = 15;

    /** Hora do dia no formato {@code HH:MM} (de {@code 00:00} a {@code 23:59}). */
    public static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private TimeGrid() {
    }

    public static boolean isAligned(int minutes) {
        return minutes % SLOT_MINUTES == 0;
    }

    public static boolean isAligned(LocalTime time) {
        return time.getSecond() == 0 && time.getNano() == 0 && isAligned(time.getMinute());
    }

    /** Arredonda para o próximo horário da grade (14:07 vira 14:15; 14:15 continua 14:15). */
    public static LocalDateTime ceil(LocalDateTime dateTime) {
        LocalDateTime truncated = dateTime.truncatedTo(ChronoUnit.MINUTES);
        if (truncated.isBefore(dateTime)) {
            truncated = truncated.plusMinutes(1);
        }
        int remainder = truncated.getMinute() % SLOT_MINUTES;
        return remainder == 0 ? truncated : truncated.plusMinutes(SLOT_MINUTES - remainder);
    }

    public static LocalTime parseTime(String time) {
        return LocalTime.parse(time, TIME_FORMAT);
    }

    public static String formatTime(LocalTime time) {
        return time.format(TIME_FORMAT);
    }
}

package com.sich.common.util;

/**
 * Converte a duração exibida na tela no formato {@code HH:MM} (de {@code 00:00} a
 * {@code 99:59}) para minutos, que é como ela fica salva no banco, e vice-versa.
 */
public final class DurationFormat {

    /** Horas com 1 ou 2 dígitos e minutos de 00 a 59. */
    public static final String PATTERN = "^\\d{1,2}:[0-5]\\d$";

    private DurationFormat() {
    }

    public static int toMinutes(String duration) {
        String[] parts = duration.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public static String format(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}

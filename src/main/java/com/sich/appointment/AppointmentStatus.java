package com.sich.appointment;

public enum AppointmentStatus {
    /** Horário reservado na agenda do prestador. */
    SCHEDULED,
    /** Cancelado pelo prestador ou pelo cliente; o horário volta a ficar livre. */
    CANCELED
}

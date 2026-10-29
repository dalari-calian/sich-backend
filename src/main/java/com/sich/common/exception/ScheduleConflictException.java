package com.sich.common.exception;

/** Horário já ocupado na agenda do prestador (409). */
public class ScheduleConflictException extends RuntimeException {

    public ScheduleConflictException(String message) {
        super(message);
    }
}

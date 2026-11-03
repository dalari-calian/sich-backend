package com.sich.appointment.dto;

import java.time.LocalDate;
import java.util.List;

import com.sich.timeblock.dto.TimeBlockResponse;

/**
 * Um dia da grade da agenda do prestador: expediente, pausas entre os intervalos
 * (ex.: almoço), bloqueios que encostam no dia e agendamentos (inclusive cancelados)
 * que começam nele. {@code weekday} segue o padrão do backend: 1 = segunda ... 7 = domingo.
 */
public record AgendaDayResponse(
        LocalDate date,
        int weekday,
        List<AgendaIntervalResponse> workingHours,
        List<AgendaIntervalResponse> breaks,
        List<TimeBlockResponse> blocks,
        List<AppointmentResponse> appointments) {
}

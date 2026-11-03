package com.sich.appointment;

/** Quantidade de agendamentos de um prestador (resultado de consulta agregada). */
public record ProviderAppointmentCount(
        Long providerId,
        Long count) {
}

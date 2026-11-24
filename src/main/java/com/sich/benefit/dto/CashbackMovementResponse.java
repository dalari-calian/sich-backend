package com.sich.benefit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.sich.benefit.CashbackStatus;

/**
 * Linha do extrato da carteira: um agendamento que gerou ({@code cashbackEarned}) ou
 * usou ({@code cashbackUsed}) cashback. Os bônus do cartão fidelidade não aparecem
 * aqui; eles estão em {@code loyalty.bonusEarnedTotal} de {@code /customers/me/benefits}.
 */
public record CashbackMovementResponse(
        Long appointmentId,
        LocalDateTime startAt,
        Long providerId,
        String providerName,
        BigDecimal totalPrice,
        BigDecimal cashbackUsed,
        BigDecimal cashbackEarned,
        CashbackStatus status) {
}

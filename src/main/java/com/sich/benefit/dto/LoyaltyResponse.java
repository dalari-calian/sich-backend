package com.sich.benefit.dto;

import java.math.BigDecimal;

/**
 * Cartão fidelidade: a cada {@code goal} atendimentos realizados o cliente completa um
 * cartão e ganha {@code bonusPerCard} de cashback. {@code progress}/{@code goal} é o
 * "3/5" do perfil e {@code remaining} quantos faltam para o próximo bônus.
 */
public record LoyaltyResponse(
        int goal,
        int progress,
        int remaining,
        long completedCards,
        long completedAppointments,
        BigDecimal bonusPerCard,
        BigDecimal bonusEarnedTotal) {
}

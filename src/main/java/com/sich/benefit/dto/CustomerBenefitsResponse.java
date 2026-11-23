package com.sich.benefit.dto;

import java.math.BigDecimal;

/**
 * Carteira de benefícios do cliente.
 * <ul>
 * <li>{@code cashbackBalance}: saldo disponível para usar em um agendamento;</li>
 * <li>{@code cashbackPending}: cashback dos agendamentos que ainda vão acontecer (entra no saldo quando terminarem);</li>
 * <li>{@code cashbackEarnedTotal}: tudo o que já entrou no saldo (atendimentos realizados + bônus de fidelidade);</li>
 * <li>{@code cashbackUsedTotal}: tudo o que foi usado como desconto em agendamentos não cancelados;</li>
 * <li>{@code cashbackRate}: percentual do valor pago que volta como cashback (0.05 = 5%);</li>
 * <li>{@code maxCashbackUsageRate}: até quanto do valor do agendamento pode ser pago com cashback (0.20 = 20%).
 * O desconto aplicado é {@code min(cashbackBalance, total × maxCashbackUsageRate)}, arredondado para baixo.</li>
 * </ul>
 */
public record CustomerBenefitsResponse(
        BigDecimal cashbackBalance,
        BigDecimal cashbackPending,
        BigDecimal cashbackEarnedTotal,
        BigDecimal cashbackUsedTotal,
        BigDecimal cashbackRate,
        BigDecimal maxCashbackUsageRate,
        LoyaltyResponse loyalty) {
}

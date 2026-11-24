package com.sich.benefit;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Regras do programa de benefícios ({@code app.benefits.*} no application.properties).
 * Os valores aqui são os padrões usados quando a propriedade não é informada.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.benefits")
public class BenefitProperties {

    /** Percentual do valor pago que volta como cashback (0.05 = 5%). */
    private BigDecimal cashbackRate = new BigDecimal("0.05");

    /** Até quanto do valor do agendamento pode ser pago com cashback (0.20 = 20%). */
    private BigDecimal maxCashbackUsageRate = new BigDecimal("0.20");

    /** Atendimentos realizados para completar um cartão fidelidade. */
    private int loyaltyGoal = 5;

    /** Bônus de cashback (R$) creditado a cada cartão fidelidade completo. */
    private BigDecimal loyaltyBonus = new BigDecimal("10.00");
}

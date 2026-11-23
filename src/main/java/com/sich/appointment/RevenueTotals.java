package com.sich.appointment;

import java.math.BigDecimal;

/**
 * Quantidade de agendamentos, soma dos totais e soma do cashback usado em um período
 * (resultado de consulta agregada). Somas sem linhas vêm nulas do banco e viram zero.
 */
public record RevenueTotals(Long count, BigDecimal gross, BigDecimal cashback) {

    public RevenueTotals {
        count = count == null ? 0L : count;
        gross = gross == null ? BigDecimal.ZERO : gross;
        cashback = cashback == null ? BigDecimal.ZERO : cashback;
    }

    /** Valor efetivamente pago pelos clientes: total menos o desconto de cashback. */
    public BigDecimal net() {
        return gross.subtract(cashback);
    }
}

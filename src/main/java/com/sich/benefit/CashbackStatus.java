package com.sich.benefit;

/** Situação do cashback de um agendamento no extrato da carteira. */
public enum CashbackStatus {

    /** Atendimento realizado: o cashback ganho já está no saldo. */
    CREDITED,

    /** Atendimento ainda não terminou: o cashback ganho entra no saldo depois. */
    PENDING,

    /** Agendamento cancelado: o cashback não entra e o desconto usado voltou para o saldo. */
    CANCELED
}

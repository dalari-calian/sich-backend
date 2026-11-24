package com.sich.appointment;

import java.math.BigDecimal;

/** Quantas vezes um serviço foi realizado e quanto faturou (resultado de consulta agregada). */
public record ProductRevenue(String productName, Long count, BigDecimal revenue) {
}

package com.sich.financial.dto;

import java.math.BigDecimal;

/** Serviço no ranking de faturamento: quantas vezes foi realizado e quanto faturou (preço do serviço, antes do cashback). */
public record ProductRevenueResponse(
        String productName,
        long count,
        BigDecimal revenue) {
}

package com.sich.product;

import java.math.BigDecimal;

/** Menor preço entre os produtos ativos de um prestador (resultado de consulta agregada). */
public record ProviderMinPrice(Long providerId, BigDecimal minPrice) {
}

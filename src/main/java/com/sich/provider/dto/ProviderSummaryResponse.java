package com.sich.provider.dto;

import java.math.BigDecimal;
import java.util.List;

import com.sich.provider.ProviderEntity;

/**
 * Card do prestador na busca e no perfil: {@code minPrice} é o menor preço entre os
 * produtos ativos ("a partir de") e {@code categories} as categorias em que ele atende.
 */
public record ProviderSummaryResponse(
        Long id,
        String name,
        String city,
        String state,
        String neighborhood,
        BigDecimal minPrice,
        List<String> categories) {

    public static ProviderSummaryResponse of(ProviderEntity provider, BigDecimal minPrice, List<String> categories) {
        return new ProviderSummaryResponse(
                provider.getId(),
                provider.getName(),
                provider.getCity(),
                provider.getState(),
                provider.getNeighborhood(),
                minPrice,
                categories);
    }
}

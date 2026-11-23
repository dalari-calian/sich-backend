package com.sich.provider.dto;

import java.math.BigDecimal;
import java.util.List;

import com.sich.provider.ProviderEntity;

/**
 * Card do prestador na busca, nos favoritos e no perfil: {@code minPrice} é o menor
 * preço entre os produtos ativos ("a partir de"), {@code categories} as categorias em
 * que ele atende, {@code photo} a foto de perfil (data URL, ou nulo) e
 * {@code favorite} se o cliente autenticado o favoritou (sempre {@code false} para
 * quem não é cliente).
 */
public record ProviderSummaryResponse(
        Long id,
        String name,
        String city,
        String state,
        String neighborhood,
        BigDecimal minPrice,
        List<String> categories,
        String photo,
        boolean favorite) {

    public static ProviderSummaryResponse of(ProviderEntity provider, BigDecimal minPrice, List<String> categories,
            String photo, boolean favorite) {
        return new ProviderSummaryResponse(
                provider.getId(),
                provider.getName(),
                provider.getCity(),
                provider.getState(),
                provider.getNeighborhood(),
                minPrice,
                categories,
                photo,
                favorite);
    }
}

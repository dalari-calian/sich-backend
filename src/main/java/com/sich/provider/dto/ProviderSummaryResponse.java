package com.sich.provider.dto;

import com.sich.provider.ProviderEntity;

public record ProviderSummaryResponse(
        Long id,
        String name,
        String city,
        String state,
        String neighborhood) {

    public static ProviderSummaryResponse of(ProviderEntity provider) {
        return new ProviderSummaryResponse(
                provider.getId(),
                provider.getName(),
                provider.getCity(),
                provider.getState(),
                provider.getNeighborhood());
    }
}

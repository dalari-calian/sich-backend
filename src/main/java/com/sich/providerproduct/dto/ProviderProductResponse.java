package com.sich.providerproduct.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.sich.providerproduct.ProviderProductEntity;

public record ProviderProductResponse(
        Long id,
        Long productId,
        String productName,
        String productSku,
        String description,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt) {

    public static ProviderProductResponse of(ProviderProductEntity providerProduct) {
        return new ProviderProductResponse(
                providerProduct.getId(),
                providerProduct.getProduct().getId(),
                providerProduct.getProduct().getName(),
                providerProduct.getProduct().getSku(),
                providerProduct.getDescription(),
                providerProduct.getPrice(),
                providerProduct.getCreatedAt(),
                providerProduct.getUpdatedAt());
    }
}

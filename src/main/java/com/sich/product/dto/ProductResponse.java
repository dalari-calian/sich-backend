package com.sich.product.dto;

import java.time.Instant;

import com.sich.product.ProductEntity;

public record ProductResponse(
        Long id,
        Long parentId,
        String name,
        String description,
        String sku,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse of(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getParent() == null ? null : product.getParent().getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}

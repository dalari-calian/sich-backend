package com.sich.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.sich.common.util.DurationFormat;
import com.sich.product.ProductEntity;

public record ProductResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description,
        BigDecimal price,
        String duration,
        Integer durationMinutes,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse of(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                DurationFormat.format(product.getDurationMinutes()),
                product.getDurationMinutes(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}

package com.sich.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
        boolean active,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse of(ProductEntity product, List<ProductImageResponse> images) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                DurationFormat.format(product.getDurationMinutes()),
                product.getDurationMinutes(),
                product.isActive(),
                images,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}

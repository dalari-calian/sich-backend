package com.sich.product.dto;

import java.time.Instant;

import com.sich.product.ProductImageEntity;

public record ProductImageResponse(
        Long id,
        String image,
        Instant createdAt) {

    public static ProductImageResponse of(ProductImageEntity image) {
        return new ProductImageResponse(image.getId(), image.getImage(), image.getCreatedAt());
    }
}

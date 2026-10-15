package com.sich.category.dto;

import java.time.Instant;

import com.sich.category.CategoryEntity;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt) {

    public static CategoryResponse of(CategoryEntity category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}

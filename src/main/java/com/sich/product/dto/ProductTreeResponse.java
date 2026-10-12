package com.sich.product.dto;

import java.util.ArrayList;
import java.util.List;

import com.sich.product.ProductEntity;

public record ProductTreeResponse(
        Long id,
        String name,
        String description,
        String sku,
        List<ProductTreeResponse> children) {

    public static ProductTreeResponse of(ProductEntity product) {
        return new ProductTreeResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                new ArrayList<>());
    }
}

package com.hoya.aicommerce.catalog.presentation.response;

import com.hoya.aicommerce.catalog.application.dto.ProductResult;
import com.hoya.aicommerce.catalog.domain.ProductStatus;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        ProductStatus status,
        Long sellerId
) {
    public static ProductResponse from(ProductResult result) {
        return new ProductResponse(
                result.id(),
                result.name(),
                result.description(),
                result.price(),
                result.stockQuantity(),
                result.status(),
                result.sellerId()
        );
    }
}

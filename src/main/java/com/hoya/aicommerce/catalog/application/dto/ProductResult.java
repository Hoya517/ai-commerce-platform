package com.hoya.aicommerce.catalog.application.dto;

import com.hoya.aicommerce.catalog.domain.Product;
import com.hoya.aicommerce.catalog.domain.ProductStatus;

import java.math.BigDecimal;

public record ProductResult(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        ProductStatus status,
        Long sellerId
) {
    public static ProductResult from(Product product) {
        return new ProductResult(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice().getValue(),
                product.getStockQuantity(),
                product.getStatus(),
                product.getSellerId()
        );
    }
}

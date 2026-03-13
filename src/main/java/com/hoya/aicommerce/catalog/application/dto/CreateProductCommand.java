package com.hoya.aicommerce.catalog.application.dto;

import java.math.BigDecimal;

public record CreateProductCommand(
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        Long sellerId
) {}

package com.hoya.aicommerce.order.application.dto;

public record OrderItemCommand(
        Long productId,
        int quantity
) {}

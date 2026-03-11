package com.hoya.aicommerce.order.presentation.response;

import com.hoya.aicommerce.order.application.dto.OrderItemResult;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        BigDecimal price,
        int quantity
) {
    public static OrderItemResponse from(OrderItemResult result) {
        return new OrderItemResponse(
                result.productId(),
                result.productName(),
                result.price(),
                result.quantity()
        );
    }
}

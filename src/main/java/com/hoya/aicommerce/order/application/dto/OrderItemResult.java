package com.hoya.aicommerce.order.application.dto;

import com.hoya.aicommerce.order.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemResult(
        Long productId,
        String productName,
        BigDecimal price,
        int quantity
) {
    public static OrderItemResult from(OrderItem item) {
        return new OrderItemResult(
                item.getProductId(),
                item.getProductName(),
                item.getPrice().getValue(),
                item.getQuantity()
        );
    }
}

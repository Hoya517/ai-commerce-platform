package com.hoya.aicommerce.order.presentation.response;

import com.hoya.aicommerce.order.application.dto.OrderResult;
import com.hoya.aicommerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long memberId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(OrderResult result) {
        List<OrderItemResponse> items = result.items().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                result.orderId(),
                result.memberId(),
                result.status(),
                result.totalAmount(),
                items
        );
    }
}

package com.hoya.aicommerce.order.application.dto;

import com.hoya.aicommerce.order.domain.Order;
import com.hoya.aicommerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderResult(
        Long orderId,
        Long memberId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResult> items
) {
    public static OrderResult from(Order order) {
        List<OrderItemResult> items = order.getItems().stream()
                .map(OrderItemResult::from)
                .toList();
        return new OrderResult(
                order.getId(),
                order.getMemberId(),
                order.getStatus(),
                order.getTotalAmount().getValue(),
                items
        );
    }
}

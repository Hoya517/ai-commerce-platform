package com.hoya.aicommerce.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
        Long orderId,
        Long memberId,
        BigDecimal totalAmount,
        LocalDateTime occurredAt
) {
    public static OrderCreatedEvent of(Long orderId, Long memberId, BigDecimal totalAmount) {
        return new OrderCreatedEvent(orderId, memberId, totalAmount, LocalDateTime.now());
    }
}

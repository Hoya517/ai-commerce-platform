package com.hoya.aicommerce.common.kafka.message;

import com.hoya.aicommerce.common.event.OrderCreatedEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEventMessage(
        String eventId,
        Long orderId,
        Long memberId,
        BigDecimal totalAmount,
        LocalDateTime occurredAt
) {
    public static OrderCreatedEventMessage from(OrderCreatedEvent event) {
        return new OrderCreatedEventMessage(
                "order-" + event.orderId() + "-" + event.occurredAt().toEpochSecond(java.time.ZoneOffset.UTC),
                event.orderId(),
                event.memberId(),
                event.totalAmount(),
                event.occurredAt()
        );
    }
}

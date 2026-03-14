package com.hoya.aicommerce.common.kafka.message;

import com.hoya.aicommerce.common.event.PaymentConfirmedEvent;
import com.hoya.aicommerce.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentConfirmedEventMessage(
        String eventId,
        Long paymentId,
        Long orderId,
        Long memberId,
        Long sellerId,
        BigDecimal amount,
        PaymentMethod method,
        LocalDateTime occurredAt
) {
    public static PaymentConfirmedEventMessage from(PaymentConfirmedEvent event) {
        return new PaymentConfirmedEventMessage(
                "payment-confirmed-" + event.paymentId() + "-" + event.occurredAt().toEpochSecond(java.time.ZoneOffset.UTC),
                event.paymentId(),
                event.orderId(),
                event.memberId(),
                event.sellerId(),
                event.amount(),
                event.method(),
                event.occurredAt()
        );
    }
}

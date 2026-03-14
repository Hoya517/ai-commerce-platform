package com.hoya.aicommerce.common.kafka.message;

import com.hoya.aicommerce.common.event.PaymentCanceledEvent;
import com.hoya.aicommerce.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCanceledEventMessage(
        String eventId,
        Long paymentId,
        Long orderId,
        Long memberId,
        Long sellerId,
        BigDecimal amount,
        PaymentMethod method,
        LocalDateTime occurredAt
) {
    public static PaymentCanceledEventMessage from(PaymentCanceledEvent event) {
        return new PaymentCanceledEventMessage(
                "payment-canceled-" + event.paymentId() + "-" + event.occurredAt().toEpochSecond(java.time.ZoneOffset.UTC),
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

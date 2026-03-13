package com.hoya.aicommerce.common.event;

import com.hoya.aicommerce.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentConfirmedEvent(
        Long paymentId,
        Long orderId,
        Long memberId,
        Long sellerId,
        BigDecimal amount,
        PaymentMethod method,
        LocalDateTime occurredAt
) {
    public static PaymentConfirmedEvent of(
            Long paymentId, Long orderId, Long memberId, Long sellerId, BigDecimal amount, PaymentMethod method) {
        return new PaymentConfirmedEvent(paymentId, orderId, memberId, sellerId, amount, method, LocalDateTime.now());
    }
}

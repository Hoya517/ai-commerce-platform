package com.hoya.aicommerce.common.event;

import com.hoya.aicommerce.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCanceledEvent(
        Long paymentId,
        Long orderId,
        Long memberId,
        BigDecimal amount,
        PaymentMethod method,
        LocalDateTime occurredAt
) {
    public static PaymentCanceledEvent of(
            Long paymentId, Long orderId, Long memberId, BigDecimal amount, PaymentMethod method) {
        return new PaymentCanceledEvent(paymentId, orderId, memberId, amount, method, LocalDateTime.now());
    }
}

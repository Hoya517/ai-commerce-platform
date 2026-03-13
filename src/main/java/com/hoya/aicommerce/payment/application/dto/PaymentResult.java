package com.hoya.aicommerce.payment.application.dto;

import com.hoya.aicommerce.payment.domain.Payment;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import com.hoya.aicommerce.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResult(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime approvedAt
) {
    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount().getValue(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getApprovedAt()
        );
    }
}

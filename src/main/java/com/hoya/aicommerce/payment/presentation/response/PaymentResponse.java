package com.hoya.aicommerce.payment.presentation.response;

import com.hoya.aicommerce.payment.application.dto.PaymentResult;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import com.hoya.aicommerce.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        PaymentMethod method,
        PaymentStatus status,
        LocalDateTime approvedAt
) {
    public static PaymentResponse from(PaymentResult result) {
        return new PaymentResponse(
                result.paymentId(),
                result.orderId(),
                result.amount(),
                result.method(),
                result.status(),
                result.approvedAt()
        );
    }
}

package com.hoya.aicommerce.payment.application.dto;

import com.hoya.aicommerce.payment.domain.PaymentMethod;

public record RequestPaymentCommand(
        Long orderId,
        PaymentMethod method
) {}

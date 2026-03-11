package com.hoya.aicommerce.payment.application.dto;

public record ConfirmPaymentCommand(
        Long paymentId,
        String paymentKey
) {}

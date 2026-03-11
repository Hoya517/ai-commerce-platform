package com.hoya.aicommerce.payment.application.dto;

public record FailPaymentCommand(
        Long paymentId,
        String failureCode,
        String failureMessage
) {}

package com.hoya.aicommerce.payment.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FailPaymentRequest(
        @NotNull Long paymentId,
        @NotBlank String failureCode,
        @NotBlank String failureMessage
) {}

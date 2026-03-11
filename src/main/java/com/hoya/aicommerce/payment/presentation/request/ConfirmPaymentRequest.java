package com.hoya.aicommerce.payment.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConfirmPaymentRequest(
        @NotNull Long paymentId,
        @NotBlank String paymentKey
) {}

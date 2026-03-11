package com.hoya.aicommerce.payment.presentation.request;

import com.hoya.aicommerce.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record RequestPaymentRequest(
        @NotNull Long orderId,
        @NotNull PaymentMethod method
) {}

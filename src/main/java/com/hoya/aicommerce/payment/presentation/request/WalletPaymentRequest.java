package com.hoya.aicommerce.payment.presentation.request;

import jakarta.validation.constraints.NotNull;

public record WalletPaymentRequest(
        @NotNull Long orderId
) {}

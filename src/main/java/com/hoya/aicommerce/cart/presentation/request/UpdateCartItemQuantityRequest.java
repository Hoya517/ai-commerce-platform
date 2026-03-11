package com.hoya.aicommerce.cart.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemQuantityRequest(
        @NotNull Long memberId,
        @Min(1) int quantity
) {}

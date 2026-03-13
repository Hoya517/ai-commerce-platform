package com.hoya.aicommerce.cart.presentation.request;

import jakarta.validation.constraints.Min;

public record UpdateCartItemQuantityRequest(
        @Min(1) int quantity
) {}

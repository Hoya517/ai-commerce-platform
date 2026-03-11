package com.hoya.aicommerce.cart.application.dto;

public record UpdateCartItemQuantityCommand(
        Long memberId,
        Long productId,
        int quantity
) {}

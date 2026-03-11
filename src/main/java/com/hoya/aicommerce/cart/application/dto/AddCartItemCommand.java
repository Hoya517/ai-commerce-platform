package com.hoya.aicommerce.cart.application.dto;

public record AddCartItemCommand(
        Long memberId,
        Long productId,
        int quantity
) {}

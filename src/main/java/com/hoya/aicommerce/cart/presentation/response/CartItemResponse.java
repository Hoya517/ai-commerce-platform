package com.hoya.aicommerce.cart.presentation.response;

import com.hoya.aicommerce.cart.application.dto.CartItemResult;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String nameSnapshot,
        BigDecimal price,
        int quantity
) {
    public static CartItemResponse from(CartItemResult result) {
        return new CartItemResponse(
                result.productId(),
                result.nameSnapshot(),
                result.price(),
                result.quantity()
        );
    }
}

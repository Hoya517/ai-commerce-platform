package com.hoya.aicommerce.cart.application.dto;

import com.hoya.aicommerce.cart.domain.CartItem;

import java.math.BigDecimal;

public record CartItemResult(
        Long productId,
        String nameSnapshot,
        BigDecimal price,
        int quantity
) {
    public static CartItemResult from(CartItem item) {
        return new CartItemResult(
                item.getProductId(),
                item.getNameSnapshot(),
                item.getPrice().getValue(),
                item.getQuantity()
        );
    }
}

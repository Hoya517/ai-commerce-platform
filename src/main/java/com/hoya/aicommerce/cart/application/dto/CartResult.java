package com.hoya.aicommerce.cart.application.dto;

import com.hoya.aicommerce.cart.domain.Cart;

import java.util.List;

public record CartResult(
        Long cartId,
        Long memberId,
        List<CartItemResult> items
) {
    public static CartResult from(Cart cart) {
        List<CartItemResult> items = cart.getItems().stream()
                .map(CartItemResult::from)
                .toList();
        return new CartResult(cart.getId(), cart.getMemberId(), items);
    }
}

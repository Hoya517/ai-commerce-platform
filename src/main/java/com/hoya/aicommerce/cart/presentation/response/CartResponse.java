package com.hoya.aicommerce.cart.presentation.response;

import com.hoya.aicommerce.cart.application.dto.CartResult;

import java.util.List;

public record CartResponse(
        Long cartId,
        Long memberId,
        List<CartItemResponse> items
) {
    public static CartResponse from(CartResult result) {
        List<CartItemResponse> items = result.items().stream()
                .map(CartItemResponse::from)
                .toList();
        return new CartResponse(result.cartId(), result.memberId(), items);
    }
}

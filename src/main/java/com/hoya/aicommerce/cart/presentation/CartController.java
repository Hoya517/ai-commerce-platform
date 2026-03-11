package com.hoya.aicommerce.cart.presentation;

import com.hoya.aicommerce.cart.application.CartService;
import com.hoya.aicommerce.cart.application.dto.AddCartItemCommand;
import com.hoya.aicommerce.cart.application.dto.UpdateCartItemQuantityCommand;
import com.hoya.aicommerce.cart.presentation.request.AddCartItemRequest;
import com.hoya.aicommerce.cart.presentation.request.UpdateCartItemQuantityRequest;
import com.hoya.aicommerce.cart.presentation.response.CartResponse;
import com.hoya.aicommerce.common.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cart", description = "장바구니 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "장바구니 조회")
    @GetMapping
    public ApiResponse<CartResponse> getCart(@RequestParam Long memberId) {
        return ApiResponse.success(CartResponse.from(cartService.getCart(memberId)));
    }

    @Operation(summary = "장바구니 상품 추가")
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        AddCartItemCommand command = new AddCartItemCommand(
                request.memberId(), request.productId(), request.quantity()
        );
        return ApiResponse.success(CartResponse.from(cartService.addItem(command)));
    }

    @Operation(summary = "장바구니 상품 수량 변경")
    @PatchMapping("/items/{productId}")
    public ApiResponse<CartResponse> updateItemQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
    ) {
        UpdateCartItemQuantityCommand command = new UpdateCartItemQuantityCommand(
                request.memberId(), productId, request.quantity()
        );
        return ApiResponse.success(CartResponse.from(cartService.updateItemQuantity(command)));
    }

    @Operation(summary = "장바구니 상품 삭제")
    @DeleteMapping("/items/{productId}")
    public ApiResponse<Void> removeItem(
            @PathVariable Long productId,
            @RequestParam Long memberId
    ) {
        cartService.removeItem(memberId, productId);
        return ApiResponse.success(null);
    }
}

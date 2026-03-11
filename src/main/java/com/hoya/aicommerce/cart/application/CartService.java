package com.hoya.aicommerce.cart.application;

import com.hoya.aicommerce.cart.application.dto.AddCartItemCommand;
import com.hoya.aicommerce.cart.application.dto.CartResult;
import com.hoya.aicommerce.cart.application.dto.UpdateCartItemQuantityCommand;
import com.hoya.aicommerce.cart.domain.Cart;
import com.hoya.aicommerce.cart.domain.CartRepository;
import com.hoya.aicommerce.cart.exception.CartException;
import com.hoya.aicommerce.catalog.domain.Product;
import com.hoya.aicommerce.catalog.domain.ProductRepository;
import com.hoya.aicommerce.catalog.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public CartResult getCart(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CartException("Cart not found"));
        return CartResult.from(cart);
    }

    @Transactional
    public CartResult addItem(AddCartItemCommand command) {
        Cart cart = cartRepository.findByMemberId(command.memberId())
                .orElseGet(() -> cartRepository.save(Cart.create(command.memberId())));

        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new ProductException("Product not found"));

        cart.addItem(
                product.getId(),
                product.getName(),
                product.getPrice(),
                command.quantity(),
                product.getStockQuantity(),
                product.isOnSale()
        );
        return CartResult.from(cart);
    }

    @Transactional
    public CartResult updateItemQuantity(UpdateCartItemQuantityCommand command) {
        Cart cart = cartRepository.findByMemberId(command.memberId())
                .orElseThrow(() -> new CartException("Cart not found"));

        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new ProductException("Product not found"));

        cart.updateItemQuantity(command.productId(), command.quantity(), product.getStockQuantity());
        return CartResult.from(cart);
    }

    @Transactional
    public void removeItem(Long memberId, Long productId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CartException("Cart not found"));
        cart.removeItem(productId);
    }
}

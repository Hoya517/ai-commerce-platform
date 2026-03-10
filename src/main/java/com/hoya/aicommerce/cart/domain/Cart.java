package com.hoya.aicommerce.cart.domain;

import com.hoya.aicommerce.cart.exception.CartException;
import com.hoya.aicommerce.common.domain.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    private Cart(Long memberId) {
        this.memberId = memberId;
    }

    public static Cart create(Long memberId) {
        return new Cart(memberId);
    }

    public void addItem(Long productId, String nameSnapshot, Money price, int quantity,
                        int availableStock, boolean isOnSale) {
        if (!isOnSale) {
            throw new CartException("Product is not on sale");
        }
        if (quantity <= 0) {
            throw new CartException("Quantity must be greater than 0");
        }

        CartItem existing = findItem(productId);
        if (existing != null) {
            int newQuantity = existing.getQuantity() + quantity;
            if (newQuantity > availableStock) {
                throw new CartException("Exceeds available stock");
            }
            existing.increaseQuantity(quantity);
        } else {
            if (quantity > availableStock) {
                throw new CartException("Exceeds available stock");
            }
            items.add(new CartItem(this, productId, nameSnapshot, price, quantity));
        }
    }

    public void removeItem(Long productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
    }

    private CartItem findItem(Long productId) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }
}

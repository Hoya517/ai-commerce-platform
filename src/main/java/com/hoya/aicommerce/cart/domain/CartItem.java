package com.hoya.aicommerce.cart.domain;

import com.hoya.aicommerce.common.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    private Long productId;
    private String nameSnapshot;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "price"))
    private Money price;

    private int quantity;

    CartItem(Cart cart, Long productId, String nameSnapshot, Money price, int quantity) {
        this.cart = cart;
        this.productId = productId;
        this.nameSnapshot = nameSnapshot;
        this.price = price;
        this.quantity = quantity;
    }

    void increaseQuantity(int amount) {
        this.quantity += amount;
    }
}

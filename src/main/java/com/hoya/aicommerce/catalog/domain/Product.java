package com.hoya.aicommerce.catalog.domain;

import com.hoya.aicommerce.catalog.exception.ProductException;
import com.hoya.aicommerce.common.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "price"))
    private Money price;

    private int stockQuantity;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private Long sellerId;

    private Product(String name, String description, Money price, int stockQuantity, Long sellerId) {
        if (price.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ProductException("Price must be greater than 0");
        }
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.status = ProductStatus.ON_SALE;
        this.sellerId = sellerId;
    }

    public static Product create(String name, String description, Money price, int stockQuantity, Long sellerId) {
        return new Product(name, description, price, stockQuantity, sellerId);
    }

    public void decreaseStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new ProductException("Insufficient stock");
        }
        this.stockQuantity -= quantity;
        if (this.stockQuantity == 0) {
            this.status = ProductStatus.OUT_OF_STOCK;
        }
    }

    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
        if (this.status == ProductStatus.OUT_OF_STOCK) {
            this.status = ProductStatus.ON_SALE;
        }
    }

    public boolean isOnSale() {
        return this.status == ProductStatus.ON_SALE;
    }

    public void changeStatus(ProductStatus newStatus) {
        this.status = newStatus;
    }
}

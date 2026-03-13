package com.hoya.aicommerce.order.domain;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.order.exception.OrderException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "total_amount"))
    private Money totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    private Order(Long memberId) {
        this.memberId = memberId;
        this.status = OrderStatus.CREATED;
        this.totalAmount = Money.zero();
    }

    public static Order create(Long memberId) {
        return new Order(memberId);
    }

    public void addItem(Long productId, String productName, Money price, int quantity) {
        items.add(new OrderItem(this, productId, productName, price, quantity));
        recalculateTotalAmount();
    }

    public void startPayment() {
        if (items.isEmpty()) {
            throw new OrderException("Order has no items");
        }
        if (status == OrderStatus.CANCELED) {
            throw new OrderException("Cannot start payment for a canceled order");
        }
        if (status == OrderStatus.PAID) {
            throw new OrderException("Order is already paid");
        }
        this.status = OrderStatus.PAYMENT_PENDING;
    }

    public void markPaid() {
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status == OrderStatus.PAID) {
            throw new OrderException("Cannot cancel a paid order");
        }
        this.status = OrderStatus.CANCELED;
    }

    public void refund() {
        if (status != OrderStatus.PAID) {
            throw new OrderException("Only paid orders can be refunded");
        }
        this.status = OrderStatus.CANCELED;
    }

    private void recalculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(item -> item.getPrice().multiply(item.getQuantity()))
                .reduce(Money.zero(), Money::add);
    }
}

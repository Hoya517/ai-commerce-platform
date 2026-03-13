package com.hoya.aicommerce.order.domain;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.order.exception.OrderException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void 주문이_생성된다() {
        Order order = Order.create(1L);
        assertThat(order.getMemberId()).isEqualTo(1L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getTotalAmount()).isEqualTo(Money.zero());
    }

    @Test
    void 항목이_추가된다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 2);

        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void 총액이_올바르게_계산된다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 2);
        order.addItem(20L, "상품B", Money.of(500L), 3);

        assertThat(order.getTotalAmount()).isEqualTo(Money.of(3500L));
    }

    @Test
    void startPayment가_PAYMENT_PENDING으로_전환된다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 1);
        order.startPayment();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void 항목이_없으면_startPayment_예외가_발생한다() {
        Order order = Order.create(1L);
        assertThatThrownBy(order::startPayment)
                .isInstanceOf(OrderException.class);
    }

    @Test
    void 취소된_주문은_startPayment_예외가_발생한다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 1);
        order.cancel();

        assertThatThrownBy(order::startPayment)
                .isInstanceOf(OrderException.class);
    }

    @Test
    void 결제완료된_주문은_startPayment_예외가_발생한다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 1);
        order.startPayment();
        order.markPaid();

        assertThatThrownBy(order::startPayment)
                .isInstanceOf(OrderException.class);
    }

    @Test
    void 주문이_취소된다() {
        Order order = Order.create(1L);
        order.cancel();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void PAID_상태에서는_취소할_수_없다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 1);
        order.startPayment();
        order.markPaid();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(OrderException.class);
    }

    @Test
    void PAID_주문이_환불된다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 1);
        order.startPayment();
        order.markPaid();
        order.refund();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void 미결제_주문은_환불할_수_없다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 1);

        assertThatThrownBy(order::refund)
                .isInstanceOf(OrderException.class);
    }
}

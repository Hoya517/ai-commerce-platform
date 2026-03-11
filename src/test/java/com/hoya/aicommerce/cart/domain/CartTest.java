package com.hoya.aicommerce.cart.domain;

import com.hoya.aicommerce.cart.exception.CartException;
import com.hoya.aicommerce.common.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    @Test
    void 장바구니가_생성된다() {
        Cart cart = Cart.create(1L);
        assertThat(cart.getMemberId()).isEqualTo(1L);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void 신규_항목이_추가된다() {
        Cart cart = Cart.create(1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void 동일_상품_추가시_수량이_증가한다() {
        Cart cart = Cart.create(1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);
        cart.addItem(10L, "상품A", Money.of(1000L), 3, 10, true);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void 수량이_0이면_예외가_발생한다() {
        Cart cart = Cart.create(1L);
        assertThatThrownBy(() -> cart.addItem(10L, "상품A", Money.of(1000L), 0, 10, true))
                .isInstanceOf(CartException.class);
    }

    @Test
    void 비판매_상품_추가시_예외가_발생한다() {
        Cart cart = Cart.create(1L);
        assertThatThrownBy(() -> cart.addItem(10L, "상품A", Money.of(1000L), 1, 10, false))
                .isInstanceOf(CartException.class);
    }

    @Test
    void 신규_항목_추가시_재고_초과하면_예외가_발생한다() {
        Cart cart = Cart.create(1L);
        assertThatThrownBy(() -> cart.addItem(10L, "상품A", Money.of(1000L), 5, 3, true))
                .isInstanceOf(CartException.class);
    }

    @Test
    void 기존_항목_수량_증가시_재고_초과하면_예외가_발생한다() {
        Cart cart = Cart.create(1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 3, 5, true);
        assertThatThrownBy(() -> cart.addItem(10L, "상품A", Money.of(1000L), 3, 5, true))
                .isInstanceOf(CartException.class);
    }

    @Test
    void 항목이_삭제된다() {
        Cart cart = Cart.create(1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);
        cart.removeItem(10L);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void 수량이_변경된다() {
        Cart cart = Cart.create(1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);
        cart.updateItemQuantity(10L, 7, 10);

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(7);
    }

    @Test
    void 존재하지_않는_항목_수량_변경시_예외가_발생한다() {
        Cart cart = Cart.create(1L);
        assertThatThrownBy(() -> cart.updateItemQuantity(99L, 3, 10))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("Item not found in cart");
    }

    @Test
    void 수량_변경시_0이하이면_예외가_발생한다() {
        Cart cart = Cart.create(1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);
        assertThatThrownBy(() -> cart.updateItemQuantity(10L, 0, 10))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("Quantity must be greater than 0");
    }

    @Test
    void 수량_변경시_재고_초과하면_예외가_발생한다() {
        Cart cart = Cart.create(1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);
        assertThatThrownBy(() -> cart.updateItemQuantity(10L, 11, 10))
                .isInstanceOf(CartException.class)
                .hasMessageContaining("Exceeds available stock");
    }
}

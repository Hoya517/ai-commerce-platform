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
import com.hoya.aicommerce.common.domain.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void 장바구니를_조회한다() {
        Cart cart = Cart.create(1L);
        given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));

        CartResult result = cartService.getCart(1L);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void 장바구니가_없으면_조회시_예외가_발생한다() {
        given(cartRepository.findByMemberId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart(99L))
                .isInstanceOf(CartException.class);
    }

    @Test
    void 장바구니에_상품이_추가된다() {
        Cart cart = Cart.create(1L);
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);

        given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));

        CartResult result = cartService.addItem(new AddCartItemCommand(1L, 10L, 2));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void 장바구니가_없으면_새로_생성하여_상품을_추가한다() {
        Cart newCart = Cart.create(1L);
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);

        given(cartRepository.findByMemberId(1L)).willReturn(Optional.empty());
        given(cartRepository.save(any())).willReturn(newCart);
        given(productRepository.findById(10L)).willReturn(Optional.of(product));

        CartResult result = cartService.addItem(new AddCartItemCommand(1L, 10L, 2));

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.items()).hasSize(1);
        verify(cartRepository).save(any());
    }

    @Test
    void 판매중이_아닌_상품은_장바구니에_추가할_수_없다() {
        Cart cart = Cart.create(1L);
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        product.changeStatus(com.hoya.aicommerce.catalog.domain.ProductStatus.HIDDEN);

        given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem(new AddCartItemCommand(1L, 10L, 2)))
                .isInstanceOf(CartException.class);
    }

    @Test
    void 장바구니_상품_수량이_변경된다() {
        Cart cart = Cart.create(1L);
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);

        given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
        given(productRepository.findById(10L)).willReturn(Optional.of(product));

        CartResult result = cartService.updateItemQuantity(new UpdateCartItemQuantityCommand(1L, 10L, 5));

        assertThat(result.items().get(0).quantity()).isEqualTo(5);
    }

    @Test
    void 장바구니_상품이_삭제된다() {
        Cart cart = Cart.create(1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);

        given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));

        cartService.removeItem(1L, 10L);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void 존재하지_않는_상품은_장바구니에_추가할_수_없다() {
        Cart cart = Cart.create(1L);
        given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(new AddCartItemCommand(1L, 99L, 2)))
                .isInstanceOf(ProductException.class);
    }
}

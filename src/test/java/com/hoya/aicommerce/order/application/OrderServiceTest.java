package com.hoya.aicommerce.order.application;

import com.hoya.aicommerce.cart.domain.Cart;
import com.hoya.aicommerce.cart.domain.CartRepository;
import com.hoya.aicommerce.cart.exception.CartException;
import com.hoya.aicommerce.catalog.domain.Product;
import com.hoya.aicommerce.catalog.domain.ProductRepository;
import com.hoya.aicommerce.catalog.domain.ProductStatus;
import com.hoya.aicommerce.catalog.exception.ProductException;
import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.common.event.OrderCreatedEvent;
import com.hoya.aicommerce.order.application.dto.CreateOrderCommand;
import com.hoya.aicommerce.order.application.dto.OrderItemCommand;
import com.hoya.aicommerce.order.application.dto.OrderResult;
import com.hoya.aicommerce.order.domain.Order;
import com.hoya.aicommerce.order.domain.OrderRepository;
import com.hoya.aicommerce.order.domain.OrderStatus;
import com.hoya.aicommerce.order.exception.OrderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository, cartRepository, eventPublisher, Optional.empty());
    }

    @Test
    void 주문이_생성된다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        Order order = Order.create(1L);
        given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(product));
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        CreateOrderCommand command = new CreateOrderCommand(1L, List.of(new OrderItemCommand(10L, 2)));
        OrderResult result = orderService.createOrder(command);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
        assertThat(result.items()).hasSize(1);
        assertThat(product.getStockQuantity()).isEqualTo(8);
    }

    @Test
    void 주문_생성_시_OrderCreatedEvent가_발행된다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(product));
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(new CreateOrderCommand(1L, List.of(new OrderItemCommand(10L, 1))));

        verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void 장바구니_기반_주문_생성_시_OrderCreatedEvent가_발행된다() {
        Cart cart = Cart.create(1L);
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);
        given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
        given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(product));
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        orderService.createOrderFromCart(1L);

        verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void 판매중이_아닌_상품은_주문할_수_없다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        product.changeStatus(ProductStatus.HIDDEN);
        given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(product));

        CreateOrderCommand command = new CreateOrderCommand(1L, List.of(new OrderItemCommand(10L, 2)));

        assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(ProductException.class);
    }

    @Test
    void 존재하지_않는_상품으로_주문시_예외가_발생한다() {
        given(productRepository.findByIdWithLock(99L)).willReturn(Optional.empty());

        CreateOrderCommand command = new CreateOrderCommand(1L, List.of(new OrderItemCommand(99L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(ProductException.class);
    }

    @Test
    void 재고_초과_주문시_예외가_발생한다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 3, 1L);
        given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(product));

        CreateOrderCommand command = new CreateOrderCommand(1L, List.of(new OrderItemCommand(10L, 5)));

        assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(ProductException.class);
    }

    @Test
    void 장바구니_기반_주문이_생성되고_장바구니가_비워진다() {
        Cart cart = Cart.create(1L);
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        cart.addItem(10L, "상품A", Money.of(1000L), 2, 10, true);

        given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));
        given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(product));
        given(orderRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        OrderResult result = orderService.createOrderFromCart(1L);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.items()).hasSize(1);
        assertThat(product.getStockQuantity()).isEqualTo(8);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void 장바구니가_없으면_from_cart_주문_실패() {
        given(cartRepository.findByMemberId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrderFromCart(1L))
                .isInstanceOf(CartException.class);
    }

    @Test
    void 장바구니가_비어있으면_from_cart_주문_실패() {
        Cart cart = Cart.create(1L);
        given(cartRepository.findByMemberId(1L)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.createOrderFromCart(1L))
                .isInstanceOf(CartException.class);
    }

    @Test
    void 주문을_조회한다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 2);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        OrderResult result = orderService.getOrder(1L);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void 존재하지_않는_주문_조회시_예외가_발생한다() {
        given(orderRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(99L))
                .isInstanceOf(OrderException.class);
    }

    @Test
    void 주문이_취소된다() {
        Order order = Order.create(1L);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        orderService.cancelOrder(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void 주문_취소_시_재고가_복구된다() {
        Product product = Product.create("상품A", "설명", Money.of(1000L), 10, 1L);
        product.decreaseStock(2);
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 2);

        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(product));

        orderService.cancelOrder(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void 결제완료된_주문은_취소할_수_없다() {
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(1000L), 1);
        order.startPayment();
        order.markPaid();
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(OrderException.class);
    }
}

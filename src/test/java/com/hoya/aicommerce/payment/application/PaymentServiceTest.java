package com.hoya.aicommerce.payment.application;

import com.hoya.aicommerce.catalog.domain.Product;
import com.hoya.aicommerce.catalog.domain.ProductRepository;
import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.order.domain.Order;
import com.hoya.aicommerce.order.domain.OrderRepository;
import com.hoya.aicommerce.order.domain.OrderStatus;
import com.hoya.aicommerce.order.exception.OrderException;
import com.hoya.aicommerce.catalog.domain.ProductStatus;
import com.hoya.aicommerce.payment.application.dto.ConfirmPaymentCommand;
import com.hoya.aicommerce.payment.application.dto.FailPaymentCommand;
import com.hoya.aicommerce.payment.application.dto.PayWithWalletCommand;
import com.hoya.aicommerce.payment.application.dto.PaymentResult;
import com.hoya.aicommerce.payment.application.dto.RequestPaymentCommand;
import com.hoya.aicommerce.payment.domain.Payment;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import com.hoya.aicommerce.payment.domain.PaymentRepository;
import com.hoya.aicommerce.payment.domain.PaymentStatus;
import com.hoya.aicommerce.payment.exception.PaymentException;
import com.hoya.aicommerce.payment.infrastructure.pg.PgGateway;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgCancelResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgConfirmResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgPrepareResponse;
import com.hoya.aicommerce.wallet.application.WalletService;
import com.hoya.aicommerce.wallet.exception.WalletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private PgGateway pgGateway;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void 결제가_요청된다() {
        Order order = Order.create(1L);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.addItem(10L, "상품A", Money.of(1000L), 2);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(pgGateway.prepare(any(), any(), any())).willReturn(new PgPrepareResponse("mock-key-001"));

        PaymentResult result = paymentService.requestPayment(
                new RequestPaymentCommand(1L, PaymentMethod.CARD));

        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(PaymentStatus.REQUESTED);
        assertThat(result.method()).isEqualTo(PaymentMethod.CARD);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void 존재하지_않는_주문_결제_요청시_예외가_발생한다() {
        given(orderRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.requestPayment(
                new RequestPaymentCommand(99L, PaymentMethod.CARD)))
                .isInstanceOf(OrderException.class);
    }

    @Test
    void 결제가_승인된다() {
        Payment payment = Payment.create(1L, Money.of(2000L), PaymentMethod.CARD);
        payment.request("mock-pg-key-abc");  // requestPayment 단계에서 발급된 paymentKey
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(2000L), 1);
        order.startPayment();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(pgGateway.confirm(any(), any())).willReturn(new PgConfirmResponse(true, null, null));

        PaymentResult result = paymentService.confirmPayment(new ConfirmPaymentCommand(1L));

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.approvedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void PG_승인_실패시_결제가_FAILED_상태가_된다() {
        Payment payment = Payment.create(1L, Money.of(2000L), PaymentMethod.CARD);
        payment.request("mock-pg-key-abc");
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(2000L), 1);
        order.startPayment();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(pgGateway.confirm(any(), any()))
                .willReturn(new PgConfirmResponse(false, "CARD_LIMIT_EXCEEDED", "카드 한도가 초과되었습니다"));

        assertThatThrownBy(() -> paymentService.confirmPayment(new ConfirmPaymentCommand(1L)))
                .isInstanceOf(PaymentException.class)
                .hasMessage("카드 한도가 초과되었습니다");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void 존재하지_않는_결제_승인시_예외가_발생한다() {
        given(paymentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(
                new ConfirmPaymentCommand(99L)))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void 결제가_실패한다() {
        Payment payment = Payment.create(1L, Money.of(2000L), PaymentMethod.CARD);
        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));

        paymentService.failPayment(new FailPaymentCommand(1L, "CARD_DECLINED", "카드 한도 초과"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureCode()).isEqualTo("CARD_DECLINED");
        assertThat(payment.getFailureMessage()).isEqualTo("카드 한도 초과");
    }

    @Test
    void 존재하지_않는_결제_실패_처리시_예외가_발생한다() {
        given(paymentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.failPayment(
                new FailPaymentCommand(99L, "ERR", "오류")))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void 예치금으로_결제가_완료된다() {
        Order order = Order.create(1L);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.addItem(10L, "상품A", Money.of(5000L), 1);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        PaymentResult result = paymentService.payWithWallet(new PayWithWalletCommand(1L, 1L));

        assertThat(result.method()).isEqualTo(PaymentMethod.WALLET);
        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.approvedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void 결제_취소_성공_CARD결제() {
        Product product = Product.create("상품A", "설명", Money.of(2000L), 10, 1L);
        ReflectionTestUtils.setField(product, "id", 10L);

        Order order = Order.create(1L);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.addItem(10L, "상품A", Money.of(2000L), 1);
        order.startPayment();
        order.markPaid();

        Payment payment = Payment.create(1L, Money.of(2000L), PaymentMethod.CARD);
        ReflectionTestUtils.setField(payment, "id", 1L);
        payment.request("pay-key-test");
        payment.approve();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(product));
        given(pgGateway.cancel(any(), any())).willReturn(new PgCancelResponse(true, null, null));

        PaymentResult result = paymentService.cancelPayment(1L, 1L);

        assertThat(result.status()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(productRepository).findByIdWithLock(10L);
        verify(walletService, never()).charge(any(), any());
    }

    @Test
    void 결제_취소_성공_WALLET결제() {
        Product product = Product.create("상품A", "설명", Money.of(5000L), 10, 1L);
        ReflectionTestUtils.setField(product, "id", 10L);

        Order order = Order.create(1L);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.addItem(10L, "상품A", Money.of(5000L), 1);
        order.startPayment();
        order.markPaid();

        Payment payment = Payment.create(1L, Money.of(5000L), PaymentMethod.WALLET);
        ReflectionTestUtils.setField(payment, "id", 1L);
        payment.request(null);
        payment.approve();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(productRepository.findByIdWithLock(10L)).willReturn(Optional.of(product));

        PaymentResult result = paymentService.cancelPayment(1L, 1L);

        assertThat(result.status()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(walletService).charge(eq(1L), any());
    }

    @Test
    void 다른_회원의_결제는_취소할_수_없다() {
        Order order = Order.create(1L);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.addItem(10L, "상품A", Money.of(2000L), 1);
        order.startPayment();
        order.markPaid();

        Payment payment = Payment.create(1L, Money.of(2000L), PaymentMethod.CARD);
        ReflectionTestUtils.setField(payment, "id", 1L);
        payment.request("pay-key-test");
        payment.approve();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.cancelPayment(1L, 99L))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void 잔액_부족_시_예치금_결제가_실패한다() {
        Order order = Order.create(1L);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.addItem(10L, "상품A", Money.of(100000L), 1);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        willThrow(new WalletException("잔액이 부족합니다"))
                .given(walletService).deduct(any(), any());

        assertThatThrownBy(() -> paymentService.payWithWallet(new PayWithWalletCommand(1L, 1L)))
                .isInstanceOf(WalletException.class)
                .hasMessage("잔액이 부족합니다");
    }
}

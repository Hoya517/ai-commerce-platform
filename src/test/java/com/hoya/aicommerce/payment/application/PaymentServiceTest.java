package com.hoya.aicommerce.payment.application;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.order.domain.Order;
import com.hoya.aicommerce.order.domain.OrderRepository;
import com.hoya.aicommerce.order.domain.OrderStatus;
import com.hoya.aicommerce.order.exception.OrderException;
import com.hoya.aicommerce.payment.application.dto.ConfirmPaymentCommand;
import com.hoya.aicommerce.payment.application.dto.FailPaymentCommand;
import com.hoya.aicommerce.payment.application.dto.PaymentResult;
import com.hoya.aicommerce.payment.application.dto.RequestPaymentCommand;
import com.hoya.aicommerce.payment.domain.Payment;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import com.hoya.aicommerce.payment.domain.PaymentRepository;
import com.hoya.aicommerce.payment.domain.PaymentStatus;
import com.hoya.aicommerce.payment.exception.PaymentException;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void 결제가_요청된다() {
        Order order = Order.create(1L);
        ReflectionTestUtils.setField(order, "id", 1L);
        order.addItem(10L, "상품A", Money.of(1000L), 2);
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        PaymentResult result = paymentService.requestPayment(
                new RequestPaymentCommand(1L, PaymentMethod.CARD));

        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo(PaymentStatus.READY);
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
        Order order = Order.create(1L);
        order.addItem(10L, "상품A", Money.of(2000L), 1);
        order.startPayment();

        given(paymentRepository.findById(1L)).willReturn(Optional.of(payment));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        PaymentResult result = paymentService.confirmPayment(
                new ConfirmPaymentCommand(1L, "pay-key-abc"));

        assertThat(result.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.paymentKey()).isEqualTo("pay-key-abc");
        assertThat(result.approvedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void 존재하지_않는_결제_승인시_예외가_발생한다() {
        given(paymentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(
                new ConfirmPaymentCommand(99L, "pay-key-abc")))
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
}

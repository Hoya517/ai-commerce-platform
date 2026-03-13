package com.hoya.aicommerce.payment.application;

import com.hoya.aicommerce.catalog.domain.ProductRepository;
import com.hoya.aicommerce.order.domain.Order;
import com.hoya.aicommerce.order.domain.OrderRepository;
import com.hoya.aicommerce.order.exception.OrderException;
import com.hoya.aicommerce.payment.application.dto.ConfirmPaymentCommand;
import com.hoya.aicommerce.payment.application.dto.FailPaymentCommand;
import com.hoya.aicommerce.payment.application.dto.PayWithWalletCommand;
import com.hoya.aicommerce.payment.application.dto.PaymentResult;
import com.hoya.aicommerce.payment.application.dto.RequestPaymentCommand;
import com.hoya.aicommerce.payment.domain.Payment;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import com.hoya.aicommerce.payment.domain.PaymentRepository;
import com.hoya.aicommerce.payment.exception.PaymentException;
import com.hoya.aicommerce.payment.infrastructure.pg.PgGateway;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgCancelResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgConfirmResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgPrepareResponse;
import com.hoya.aicommerce.wallet.application.WalletService;
import com.hoya.aicommerce.wallet.application.dto.ChargeWalletCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final WalletService walletService;
    private final PgGateway pgGateway;

    @Transactional
    public PaymentResult requestPayment(RequestPaymentCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderException("Order not found"));

        order.startPayment();

        Payment payment = Payment.create(order.getId(), order.getTotalAmount(), command.method());

        // PG에 결제 준비 요청 → paymentKey 발급받아 Payment에 저장 (READY → REQUESTED)
        PgPrepareResponse pgResponse = pgGateway.prepare(order.getId(), order.getTotalAmount(), command.method());
        payment.request(pgResponse.paymentKey());

        return PaymentResult.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResult confirmPayment(ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentException("Payment not found"));

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new OrderException("Order not found"));

        // 서버가 내부 보유한 paymentKey로 PG에 승인 요청
        PgConfirmResponse pgResponse = pgGateway.confirm(payment.getPaymentKey(), order.getTotalAmount());

        if (!pgResponse.success()) {
            payment.fail(pgResponse.failureCode(), pgResponse.failureMessage());
            throw new PaymentException(pgResponse.failureMessage());
        }

        payment.approve();
        order.markPaid();

        return PaymentResult.from(payment);
    }

    @Transactional
    public PaymentResult payWithWallet(PayWithWalletCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderException("Order not found"));

        order.startPayment();
        walletService.deduct(command.memberId(), order.getTotalAmount());

        Payment payment = Payment.create(order.getId(), order.getTotalAmount(), PaymentMethod.WALLET);
        payment.request(null);
        payment.approve();
        order.markPaid();

        return PaymentResult.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResult cancelPayment(Long paymentId, Long memberId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found"));

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new OrderException("Order not found"));

        if (!order.getMemberId().equals(memberId)) {
            throw new PaymentException("Unauthorized to cancel this payment");
        }

        // WALLET 제외: PG에 취소 요청
        if (payment.getMethod() != PaymentMethod.WALLET) {
            PgCancelResponse pgResponse = pgGateway.cancel(payment.getPaymentKey(), payment.getAmount());
            if (!pgResponse.success()) {
                throw new PaymentException(pgResponse.failureMessage());
            }
        }

        payment.cancel();
        order.refund();

        order.getItems().forEach(item ->
                productRepository.findByIdWithLock(item.getProductId())
                        .ifPresent(product -> product.increaseStock(item.getQuantity()))
        );

        if (payment.getMethod() == PaymentMethod.WALLET) {
            walletService.charge(memberId, new ChargeWalletCommand(payment.getAmount().getValue()));
        }

        return PaymentResult.from(payment);
    }

    @Transactional
    public void failPayment(FailPaymentCommand command) {
        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentException("Payment not found"));
        payment.fail(command.failureCode(), command.failureMessage());
    }
}

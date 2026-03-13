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

    @Transactional
    public PaymentResult requestPayment(RequestPaymentCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderException("Order not found"));

        order.startPayment();

        Payment payment = Payment.create(order.getId(), order.getTotalAmount(), command.method());
        return PaymentResult.from(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResult confirmPayment(ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentException("Payment not found"));

        payment.request(command.paymentKey());
        payment.approve();

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new OrderException("Order not found"));
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

        payment.cancel();
        order.refund();

        order.getItems().forEach(item ->
                productRepository.findById(item.getProductId())
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

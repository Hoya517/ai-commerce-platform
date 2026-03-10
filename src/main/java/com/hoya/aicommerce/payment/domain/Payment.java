package com.hoya.aicommerce.payment.domain;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.payment.exception.PaymentException;
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

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "amount"))
    private Money amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String paymentKey;
    private LocalDateTime approvedAt;
    private String failureCode;
    private String failureMessage;

    private Payment(Long orderId, Money amount, PaymentMethod method) {
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.READY;
    }

    public static Payment create(Long orderId, Money amount, PaymentMethod method) {
        return new Payment(orderId, amount, method);
    }

    public void request(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.REQUESTED;
    }

    public void approve() {
        if (status == PaymentStatus.APPROVED) {
            throw new PaymentException("Payment is already approved");
        }
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail(String failureCode, String failureMessage) {
        this.status = PaymentStatus.FAILED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public void cancel() {
        if (status != PaymentStatus.APPROVED) {
            throw new PaymentException("Only approved payments can be canceled");
        }
        this.status = PaymentStatus.CANCELED;
    }
}

package com.hoya.aicommerce.payment.domain;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.payment.exception.PaymentException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void 결제가_READY_상태로_생성된다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);

        assertThat(payment.getOrderId()).isEqualTo(1L);
        assertThat(payment.getAmount()).isEqualTo(Money.of(10000L));
        assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
    }

    @Test
    void request_호출시_REQUESTED_상태로_전환된다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REQUESTED);
        assertThat(payment.getPaymentKey()).isEqualTo("pay-key-001");
    }

    @Test
    void approve_호출시_APPROVED_상태로_전환된다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");
        payment.approve();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void approve_호출시_approvedAt이_설정된다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");
        payment.approve();

        assertThat(payment.getApprovedAt()).isNotNull();
    }

    @Test
    void 중복_승인시_예외가_발생한다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");
        payment.approve();

        assertThatThrownBy(payment::approve)
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void fail_호출시_FAILED_상태와_이력이_저장된다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");
        payment.fail("CARD_DECLINED", "카드가 거절되었습니다");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureCode()).isEqualTo("CARD_DECLINED");
        assertThat(payment.getFailureMessage()).isEqualTo("카드가 거절되었습니다");
    }

    @Test
    void APPROVED_상태에서_cancel_호출시_CANCELED로_전환된다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");
        payment.approve();
        payment.cancel();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    void 미승인_상태에서_cancel_호출시_예외가_발생한다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");

        assertThatThrownBy(payment::cancel)
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void READY가_아닌_상태에서_request_호출시_예외가_발생한다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");

        assertThatThrownBy(() -> payment.request("pay-key-002"))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void REQUESTED가_아닌_상태에서_approve_호출시_예외가_발생한다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);

        assertThatThrownBy(payment::approve)
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void APPROVED_상태에서_fail_호출시_예외가_발생한다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");
        payment.approve();

        assertThatThrownBy(() -> payment.fail("ERR", "오류"))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void CANCELED_상태에서_fail_호출시_예외가_발생한다() {
        Payment payment = Payment.create(1L, Money.of(10000L), PaymentMethod.CARD);
        payment.request("pay-key-001");
        payment.approve();
        payment.cancel();

        assertThatThrownBy(() -> payment.fail("ERR", "오류"))
                .isInstanceOf(PaymentException.class);
    }
}

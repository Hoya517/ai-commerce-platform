package com.hoya.aicommerce.settlement.application;

import com.hoya.aicommerce.common.event.PaymentCanceledEvent;
import com.hoya.aicommerce.common.event.PaymentConfirmedEvent;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;

class SettlementEventListenerTest {

    private final SettlementEventListener listener = new SettlementEventListener();

    @Test
    void PaymentConfirmedEvent_수신_시_예외_없이_처리된다() {
        PaymentConfirmedEvent event = PaymentConfirmedEvent.of(
                1L, 1L, 1L, BigDecimal.valueOf(50000), PaymentMethod.CARD);

        assertThatCode(() -> listener.onPaymentConfirmed(event))
                .doesNotThrowAnyException();
    }

    @Test
    void PaymentCanceledEvent_수신_시_예외_없이_처리된다() {
        PaymentCanceledEvent event = PaymentCanceledEvent.of(
                1L, 1L, 1L, BigDecimal.valueOf(50000), PaymentMethod.CARD);

        assertThatCode(() -> listener.onPaymentCanceled(event))
                .doesNotThrowAnyException();
    }

    @Test
    void WALLET_결제_확정_이벤트도_처리된다() {
        PaymentConfirmedEvent event = PaymentConfirmedEvent.of(
                2L, 2L, 1L, BigDecimal.valueOf(30000), PaymentMethod.WALLET);

        assertThatCode(() -> listener.onPaymentConfirmed(event))
                .doesNotThrowAnyException();
    }
}

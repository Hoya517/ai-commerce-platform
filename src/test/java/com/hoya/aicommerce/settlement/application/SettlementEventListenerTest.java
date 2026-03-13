package com.hoya.aicommerce.settlement.application;

import com.hoya.aicommerce.common.event.PaymentCanceledEvent;
import com.hoya.aicommerce.common.event.PaymentConfirmedEvent;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementEventListenerTest {

    @Mock
    private SettlementService settlementService;

    @InjectMocks
    private SettlementEventListener listener;

    @Test
    void PaymentConfirmedEvent_수신_시_settlementService_accumulate_호출() {
        PaymentConfirmedEvent event = PaymentConfirmedEvent.of(
                1L, 1L, 1L, 10L, BigDecimal.valueOf(50000), PaymentMethod.CARD);

        listener.onPaymentConfirmed(event);

        verify(settlementService).accumulate(eq(10L), any());
    }

    @Test
    void PaymentCanceledEvent_수신_시_settlementService_deductPayment_호출() {
        PaymentCanceledEvent event = PaymentCanceledEvent.of(
                1L, 1L, 1L, 10L, BigDecimal.valueOf(50000), PaymentMethod.CARD);

        listener.onPaymentCanceled(event);

        verify(settlementService).deductPayment(eq(10L), any());
    }

    @Test
    void sellerId가_null인_이벤트도_예외_없이_처리된다() {
        PaymentConfirmedEvent event = PaymentConfirmedEvent.of(
                1L, 1L, 1L, null, BigDecimal.valueOf(50000), PaymentMethod.CARD);

        assertThatCode(() -> listener.onPaymentConfirmed(event))
                .doesNotThrowAnyException();
    }
}

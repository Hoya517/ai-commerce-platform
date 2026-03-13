package com.hoya.aicommerce.settlement.application;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.common.event.PaymentCanceledEvent;
import com.hoya.aicommerce.common.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 이벤트를 구독하여 정산 처리를 담당하는 리스너.
 * PaymentService는 SettlementEventListener를 직접 알지 못함 — 느슨한 결합.
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT): 결제 트랜잭션 커밋 완료 후에만 실행.
 *   → DB 롤백 시 이벤트 발행 자체가 취소되므로 정산 중복 실행 방지.
 * @Async: 결제 응답을 블로킹하지 않고 별도 스레드에서 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final SettlementService settlementService;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("[Settlement] 정산 누적 — paymentId={}, sellerId={}, amount={}",
                event.paymentId(), event.sellerId(), event.amount());
        settlementService.accumulate(event.sellerId(), Money.of(event.amount()));
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCanceled(PaymentCanceledEvent event) {
        log.info("[Settlement] 정산 취소 예약 — paymentId={}, sellerId={}, amount={}",
                event.paymentId(), event.sellerId(), event.amount());
        // TODO: 정산 차감 처리 (ISSUE-20 배치/정산 확정 시 구현)
    }
}

package com.hoya.aicommerce.settlement.application.kafka;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.common.kafka.idempotency.EventIdempotencyService;
import com.hoya.aicommerce.common.kafka.message.PaymentCanceledEventMessage;
import com.hoya.aicommerce.common.kafka.message.PaymentConfirmedEventMessage;
import com.hoya.aicommerce.settlement.application.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka payment-events 토픽을 구독하여 정산을 처리하는 컨슈머.
 * kafka.enabled=true 일 때만 활성화 (SettlementEventListener 대신 사용).
 *
 * - manual commit: 처리 완료 후에만 오프셋 커밋 (at-least-once delivery)
 * - 중복 방지: EventIdempotencyService로 processed_event 테이블 확인
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class SettlementKafkaConsumer {

    private final SettlementService settlementService;
    private final EventIdempotencyService idempotencyService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "settlement-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Object rawMessage, Acknowledgment ack) {
        // 타입 분기: PaymentConfirmedEventMessage / PaymentCanceledEventMessage
        if (rawMessage instanceof PaymentConfirmedEventMessage message) {
            handleConfirmed(message);
        } else if (rawMessage instanceof PaymentCanceledEventMessage message) {
            handleCanceled(message);
        } else {
            log.warn("[SettlementConsumer] 알 수 없는 메시지 타입 — type={}", rawMessage.getClass().getName());
        }
        ack.acknowledge();
    }

    private void handleConfirmed(PaymentConfirmedEventMessage message) {
        if (idempotencyService.isAlreadyProcessed(message.eventId())) {
            log.info("[SettlementConsumer] 중복 이벤트 스킵 — eventId={}", message.eventId());
            return;
        }
        log.info("[SettlementConsumer] 정산 누적 — paymentId={}, sellerId={}, amount={}",
                message.paymentId(), message.sellerId(), message.amount());
        settlementService.accumulate(message.sellerId(), Money.of(message.amount()));
        idempotencyService.markProcessed(message.eventId());
    }

    private void handleCanceled(PaymentCanceledEventMessage message) {
        if (idempotencyService.isAlreadyProcessed(message.eventId())) {
            log.info("[SettlementConsumer] 중복 이벤트 스킵 — eventId={}", message.eventId());
            return;
        }
        log.info("[SettlementConsumer] 정산 차감 — paymentId={}, sellerId={}, amount={}",
                message.paymentId(), message.sellerId(), message.amount());
        settlementService.deductPayment(message.sellerId(), Money.of(message.amount()));
        idempotencyService.markProcessed(message.eventId());
    }
}

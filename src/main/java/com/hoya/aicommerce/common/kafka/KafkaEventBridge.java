package com.hoya.aicommerce.common.kafka;

import com.hoya.aicommerce.common.event.OrderCreatedEvent;
import com.hoya.aicommerce.common.event.PaymentCanceledEvent;
import com.hoya.aicommerce.common.event.PaymentConfirmedEvent;
import com.hoya.aicommerce.common.kafka.message.OrderCreatedEventMessage;
import com.hoya.aicommerce.common.kafka.message.PaymentCanceledEventMessage;
import com.hoya.aicommerce.common.kafka.message.PaymentConfirmedEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Spring ApplicationEvent → Kafka Topic 브리지.
 * kafka.enabled=true 일 때만 활성화. SettlementEventListener 대신 이 컴포넌트가 이벤트를 처리.
 *
 * Domain Service → Spring Event → KafkaEventBridge → Kafka Topic → Consumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaEventBridge {

    private static final String ORDER_EVENTS_TOPIC = "order-events";
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        OrderCreatedEventMessage message = OrderCreatedEventMessage.from(event);
        log.info("[KafkaBridge] 주문 생성 이벤트 발행 — orderId={}", event.orderId());
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, String.valueOf(event.orderId()), message);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        PaymentConfirmedEventMessage message = PaymentConfirmedEventMessage.from(event);
        log.info("[KafkaBridge] 결제 승인 이벤트 발행 — paymentId={}", event.paymentId());
        kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, String.valueOf(event.paymentId()), message);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCanceled(PaymentCanceledEvent event) {
        PaymentCanceledEventMessage message = PaymentCanceledEventMessage.from(event);
        log.info("[KafkaBridge] 결제 취소 이벤트 발행 — paymentId={}", event.paymentId());
        kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, String.valueOf(event.paymentId()), message);
    }
}

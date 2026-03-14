package com.hoya.aicommerce.catalog.application.kafka;

import com.hoya.aicommerce.catalog.application.StockService;
import com.hoya.aicommerce.common.kafka.message.StockDecreaseEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Redis 선차감 이후 DB 재고를 비동기로 반영하는 Kafka 컨슈머.
 * kafka.enabled=true 일 때만 활성화 (StockDecreaseWorker 대체).
 *
 * @RetryableTopic: 실패 시 자동으로 retry 토픽 → DLQ 로 이동
 *   stock-decrease-events
 *   └── stock-decrease-events-retry-0  (1차 재시도, 1초 후)
 *   └── stock-decrease-events-retry-1  (2차 재시도, 2초 후)
 *   └── stock-decrease-events-dlq      (최종 실패 격리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class StockDecreaseKafkaConsumer {

    private final Optional<StockService> stockService;

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true",
            kafkaTemplate = "kafkaTemplate",
            listenerContainerFactory = "retryableKafkaListenerContainerFactory"
    )
    @KafkaListener(topics = "stock-decrease-events", groupId = "stock-worker-group")
    public void consume(StockDecreaseEventMessage message) {
        log.info("[StockConsumer] DB 재고 차감 — productId={}, qty={}, orderId={}",
                message.productId(), message.quantity(), message.orderId());

        stockService.ifPresentOrElse(
                service -> service.decreaseInDb(message.productId(), message.quantity()),
                () -> log.warn("[StockConsumer] StockService 없음 (redis.enabled=false). 메시지 스킵 — productId={}", message.productId())
        );
    }

    /**
     * 3회 재시도 모두 실패 시 DLQ로 이동. 수동 처리 필요.
     */
    @DltHandler
    public void handleDlt(StockDecreaseEventMessage message) {
        log.error("[StockDLQ] 재고 차감 최종 실패 — productId={}, qty={}, orderId={}. 수동 처리 필요.",
                message.productId(), message.quantity(), message.orderId());

        // Redis 선차감 보상 (DB 반영 실패 → Redis 재고 원복)
        stockService.ifPresent(service -> {
            try {
                service.compensate(message.productId(), message.quantity());
                log.info("[StockDLQ] Redis 선차감 보상 완료 — productId={}", message.productId());
            } catch (Exception e) {
                log.error("[StockDLQ] Redis 보상도 실패 — productId={}. 즉시 수동 개입 필요.", message.productId(), e);
            }
        });
    }
}

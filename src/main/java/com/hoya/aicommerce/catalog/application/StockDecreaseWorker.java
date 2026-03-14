package com.hoya.aicommerce.catalog.application;

import com.hoya.aicommerce.common.event.StockDecreaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Redis 선차감 이후 DB에 재고를 비동기로 반영하는 워커.
 * redis.enabled=true AND kafka.enabled=false 일 때만 활성화.
 * kafka.enabled=true 이면 StockDecreaseKafkaConsumer가 대신 처리.
 *
 * 흐름:
 *   1. 주문 트랜잭션 커밋 후 이벤트 수신 (AFTER_COMMIT)
 *   2. DB 재고 차감
 *   3. 실패 시 Redis 재고 복구 (보상)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("'${redis.enabled:false}' == 'true' and '${kafka.enabled:false}' != 'true'")
public class StockDecreaseWorker {

    private final StockService stockService;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(StockDecreaseEvent event) {
        try {
            stockService.decreaseInDb(event.productId(), event.quantity());
            log.debug("DB stock decreased: product={}, qty={}, order={}",
                    event.productId(), event.quantity(), event.orderId());
        } catch (Exception e) {
            log.error("DB stock decrease failed for product={}, qty={}, order={}. Compensating Redis.",
                    event.productId(), event.quantity(), event.orderId(), e);
            try {
                stockService.compensate(event.productId(), event.quantity());
            } catch (Exception compensateEx) {
                log.error("Redis compensation also failed for product={}. Manual intervention required.",
                        event.productId(), compensateEx);
            }
        }
    }
}

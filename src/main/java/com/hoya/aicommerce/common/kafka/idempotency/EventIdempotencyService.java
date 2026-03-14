package com.hoya.aicommerce.common.kafka.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka at-least-once delivery 특성으로 인한 중복 이벤트 방지.
 * processed_event 테이블에 eventId를 저장하여 이미 처리된 이벤트를 스킵.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class EventIdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean isAlreadyProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    @Transactional
    public void markProcessed(String eventId) {
        processedEventRepository.save(ProcessedEvent.of(eventId));
        log.debug("[Idempotency] 이벤트 처리 완료 기록 — eventId={}", eventId);
    }
}

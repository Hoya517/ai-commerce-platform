# Kafka 이벤트 설계 (ISSUE-31)

> Spring ApplicationEvent → Kafka 전환 설계 문서

---

## 전환 조건 (트리거)

아래 조건이 충족될 때 Kafka 전환:
- PostgreSQL 전환 완료 (실 DB 환경)
- Settlement 또는 알림 서비스가 별도 프로세스로 분리될 때
- 메시지 유실 없는 재처리가 실제로 필요해질 때

---

## 이벤트 목록

| 이벤트 | 발행 주체 | 토픽 | 파티션 키 |
|--------|----------|------|---------|
| OrderCreated | Order Service | `order-events` | `orderId` |
| PaymentConfirmed | Payment Service | `payment-events` | `paymentId` |
| PaymentCanceled | Payment Service | `payment-events` | `paymentId` |

---

## Phase 1: 아키텍처 설계

### Event Bridge 구조

```
Domain Service
      │
      ▼ Spring ApplicationEvent (현재)
Spring Event Publisher
      │
      ▼ (전환 후)
Kafka Producer (KafkaTemplate)
      │
      ▼
Kafka Topic (order-events / payment-events)
      │
      ▼
Kafka Consumer (@KafkaListener)
      │
      ▼
Settlement / Notification Service
```

### 토픽 설계

```
order-events
  - Partitions: 3
  - Replication Factor: 2 (운영), 1 (개발)
  - Retention: 7일
  - 파티션 키: orderId (동일 주문 순서 보장)

payment-events
  - Partitions: 3
  - Replication Factor: 2 (운영), 1 (개발)
  - Retention: 7일
  - 파티션 키: paymentId
```

### 메시지 스키마

```java
public record OrderCreatedEventMessage(
    Long orderId,
    Long memberId,
    BigDecimal amount,
    LocalDateTime createdAt
) {}

public record PaymentConfirmedEventMessage(
    Long paymentId,
    Long orderId,
    Long sellerId,
    BigDecimal amount,
    LocalDateTime approvedAt
) {}

public record PaymentCanceledEventMessage(
    Long paymentId,
    Long orderId,
    Long sellerId,
    BigDecimal amount,
    LocalDateTime canceledAt
) {}
```

**직렬화**: Jackson JsonSerializer
**Schema Evolution**: Backward Compatible (필드 추가만 허용, 삭제 금지)

### Producer 설정

```yaml
spring:
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all          # 모든 replica 확인 (유실 방지)
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 1
```

---

## Phase 2: POC 구현 계획

### Docker Compose (KRaft 모드, Zookeeper 없음)

```yaml
services:
  kafka:
    image: apache/kafka:3.8.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

### KafkaProducer 구현 예시

```java
@Component
@RequiredArgsConstructor
public class PaymentEventKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishConfirmed(PaymentConfirmedEventMessage message) {
        kafkaTemplate.send("payment-events",
                String.valueOf(message.paymentId()), message);
    }
}
```

### KafkaConsumer 구현 예시

```java
@Component
@RequiredArgsConstructor
public class SettlementKafkaConsumer {

    private final SettlementService settlementService;

    @KafkaListener(topics = "payment-events", groupId = "settlement-group")
    public void consume(PaymentConfirmedEventMessage message,
                        Acknowledgment ack) {
        settlementService.accumulate(message.sellerId(),
                Money.of(message.amount()));
        ack.acknowledge(); // manual commit
    }
}
```

---

## Phase 3: 신뢰성 / 장애 대응

### Transactional Outbox 패턴

```
도메인 트랜잭션
  ├─ Payment.approve() + DB 저장
  └─ OutboxEvent 저장 (동일 트랜잭션)

별도 Scheduler (Polling)
  └─ OutboxEvent 조회 → Kafka 발행 → OutboxEvent.markPublished()
```

**목적**: DB 저장 성공 + Kafka 발행 실패 케이스 대응

### DLQ 구조

```
payment-events
  └─ (실패 시) → payment-events-retry
        └─ (재시도 실패) → payment-events-dlq
```

### Offset 관리

- **방식**: manual commit
- **이유**: at-least-once delivery, 중복 처리 방지는 Idempotency로 해결
- **Idempotency**: `processed_event` 테이블로 중복 이벤트 무시

---

## 현재 Spring Events vs Kafka 비교

| 항목 | Spring Events | Kafka |
|------|--------------|-------|
| 메시지 유실 | 앱 재시작 시 유실 | 디스크 저장, 재처리 가능 |
| 분산 처리 | 단일 프로세스 | 멀티 인스턴스 소비 |
| 구현 복잡도 | 낮음 | 높음 |
| 운영 비용 | 없음 | Kafka 클러스터 필요 |
| 순서 보장 | 동기 처리 시 O | 파티션 내 보장 |

# 이벤트 기반 아키텍처 설계 문서

> 기준일: 2026-03-13 (ISSUE-14~15 반영)
> 발표 슬라이드 1장으로 설명 가능한 수준으로 작성.

---

## 1. 왜 이벤트를 도입했는가

### 도입 전 — 직접 호출의 문제

```
PaymentService
  ├── orderRepository.findById()
  ├── pgGateway.confirm()
  ├── walletService.deduct()        ← 예치금 결제 시
  ├── productRepository.findByIdWithLock()  ← 취소 시 재고 복구
  └── settlementService.prepare()   ← 정산까지 추가되면?
```

`PaymentService`가 점점 비대해지고, 새로운 요구사항마다 결제 서비스 코드를 수정해야 함.

### 도입 후 — 이벤트로 분리

```
PaymentService
  ├── pgGateway.confirm()           ← 결제 핵심 로직만
  └── publishEvent(PaymentConfirmedEvent)   ← "결제됐다" 선언만

SettlementEventListener            ← PaymentService 모름
  └── onPaymentConfirmed()         ← 알아서 반응
```

**결제 서비스는 정산/알림/통계가 존재하는지 알 필요 없음.**

---

## 2. 구현된 이벤트 목록

| 이벤트 | 발행 위치 | 포함 데이터 |
|--------|----------|------------|
| `OrderCreatedEvent` | `OrderService.createOrder()` / `createOrderFromCart()` | orderId, memberId, totalAmount, occurredAt |
| `PaymentConfirmedEvent` | `PaymentService.confirmPayment()` / `payWithWallet()` | paymentId, orderId, memberId, amount, method, occurredAt |
| `PaymentCanceledEvent` | `PaymentService.cancelPayment()` | paymentId, orderId, memberId, amount, method, occurredAt |

**패키지:** `common/event/` — Kafka 전환 시에도 재사용 가능한 위치

---

## 3. 동기 vs 비동기 경계

### 동기 처리 (같은 트랜잭션 내)

원자성이 필요한 작업 — 하나라도 실패하면 전체 롤백.

| 작업 | 이유 |
|------|------|
| 재고 차감 (`product.decreaseStock`) | 주문과 동시에 처리되어야 함 |
| 지갑 차감 (`walletService.deduct`) | 결제와 원자적으로 처리 |
| 지갑 환불 (`walletService.charge`) | 취소와 원자적으로 처리 |
| 재고 복구 (`product.increaseStock`) | 주문 취소와 원자적으로 처리 |
| 주문 상태 변경 (`order.markPaid`) | 결제와 동일 트랜잭션 |

### 비동기 처리 (이벤트 구독, AFTER_COMMIT)

실패해도 결제/주문에 영향 없는 작업 — 별도 스레드에서 처리.

| 이벤트 | 구독자 | 처리 내용 |
|--------|--------|----------|
| `PaymentConfirmedEvent` | `SettlementEventListener` | 정산 준비 예약 (stub) |
| `PaymentCanceledEvent` | `SettlementEventListener` | 정산 취소 예약 (stub) |
| `OrderCreatedEvent` | (미구현) | 알림, 통계 등 추후 연결 |

---

## 4. `@TransactionalEventListener` 사용 이유

```
결제 트랜잭션 시작
  └── payment.approve()
  └── order.markPaid()
  └── eventPublisher.publishEvent(PaymentConfirmedEvent)
결제 트랜잭션 커밋 ──────────────────────────────────────┐
                                                          ▼
                              SettlementEventListener.onPaymentConfirmed()
                              (별도 스레드 — eventTaskExecutor)
```

**`@EventListener` (기본)** 대신 **`@TransactionalEventListener(AFTER_COMMIT)`** 를 사용하는 이유:

| 상황 | `@EventListener` | `@TransactionalEventListener(AFTER_COMMIT)` |
|------|-----------------|---------------------------------------------|
| 결제 트랜잭션 정상 커밋 | 리스너 실행 ✅ | 리스너 실행 ✅ |
| 결제 트랜잭션 롤백 | 리스너 **이미 실행됨** ❌ | 리스너 **실행 안 됨** ✅ |

롤백된 결제에 대해 정산이 시작되는 상황을 원천 차단.

---

## 5. 실패 시나리오와 현재 전략

### 현재 (Spring Event 단계)

```
결제 커밋 완료 → PaymentConfirmedEvent 발행 → SettlementEventListener 처리 실패
                                                          ↓
                                              예외 로그만 남고 이벤트 소실
```

| 실패 단계 | 결과 | 현재 대응 |
|----------|------|----------|
| 결제 트랜잭션 실패 | 이벤트 미발행 (AFTER_COMMIT 덕분) | 자동 처리 ✅ |
| 리스너 처리 실패 | 이벤트 소실 | 로그 기록 → 수동 재처리 ⚠️ |

### 이후 (Kafka 전환 시, ISSUE-31)

```
결제 커밋 + Outbox 저장 (같은 트랜잭션)
  ↓
Outbox Polling → Kafka 발행
  ↓
Consumer 처리 실패 → offset 미커밋 → 자동 재시도 → DLT
```

재처리/내구성이 자동으로 보장됨.

---

## 6. Kafka 전환 시 변경 범위

Spring Event → Kafka로 전환할 때 **이벤트 클래스(`common/event/`)는 변경 없음**.

| 레이어 | Spring Event | Kafka |
|--------|-------------|-------|
| 이벤트 클래스 | `OrderCreatedEvent` 등 record | **동일** |
| 발행 | `ApplicationEventPublisher.publishEvent()` | `KafkaTemplate.send()` |
| 구독 | `@TransactionalEventListener` | `@KafkaListener` |
| 트랜잭션 안전성 | AFTER_COMMIT | Outbox 패턴 |
| 재처리 | 없음 (수동) | 자동 (offset + DLT) |

발행자와 구독자만 교체, **비즈니스 로직은 그대로.**

---

## 7. 전체 이벤트 흐름 다이어그램

```
[주문 생성]
CartService → OrderService ──publishEvent──▶ OrderCreatedEvent
                                                    │
                                          (리스너 미구현 — 추후 알림)

[결제 승인]
PaymentController → PaymentService ──publishEvent──▶ PaymentConfirmedEvent
                                                             │
                                               ┌────────────▼────────────┐
                                               │  SettlementEventListener │
                                               │  onPaymentConfirmed()    │
                                               │  [stub: log only]        │
                                               └─────────────────────────┘

[결제 취소]
PaymentController → PaymentService ──publishEvent──▶ PaymentCanceledEvent
                                                             │
                                               ┌────────────▼────────────┐
                                               │  SettlementEventListener │
                                               │  onPaymentCanceled()     │
                                               │  [stub: log only]        │
                                               └─────────────────────────┘
```

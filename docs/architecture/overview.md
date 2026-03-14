# 아키텍처 개요

> AI Commerce Platform — 현재 모놀리식 → 향후 MSA 전환 전략

---

## 현재 구조: 모놀리식 레이어드 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                     AI Commerce Platform                         │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                   Presentation Layer                       │   │
│  │  MemberController  SellerController  ProductController    │   │
│  │  CartController    OrderController   PaymentController    │   │
│  │  WalletController  SettlementController                   │   │
│  └────────────────────────┬──────────────────────────────────┘   │
│                           │ @RequiresAuth / JWT Filter            │
│  ┌────────────────────────▼──────────────────────────────────┐   │
│  │                   Application Layer                        │   │
│  │  MemberService   SellerService    ProductService          │   │
│  │  CartService     OrderService     PaymentService          │   │
│  │  WalletService   SettlementService                        │   │
│  └────────────────────────┬──────────────────────────────────┘   │
│                           │ Domain Events (Spring Events)         │
│  ┌────────────────────────▼──────────────────────────────────┐   │
│  │                    Domain Layer                            │   │
│  │  Member  Seller  Product  Cart  Order  Payment  Wallet    │   │
│  │  Settlement                                               │   │
│  └────────────────────────┬──────────────────────────────────┘   │
│                           │                                       │
│  ┌────────────────────────▼──────────────────────────────────┐   │
│  │               Infrastructure (H2 / JPA)                   │   │
│  └───────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Bounded Context 분리 기준

| Context | 핵심 책임 | 집계 루트 |
|---------|----------|---------|
| **Member** | 회원 가입·인증·JWT 발급 | `Member` |
| **Seller** | 판매자 등록·승인·검증 | `Seller` |
| **Catalog** | 상품 등록·상태·재고 | `Product` |
| **Cart** | 장바구니 관리 | `Cart` |
| **Order** | 주문 생성·상태 흐름 | `Order` |
| **Payment** | 결제 요청·승인·취소 | `Payment` |
| **Wallet** | 예치금 관리 | `Wallet` |
| **Settlement** | 정산 누적·배치 처리 | `Settlement` |

크로스-집계 참조 원칙: **ID 참조만 허용** (직접 엔티티 참조 금지)

---

## 이벤트 흐름

```
PaymentService.confirmPayment()
        │
        ▼
  PaymentConfirmedEvent (sellerId, amount)
        │
        ▼ @TransactionalEventListener (AFTER_COMMIT)
  SettlementEventListener.onPaymentConfirmed()
        │
        ▼
  SettlementService.accumulate(sellerId, amount)
        │
        ▼
  Settlement (당월 PENDING) += amount * (1 - FeePolicy.STANDARD_RATE)


PaymentService.cancelPayment()
        │
        ▼
  PaymentCanceledEvent (sellerId, amount)
        │
        ▼
  SettlementEventListener.onPaymentCanceled()
        │
        ▼
  SettlementService.deductPayment(sellerId, amount)
```

---

## 주요 API 호출 흐름

### 주문 → 결제 흐름

```
클라이언트
  │
  ├─ POST /orders          (JWT 필요)
  │     └─ OrderService.createOrder()
  │           ├─ Product.decreaseStock()   ← 비관적 락 (SELECT FOR UPDATE)
  │           └─ Order (CREATED) 저장
  │
  ├─ POST /payments        (결제 요청)
  │     └─ PaymentService.requestPayment()
  │           └─ Payment (READY) 저장
  │
  └─ POST /payments/confirm (결제 승인)
        └─ PaymentService.confirmPayment()
              ├─ Payment (APPROVED)
              ├─ Order (PAID)
              └─ PaymentConfirmedEvent → SettlementService.accumulate()
```

### 예치금 결제 흐름

```
POST /payments/wallet  (JWT 필요)
  └─ PaymentService.payWithWallet()
        ├─ Order.startPayment()
        ├─ WalletService.deduct()  ← 잔액 부족 시 WalletException
        ├─ Payment.request(null) → Payment.approve()
        └─ Order.markPaid()
```

### 정산 배치 흐름

```
@Scheduled cron="0 0 2 1 * *"  (매월 1일 02:00)
  또는 POST /settlements/batch
        └─ Spring Batch Job: settlementJob
              └─ Step: completeSettlementsStep (chunk=50)
                    ├─ Reader: PENDING이고 periodEnd <= targetDate 인 Settlement 목록
                    ├─ Processor: Settlement.complete()
                    └─ Writer: 저장
```

---

## 향후 MSA 분리 구조 (목표)

```
                    ┌─────────────────┐
                    │   API Gateway   │  ← NGINX / Spring Cloud Gateway
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Member /    │    │   Catalog /  │    │   Order /    │
│  Auth Svc    │    │   Cart Svc   │    │   Payment    │
└──────────────┘    └──────────────┘    └──────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  Message Broker  │  ← Kafka
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Settlement Svc  │
                    └─────────────────┘
```

각 서비스는 독립 DB 보유 (Database-per-Service 패턴)

---

## 기술 스택

| 구분 | 현재 | 향후 |
|------|------|------|
| 프레임워크 | Spring Boot 4.0.3 | 유지 |
| DB | H2 (개발), PostgreSQL (운영 예정) | 서비스별 분리 |
| 메시지 | Spring ApplicationEvent | Kafka |
| 인증 | JWT (직접 구현) | API Gateway OAuth2 |
| 배치 | Spring Batch 6 | 유지 |
| 검색 | JPA 조회 | Elasticsearch |
| 컨테이너 | Docker | Kubernetes |

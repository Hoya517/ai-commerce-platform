# Enum States Reference

> 기준일: 2026-03-13 (ISSUE-11 반영)
> 프로젝트 전체 enum 상태값, 전이 규칙, 초기값을 한 곳에 정리한 레퍼런스 문서.

---

## 1. ProductStatus

**위치:** `catalog/domain/ProductStatus.java`

| 값 | 설명 | 초기값 |
|----|------|--------|
| `ON_SALE` | 판매 중 | ✅ `Product.create()` 시 기본값 |
| `OUT_OF_STOCK` | 품절 | 재고(`stockQuantity`)가 0이 되면 자동 전환 |
| `HIDDEN` | 숨김 | 판매자가 명시적으로 숨긴 상태 |
| `DELETED` | 삭제됨 | 소프트 삭제 상태 |

### 상태 전이

```
         재고 0
ON_SALE ──────────→ OUT_OF_STOCK
   ↑                      │
   │ increaseStock()       │ increaseStock()
   └──────────────────────┘

ON_SALE / OUT_OF_STOCK ──→ HIDDEN
ON_SALE / OUT_OF_STOCK ──→ DELETED
```

### 규칙
- `ON_SALE` 상태가 아닌 상품은 장바구니에 담거나 주문할 수 없다.
- 재고 차감(`decreaseStock`) 후 재고가 0이 되면 자동으로 `OUT_OF_STOCK`으로 전환.
- 재고 복구(`increaseStock`) 시 `OUT_OF_STOCK`이면 `ON_SALE`로 자동 복귀.

---

## 2. OrderStatus

**위치:** `order/domain/OrderStatus.java`

| 값 | 설명 | 초기값 |
|----|------|--------|
| `CREATED` | 주문 생성 완료, 결제 대기 | ✅ `Order.create()` 시 기본값 |
| `PAYMENT_PENDING` | 결제 진행 중 | `order.startPayment()` 호출 시 |
| `PAID` | 결제 완료 | `order.markPaid()` 호출 시 |
| `CANCELED` | 주문 취소 | `order.cancel()` 호출 시 |
| `FAILED` | Reserved (현재 미사용) | — |

### 상태 전이

```
            startPayment()              markPaid()
CREATED ──────────────────→ PAYMENT_PENDING ──────────→ PAID
   │                               │
   │ cancel()                      │ cancel()
   ↓                               ↓
CANCELED                       CANCELED
```

### 규칙
- `startPayment()`: CREATED에서만 호출 가능. PAYMENT_PENDING 중복 호출 불가.
- `markPaid()`: PAYMENT_PENDING에서만 호출 가능. 다른 상태에서 호출 시 `OrderException`.
- `cancel()`: PAID 상태에서 호출 불가 (`OrderException`). PAID 주문 취소는 `refund()` 사용.
- `refund()`: PAID 상태에서만 호출 가능 (결제 취소/환불 전용).
- 주문 취소/환불 시 각 OrderItem의 상품 재고를 복구(`increaseStock`).

---

## 3. PaymentStatus

**위치:** `payment/domain/PaymentStatus.java`

| 값 | 설명 | 초기값 |
|----|------|--------|
| `READY` | 결제 객체 생성됨 | ✅ `Payment.create()` 시 기본값 |
| `REQUESTED` | 외부 PG에 결제 키 발급 요청됨 | `payment.request(paymentKey)` 호출 시 |
| `APPROVED` | 결제 승인 완료 | `payment.approve()` 호출 시 |
| `FAILED` | 결제 실패 | `payment.fail(code, message)` 호출 시 |
| `CANCELED` | 결제 취소 | `payment.cancel()` 호출 시 |

### 상태 전이 — PG 결제 (CARD / EASY_PAY / VIRTUAL_ACCOUNT)

```
          request()           approve()
READY ──────────→ REQUESTED ──────────→ APPROVED ──→ CANCELED
                      │                                (cancel)
                      │ fail()
                      ↓
                    FAILED
```

### 상태 전이 — 예치금 결제 (WALLET)

```
          request(null)       approve()
READY ──────────────→ REQUESTED ──────────→ APPROVED
```

> WALLET 결제도 동일한 상태 흐름을 따름. `paymentKey`는 `null`로 처리.

### 규칙
- `request()`: READY 상태에서만 호출 가능.
- `approve()`: REQUESTED 상태에서만 호출 가능.
- `fail()`: READY 또는 REQUESTED 상태에서만 호출 가능. APPROVED/CANCELED 이후 불가.
- `cancel()`: APPROVED 상태에서만 가능.
- `approvedAt`은 `approve()` 호출 시각으로 자동 기록.

---

## 4. PaymentMethod

**위치:** `payment/domain/PaymentMethod.java`

| 값 | 설명 | 결제 흐름 |
|----|------|-----------|
| `CARD` | 신용/체크카드 | PG 3단계 (READY → REQUESTED → APPROVED) |
| `EASY_PAY` | 간편결제 (카카오페이 등) | PG 3단계 |
| `VIRTUAL_ACCOUNT` | 가상계좌 | PG 3단계 |
| `WALLET` | 내부 예치금 | 동일 흐름 (READY → REQUESTED(null key) → APPROVED) |

---

## 5. SellerStatus

**위치:** `seller/domain/SellerStatus.java`

| 값 | 설명 | 초기값 |
|----|------|--------|
| `PENDING` | 판매자 등록 신청, 승인 대기 | ✅ `Seller.create()` 시 기본값 |
| `APPROVED` | 승인 완료, 상품 등록 가능 | `approveSeller()` 호출 시 |
| `SUSPENDED` | 정지됨, 상품 등록 불가 | (추후 구현) |

### 상태 전이

```
          approveSeller()            (미구현)
PENDING ──────────────→ APPROVED ──────────→ SUSPENDED
```

### 규칙
- 상품 등록 시 `SellerService.verifyApprovedSeller()`로 `APPROVED` 여부 검증.
- `PENDING` 또는 `SUSPENDED` 판매자는 상품 등록 불가 (`SellerException`).

---

## 6. MemberRole

**위치:** `member/domain/MemberRole.java`

| 값 | 설명 | 부여 시점 |
|----|------|-----------|
| `MEMBER` | 일반 회원 | ✅ 회원가입 시 기본값 |
| `SELLER` | 판매자 | `member.promoteToSeller()` 호출 시 |
| `ADMIN` | 관리자 | (수동 부여, API 미구현) |

### 규칙
- 판매자 등록(`POST /sellers`) 시 `member.promoteToSeller()`로 `SELLER`로 승격.
- 현재 Role 기반 접근 제어는 JWT 인증(유무)만 구현, SELLER/ADMIN 권한 분기는 미구현.

---

## 7. MemberStatus

**위치:** `member/domain/MemberStatus.java`

| 값 | 설명 | 초기값 |
|----|------|--------|
| `ACTIVE` | 정상 활성 상태 | ✅ 회원가입 시 기본값 |
| `INACTIVE` | 비활성 (탈퇴/정지) | (추후 구현) |

### 규칙
- 현재 `INACTIVE` 전이 로직은 미구현.

---

## 요약

| Enum | 초기값 | 상태 수 | 전이 로직 구현 |
|------|--------|---------|---------------|
| `ProductStatus` | `ON_SALE` | 4 | ✅ (재고 연동 자동 전환) |
| `OrderStatus` | `CREATED` | 5 | ✅ |
| `PaymentStatus` | `READY` | 5 | ✅ |
| `PaymentMethod` | — | 4 | ✅ |
| `SellerStatus` | `PENDING` | 3 | 일부 (SUSPENDED 미구현) |
| `MemberRole` | `MEMBER` | 3 | 일부 (ADMIN 부여 미구현) |
| `MemberStatus` | `ACTIVE` | 2 | 미구현 |

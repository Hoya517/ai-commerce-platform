# Domain Model

## 1. 프로젝트 개요

이 프로젝트는 Spring AI / MSA 학습 과정과 연계하여 진행하는 개인 사이드 프로젝트이다.  
초기에는 **모듈형 모놀리식**으로 시작하고, 이후 주문/결제/검색/AI 기능을 중심으로 점진적으로 분리한다.

1차 도메인 설계 범위는 다음과 같다.

- Catalog(상품)
- Cart(장바구니)
- Order(주문)
- Payment(결제)

현재 단계에서는 **핵심 거래 흐름의 안정적인 설계**를 목표로 한다.

---

## 2. 핵심 사용자 흐름

1. 사용자가 상품을 조회한다.
2. 상품을 장바구니에 담는다.
3. 장바구니를 기준으로 주문을 생성한다.
4. 주문을 대상으로 결제를 요청한다.
5. 결제 승인 결과에 따라 주문 상태가 변경된다.

---

## 3. 바운디드 컨텍스트

### 3.1 Catalog
책임:
- 상품 정보 관리
- 상품 가격 관리
- 판매 상태 관리
- 재고 수량 관리

핵심 개념:
- Product

---

### 3.2 Cart
책임:
- 장바구니 생성
- 장바구니 항목 추가/수정/삭제
- 주문 전 임시 선택 상태 관리

핵심 개념:
- Cart
- CartItem

---

### 3.3 Order
책임:
- 주문 생성
- 주문 항목 스냅샷 저장
- 주문 총액 계산
- 주문 상태 관리

핵심 개념:
- Order
- OrderItem

---

### 3.4 Payment
책임:
- 결제 요청
- 결제 승인/실패 상태 관리
- PG 연동 결과 저장
- 결제 이력 관리

핵심 개념:
- Payment

---

## 4. Aggregate 설계

## 4.1 Catalog Aggregate

### Aggregate Root
- Product

### 설명
상품은 판매의 기준이 되는 도메인 객체이다.  
초기 단계에서는 상품 정보와 재고 수량을 같은 Aggregate에서 관리한다.

### Product 속성
- id
- name
- description
- price
- status
- stockQuantity
- createdAt
- updatedAt

### Product 상태
- ON_SALE
- OUT_OF_STOCK
- HIDDEN
- DELETED

### 규칙
- 상품 가격은 0보다 커야 한다.
- 판매중 상태가 아닌 상품은 장바구니에 담을 수 없다.
- 재고 수량이 0 이하인 상품은 주문할 수 없다.

---

## 4.2 Cart Aggregate

### Aggregate Root
- Cart

### 내부 구성
- Cart
- CartItem

### 설명
장바구니는 아직 거래가 확정되지 않은 사용자의 선택 상태를 관리한다.  
CartItem은 Cart 없이 의미가 없으므로 Cart Aggregate 내부 엔티티로 둔다.

### Cart 속성
- id
- memberId
- createdAt
- updatedAt

### CartItem 속성
- id
- productId
- productNameSnapshot
- unitPriceSnapshot
- quantity

### 규칙
- 장바구니 항목 수량은 1 이상이어야 한다.
- 같은 상품을 다시 담으면 신규 행을 추가하지 않고 수량을 증가시킨다.
- 판매중이 아닌 상품은 담을 수 없다.
- 재고보다 많은 수량은 담을 수 없다.

### 비고
장바구니에는 상품명/가격 스냅샷을 저장할 수 있지만,  
최종 가격/재고 확정은 주문 생성 시점에 다시 검증한다.

---

## 4.3 Order Aggregate

### Aggregate Root
- Order

### 내부 구성
- Order
- OrderItem

### 설명
주문은 거래의 기준점이며, 결제는 반드시 주문을 대상으로 수행한다.  
주문 시점의 상품 정보를 스냅샷으로 저장하여 이후 상품 정보 변경과 무관하게 주문 이력을 보존한다.

### Order 속성
- id
- memberId
- status
- totalAmount
- orderedAt

### OrderItem 속성
- id
- orderId
- productId
- productName
- orderPrice
- quantity

### Order 상태
- CREATED
- PAYMENT_PENDING
- PAID
- CANCELED
- FAILED

### 규칙
- 주문 항목은 1개 이상이어야 한다.
- 주문 총액은 주문 항목 금액 합계와 일치해야 한다.
- 취소된 주문은 결제를 진행할 수 없다.
- 이미 결제 완료된 주문은 다시 결제할 수 없다.

---

## 4.4 Payment Aggregate

### Aggregate Root
- Payment

### 설명
결제는 외부 PG와의 연동 결과를 저장하고, 결제 승인/실패/취소 상태를 관리한다.  
주문과는 분리된 Aggregate로 유지하고 `orderId`로만 참조한다.

### Payment 속성
- id
- orderId
- paymentKey
- amount
- status
- method
- requestedAt
- approvedAt
- failureCode
- failureMessage

### Payment 상태
- READY
- REQUESTED
- APPROVED
- FAILED
- CANCELED

### PaymentMethod 예시
- CARD
- EASY_PAY
- VIRTUAL_ACCOUNT

### 규칙
- 결제 금액은 주문 총액과 일치해야 한다.
- 승인 완료된 결제는 다시 승인할 수 없다.
- 하나의 주문에 대해 활성 결제는 1개만 허용한다.
- 결제 실패 시 실패 이력을 남긴다.

### 비고
추후 PG 연동 시 원본 응답(rawResponse) 저장 여부를 검토한다.

---

## 5. Aggregate 간 참조 원칙

Aggregate 간에는 직접 객체 참조를 최소화하고 **식별자(ID) 참조**를 사용한다.

예:
- CartItem → Product 객체 직접 참조 금지, `productId` 사용
- OrderItem → Product 객체 직접 참조 금지, `productId` 사용
- Payment → Order 객체 직접 참조 금지, `orderId` 사용

### 이유
- Aggregate 경계가 명확해진다.
- 트랜잭션 범위를 과도하게 키우지 않는다.
- 이후 MSA 분리 시 유리하다.

---

## 6. Value Object

## 6.1 Money
금액 계산과 비교를 위한 공통 VO

### 목적
- 금액 연산 책임 집중
- 음수 금액 방지
- 합산/비교 로직 일관성 유지

### 책임 예시
- add()
- multiply()
- isNegative()
- isGreaterThan()

---

## 6.2 Quantity
수량 검증과 연산을 위한 공통 VO

### 목적
- 0 이하 수량 방지
- 증가/감소 규칙 관리

### 책임 예시
- increase()
- decrease()
- isZeroOrNegative()

---

## 7. 도메인 규칙 요약

### Catalog
- 가격은 0보다 커야 한다.
- 판매중 상품만 장바구니에 담을 수 있다.
- 재고가 없는 상품은 주문할 수 없다.

### Cart
- 수량은 1 이상이다.
- 동일 상품은 수량 증가로 처리한다.
- 주문 전 상태만 관리한다.

### Order
- 주문 항목은 최소 1개 이상이어야 한다.
- 주문 총액은 항목 합계와 일치해야 한다.
- 주문은 결제의 기준이 된다.

### Payment
- 결제 금액은 주문 총액과 같아야 한다.
- 승인 완료 결제는 중복 승인할 수 없다.
- 결제 실패 내역은 저장해야 한다.

---

## 8. 도메인 이벤트 초안

초기에는 내부 이벤트 또는 서비스 후처리 수준으로 시작한다.  
이후 Kafka 기반 이벤트로 확장할 수 있다.

### 이벤트 후보
- CartItemAddedEvent
- OrderCreatedEvent
- PaymentRequestedEvent
- PaymentApprovedEvent
- PaymentFailedEvent

---

## 9. 트랜잭션 경계

### 주문 생성
하나의 트랜잭션 안에서 처리한다.
- 장바구니 조회
- 상품 가격/재고 검증
- 주문 생성
- 주문 항목 생성

### 결제 요청
하나의 트랜잭션 안에서 처리한다.
- 주문 조회
- 결제 가능 상태 검증
- Payment 생성
- 결제 요청 상태 저장

### 결제 승인 반영
하나의 트랜잭션 안에서 처리한다.
- Payment 승인 상태 변경
- Order 상태 변경

---

## 10. 초기 ERD 초안

### product
- id
- name
- description
- price
- status
- stock_quantity
- created_at
- updated_at

### cart
- id
- member_id
- created_at
- updated_at

### cart_item
- id
- cart_id
- product_id
- product_name_snapshot
- unit_price_snapshot
- quantity

### orders
- id
- member_id
- status
- total_amount
- ordered_at

### order_item
- id
- order_id
- product_id
- product_name
- order_price
- quantity

### payment
- id
- order_id
- payment_key
- amount
- status
- method
- requested_at
- approved_at
- failure_code
- failure_message

---

## 11. 1차 범위에서 의도적으로 제외한 것

초기 복잡도를 낮추기 위해 아래 항목은 1차 범위에서 제외한다.

- 회원 도메인 상세 설계
- 상품 옵션
- 할인/쿠폰
- 포인트
- 배송지 상세 모델
- 정산
- 검색
- AI 추천/RAG
- MSA 분리

현재는 `memberId`만 유지하고, 인증/회원은 후속 단계에서 붙인다.

---

## 12. 향후 확장 방향

### 다음 단계
- 결제 승인 후 재고 차감
- 정산 도메인 추가
- Elasticsearch 기반 상품 검색
- Kafka 기반 비동기 이벤트 처리
- Spring AI 기반 상품 QA / 리뷰 요약
- API Gateway 및 서비스 분리

---

## 13. 현재 설계 원칙 요약

- 먼저 모듈형 모놀리식으로 시작한다.
- Aggregate 경계를 명확히 유지한다.
- Aggregate 간 참조는 ID 기반으로 한다.
- 주문 시점의 상품 정보는 스냅샷으로 저장한다.
- 결제는 주문을 기준으로 생성한다.
- 재고 차감은 결제 승인 이후 처리한다.
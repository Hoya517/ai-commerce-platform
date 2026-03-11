application-service-guideline.md
# Application Service Guideline

## 1. 목적

이 문서는 ai-commerce-platform 프로젝트의 **application service 계층 설계 원칙**을 정의한다.

이 프로젝트는 초기에는 **모듈형 모놀리식 + 레이어드 아키텍처**로 빠르게 개발하고,
이후 Batch, Event Driven Architecture, MSA 전환을 고려하여 확장한다.

Application Service는 비즈니스 로직을 모두 담는 계층이 아니라  
**유스케이스 실행과 흐름 제어를 담당하는 계층**이다.

---

# 2. 계층 구조

프로젝트 기본 구조


presentation
application
domain
infrastructure


## presentation

역할

- HTTP 요청 처리
- Request/Response DTO 처리
- validation
- application service 호출

예


OrderController
CartController
ProductController


---

## application

역할

- 유스케이스 실행
- 트랜잭션 관리
- 여러 domain 객체 orchestration
- repository 호출
- domain event 발행

예


OrderService
CartService
ProductService
PaymentService


---

## domain

역할

- 핵심 비즈니스 규칙
- 상태 변경 로직
- entity / value object
- aggregate
- repository interface

예


Order
OrderItem
Product
Cart
Payment


---

## infrastructure

역할

- JPA repository 구현
- 외부 API 연동
- Kafka / Redis / Batch / Search

예


JpaOrderRepository
JpaProductRepository
TossPaymentClient
KafkaEventPublisher


---

# 3. Application Service 책임

Application Service는 아래 책임만 가진다.

### 유스케이스 실행

예

- 상품 생성
- 장바구니 상품 추가
- 주문 생성
- 결제 요청
- 결제 승인 반영

---

### 트랜잭션 관리

상태 변경


@Transactional


조회


@Transactional(readOnly = true)


---

### 도메인 객체 협력

Application Service는

- repository 조회
- domain 객체 조합
- domain 행위 호출

만 수행한다.

---

### 외부 연동 orchestration

예

- PG 결제 요청
- 외부 API 호출
- ACL adapter 사용

---

### 이벤트 발행

예


OrderCreatedEvent
PaymentRequestedEvent
PaymentApprovedEvent
PaymentFailedEvent


---

# 4. Application Service가 하면 안 되는 것

## 핵심 비즈니스 규칙 구현

❌ 나쁜 예


product.setStock(product.getStock() - quantity)
order.setStatus(PAID)
payment.setStatus(APPROVED)


⭕ 좋은 예


product.decreaseStock(quantity)
order.markPaid()
payment.approve()


---

## Controller DTO 직접 사용

변환 규칙


Request DTO → Command
Service 실행
Domain → Result
Result → Response DTO


---

## JPA 편의 로직 중심 설계

Fetch 전략이나 Lazy 문제 때문에
도메인 구조를 바꾸지 않는다.

---

## 과도한 책임 집중

예

createOrder()

한 메서드에서

- 주문 생성
- 결제 요청
- 재고 차감
- 정산 생성
- 알림 발송

까지 처리하지 않는다.

현재 단계에서는 **주문 생성까지만 담당**한다.

---

# 5. 설계 원칙

## 서비스는 인터페이스 없이 클래스

현재 프로젝트에서는


ProductService
CartService
OrderService
PaymentService


처럼 **구현 클래스만 사용한다.**

이유

- 구현체 대부분 1개
- 코드 단순화
- 불필요한 파일 감소

---

## 서비스 메서드는 유스케이스 중심

좋은 예


createOrder
cancelOrder
addItem
updateItemQuantity
requestPayment
confirmPayment


나쁜 예


saveOrder
updateOrder
processCart


---

## 서비스 메서드 하나 = 유스케이스 하나

예

- 장바구니 상품 추가
- 장바구니 수량 변경
- 주문 생성
- 결제 승인 반영

---

## Aggregate 간 직접 참조 최소화

가능하면


productId
orderId
cartId


처럼 **ID 기반 참조** 사용

---

## Domain 규칙은 Domain에 둔다

예

Domain 책임

- 주문 금액 계산
- 재고 감소
- 상태 변경

Service 책임

- 흐름 제어
- 트랜잭션

---

# 6. DTO 규칙

## Presentation DTO

HTTP 계층 전용

예


CreateOrderRequest
AddCartItemRequest
ConfirmPaymentRequest


---

## Application DTO

유스케이스 입출력

예


CreateOrderCommand
CreateOrderResult
AddCartItemCommand
PaymentResult


---

## DTO 변환 흐름


Request DTO
↓
Command
↓
Service
↓
Result
↓
Response DTO


---

# 7. 트랜잭션 규칙

상태 변경


@Transactional


조회


@Transactional(readOnly = true)


---

# 8. 서비스 책임 예시

## ProductService

- 상품 생성
- 상품 조회
- 상품 상태 변경

---

## CartService

- 장바구니 조회
- 장바구니 상품 추가
- 수량 변경
- 항목 삭제

---

## OrderService

- 주문 생성
- 주문 조회
- 주문 취소

---

## PaymentService

- 결제 요청
- 결제 승인
- 결제 실패 처리

---

# 9. 핵심 원칙 요약

- application service = **유스케이스 실행**
- domain = **비즈니스 규칙**
- service = **orchestration + transaction**
- DTO = **Command / Result 분리**
- 서비스 인터페이스는 **현재 단계에서 사용하지 않음**
- 점진적으로 **Batch / Event / MSA 확장 가능 구조 유지**
# Current State — AI Commerce Platform

> 기준일: 2026-03-13

## 개요

Spring Boot 4.0.3 기반의 MSA 학습용 커머스 백엔드. 현재는 단일 모놀리식 애플리케이션으로, 4개의 Bounded Context를 레이어드 아키텍처로 구현한 상태.

---

## 도메인 구성

| Bounded Context | 핵심 엔티티 | 책임 |
|---|---|---|
| **Catalog** | `Product` | 상품 등록·조회·상태 관리, 재고 차감 |
| **Cart** | `Cart`, `CartItem` | 회원별 장바구니 관리 (추가·수량변경·삭제) |
| **Order** | `Order`, `OrderItem` | 주문 생성·조회·취소, 결제 완료 상태 반영 |
| **Payment** | `Payment` | 결제 요청·승인·실패 처리 |
| **Common** | `Money`, `Quantity` | 공통 값 객체 (금액, 수량) |

---

## API 목록

### Product — `/products`

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/products` | 상품 등록 |
| `GET` | `/products/{productId}` | 상품 단건 조회 |
| `PATCH` | `/products/{productId}/status` | 상품 상태 변경 (ACTIVE / INACTIVE / SOLD_OUT) |

### Cart — `/cart`

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/cart?memberId=` | 장바구니 조회 |
| `POST` | `/cart/items` | 상품 추가 (장바구니 없으면 자동 생성) |
| `PATCH` | `/cart/items/{productId}` | 상품 수량 변경 |
| `DELETE` | `/cart/items/{productId}?memberId=` | 상품 삭제 |

### Order — `/orders`

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/orders` | 주문 생성 (재고 차감 포함) |
| `GET` | `/orders/{orderId}` | 주문 단건 조회 |
| `POST` | `/orders/{orderId}/cancel` | 주문 취소 |

### Payment — `/payments`

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/payments` | 결제 요청 |
| `POST` | `/payments/confirm` | 결제 승인 (→ 주문 PAID 상태로 변경) |
| `POST` | `/payments/fail` | 결제 실패 처리 |

---

## 레이어 구조

```
Presentation  →  Application  →  Domain  →  Infrastructure(Repository)
(Controller)     (Service)       (Entity/VO)   (Spring Data JPA)
```

- **Presentation**: `@RestController`, `ApiResponse<T>` 공통 응답, `GlobalExceptionHandler`
- **Application**: `@Service`, Command/Result DTO (Java records), `@Transactional`
- **Domain**: `@Entity` Aggregate Root, 값 객체(`Money`, `Quantity`), 도메인 예외
- **Repository**: Spring Data JPA 인터페이스

---

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Build | Gradle 9.3.1 |
| Persistence | Spring Data JPA (Hibernate) |
| Test DB | H2 in-memory |
| API Docs | SpringDoc OpenAPI (Swagger UI: `/swagger-ui.html`) |
| Utility | Lombok |
| Test | JUnit 5, Mockito, AssertJ |

**Planned:**
- Redis
- PostgreSQL
- Spring Event
- Kafka (optional)

---

## 현재 상태 요약

### 구현된 기능

- 4개 Bounded Context 전체 Domain · Application · Presentation 레이어 구현
- 도메인 단위 테스트, 서비스 단위 테스트, 컨트롤러 슬라이스 테스트 모두 통과
- 공통 응답 포맷(`ApiResponse<T>`) 및 전역 예외 처리
- Swagger UI를 통한 API 문서 자동 생성

### 아직 구현되지 않은 기능

- Member 도메인 (회원 가입·로그인)
- JWT 인증 (현재 `memberId`를 쿼리 파라미터로 직접 전달)
- Seller 도메인
- Wallet (예치금)
- Settlement (정산)
- 결제 환불
- 이벤트 기반 처리
- GitHub Actions CI
- 실 DB 연동 (현재 H2 in-memory 사용)

### 다음 단계 예정 작업

Member / Auth 구현 → Wallet / Seller → Settlement → 이벤트 기반 아키텍처 → CI/CD

---

## 다음 단계 (Planned Work)

| 항목 | 설명 |
|---|---|
| Member / Auth | 회원 도메인 + JWT 인증 적용 |
| Wallet (예치금) | 회원별 예치금 충전·사용 |
| Seller | 판매자 도메인 및 상품 등록 권한 |
| Settlement | 판매 정산 처리 |
| Event-driven flow | 도메인 이벤트 발행·구독 (Spring Event / Kafka) |
| CI/CD pipeline | GitHub Actions 기반 빌드·테스트 자동화 |

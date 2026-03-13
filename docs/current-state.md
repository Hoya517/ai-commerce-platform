# Current State — AI Commerce Platform

> 기준일: 2026-03-13 (ISSUE-07 반영)

## 개요

Spring Boot 4.0.3 기반의 커머스 백엔드. 현재는 단일 모놀리식 애플리케이션으로, 6개의 Bounded Context를 레이어드 아키텍처로 구현한 상태. JWT 기반 인증 적용 완료.

---

## 도메인 구성

| Bounded Context | 핵심 엔티티 | 책임 |
|---|---|---|
| **Catalog** | `Product` | 상품 등록·조회·상태 관리, 재고 차감 |
| **Cart** | `Cart`, `CartItem` | 회원별 장바구니 관리 (추가·수량변경·삭제) |
| **Order** | `Order`, `OrderItem` | 주문 생성·조회·취소, 결제 완료 상태 반영 |
| **Payment** | `Payment` | 결제 요청·승인·실패 처리 |
| **Member** | `Member` | 회원 가입·로그인·조회, JWT 발급 |
| **Seller** | `Seller` | 판매자 등록·조회·승인·정지, 상품 등록 권한 검증 |
| **Wallet** | `Wallet` | 회원별 예치금 조회·충전, 차감(도메인 메서드) |
| **Common** | `Money`, `Quantity` | 공통 값 객체 (금액, 수량) |

---

## API 목록

### Product — `/products`

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/products` | 상품 등록 |
| `GET` | `/products/{productId}` | 상품 단건 조회 |
| `PATCH` | `/products/{productId}/status` | 상품 상태 변경 |

### Cart — `/cart` 🔒 JWT 필요

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/cart` | 장바구니 조회 |
| `POST` | `/cart/items` | 상품 추가 (장바구니 없으면 자동 생성) |
| `PATCH` | `/cart/items/{productId}` | 상품 수량 변경 |
| `DELETE` | `/cart/items/{productId}` | 상품 삭제 |

### Order — `/orders`

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| `POST` | `/orders` | 주문 생성 (items 직접 전달) | 🔒 JWT |
| `POST` | `/orders/from-cart` | 장바구니 기반 주문 생성 (Cart 전체 비움) | 🔒 JWT |
| `GET` | `/orders/{orderId}` | 주문 단건 조회 | 공개 |
| `POST` | `/orders/{orderId}/cancel` | 주문 취소 (재고 복구 포함) | 공개 |

### Payment — `/payments`

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/payments` | 결제 요청 |
| `POST` | `/payments/confirm` | 결제 승인 (→ 주문 PAID 상태로 변경) |
| `POST` | `/payments/fail` | 결제 실패 처리 |

### Member — `/members`

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/members` | 회원 가입 (BCrypt 암호화) |
| `POST` | `/members/login` | 로그인 → JWT 발급 |
| `GET` | `/members/{memberId}` | 회원 단건 조회 |

### Wallet — `/wallets` 🔒 JWT 필요

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/wallets/me` | 내 지갑(예치금) 조회 |
| `POST` | `/wallets/charge` | 예치금 충전 |

### Seller — `/sellers` 🔒 일부 JWT 필요

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| `POST` | `/sellers` | 판매자 등록 | 🔒 JWT |
| `GET` | `/sellers/{sellerId}` | 판매자 단건 조회 | 공개 |
| `PATCH` | `/sellers/{sellerId}/approve` | 판매자 승인 | 공개 (추후 ADMIN 전용) |

---

## 인증 방식

```
Authorization: Bearer {token}
```

- 보호 API에 JWT가 없으면 `401 Unauthorized` 반환
- JWT subject에서 memberId 추출 → ThreadLocal(`AuthContext`)로 컨트롤러에 전달
- JJWT 0.12.6, HS256 대칭키, 만료 24시간

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
- **Common/Auth**: `JwtProvider`, `AuthContext`, `JwtAuthenticationFilter`

---

## 기술 스택

| 항목 | 내용 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Build | Gradle 9.3.1 |
| Persistence | Spring Data JPA (Hibernate) |
| Auth | JJWT 0.12.6 (HS256), BCrypt |
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

- 6개 Bounded Context 전체 Domain · Application · Presentation 레이어 구현
- JWT 기반 인증 (로그인 API, BCrypt 암호화, 인증 필터)
- 보호 API에서 memberId 쿼리 파라미터 제거 → JWT에서 추출
- Seller 도메인: 판매자 등록·승인·정지 상태 머신 (PENDING → APPROVED → SUSPENDED)
- 상품 등록 시 승인된 판매자 여부 검증 및 sellerId Product에 저장 (ISSUE-06)
- Wallet 도메인: 회원 가입 시 지갑 자동 생성, 예치금 조회·충전 API (ISSUE-07)
- 장바구니 기반 주문 생성 (`POST /orders/from-cart`), 주문 취소 시 재고 복구 (ISSUE-08)
- 도메인 단위 테스트, 서비스 단위 테스트, 컨트롤러 슬라이스 테스트 모두 통과
- 공통 응답 포맷(`ApiResponse<T>`) 및 전역 예외 처리
- Swagger UI를 통한 API 문서 자동 생성

### 아직 구현되지 않은 기능

- Wallet (예치금)
- Settlement (정산)
- 결제 환불
- 이벤트 기반 처리
- GitHub Actions CI
- 실 DB 연동 (현재 H2 in-memory 사용)

### 다음 단계 예정 작업

Wallet → Settlement → 이벤트 기반 아키텍처 → CI/CD

---

## 다음 단계 (Planned Work)

| 항목 | 설명 |
|---|---|
| Wallet (예치금) | 회원별 예치금 충전·사용 |
| ~~Seller~~ | ~~판매자 도메인 및 상품 등록 권한~~ — 완료 |
| Settlement | 판매 정산 처리 |
| Event-driven flow | 도메인 이벤트 발행·구독 (Spring Event / Kafka) |
| CI/CD pipeline | GitHub Actions 기반 빌드·테스트 자동화 |

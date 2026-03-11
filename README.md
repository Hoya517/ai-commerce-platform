# AI Commerce Platform

Spring Boot 기반 이커머스 백엔드 플랫폼. MSA 아키텍처와 Spring AI 통합을 학습하기 위한 프로젝트입니다.

## Tech Stack

| 항목 | 버전 |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.3 |
| Spring Data JPA | Hibernate |
| SpringDoc OpenAPI | 3.0.2 |
| H2 Database | In-Memory |
| Lombok | - |
| JUnit | 5 |

## Getting Started

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

앱 실행 후 접근 가능한 URL:

| 항목 | URL |
|---|---|
| Swagger UI | http://localhost:9090/swagger-ui.html |
| OpenAPI JSON | http://localhost:9090/v3/api-docs |
| H2 Console | http://localhost:9090/h2-console |

**H2 Console 접속 정보**

| 항목 | 값 |
|---|---|
| JDBC URL | `jdbc:h2:mem:aicommerce` |
| User Name | `sa` |
| Password | (비워두기) |

## Architecture

4개의 Bounded Context로 구성됩니다.

```
src/main/java/com/hoya/aicommerce/
├── common/
│   ├── domain/         # Money, Quantity (Value Object)
│   ├── presentation/   # ApiResponse, GlobalExceptionHandler
│   └── config/         # H2ConsoleConfig
├── catalog/            # 상품 관리
├── cart/               # 장바구니
├── order/              # 주문
└── payment/            # 결제
```

각 도메인은 아래 레이어로 구성됩니다.

```
{domain}/
├── domain/             # Entity, Repository interface, Exception
├── application/        # Service, Command/Result DTO
└── presentation/       # Controller, Request/Response DTO
```

## API Overview

### Product
| Method | URL | 설명 |
|---|---|---|
| POST | `/products` | 상품 등록 |
| GET | `/products/{productId}` | 상품 조회 |
| PATCH | `/products/{productId}/status` | 상품 상태 변경 |

### Cart
| Method | URL | 설명 |
|---|---|---|
| GET | `/cart?memberId=` | 장바구니 조회 |
| POST | `/cart/items` | 상품 추가 |
| PATCH | `/cart/items/{productId}` | 수량 변경 |
| DELETE | `/cart/items/{productId}?memberId=` | 상품 삭제 |

### Order
| Method | URL | 설명 |
|---|---|---|
| POST | `/orders` | 주문 생성 |
| GET | `/orders/{orderId}` | 주문 조회 |
| POST | `/orders/{orderId}/cancel` | 주문 취소 |

### Payment
| Method | URL | 설명 |
|---|---|---|
| POST | `/payments` | 결제 요청 |
| POST | `/payments/confirm` | 결제 승인 |
| POST | `/payments/fail` | 결제 실패 |

## Test Scenario

정상 구매 플로우:

1. `POST /products` — 상품 등록
2. `POST /cart/items` — 장바구니 담기
3. `POST /orders` — 주문 생성 (재고 자동 차감)
4. `POST /payments` — 결제 요청
5. `POST /payments/confirm` — 결제 승인 (주문 상태 → PAID)

## Test

```bash
# 전체 테스트
./gradlew test

# 도메인별 테스트
./gradlew test --tests "com.hoya.aicommerce.catalog.*"
./gradlew test --tests "com.hoya.aicommerce.cart.*"
./gradlew test --tests "com.hoya.aicommerce.order.*"
./gradlew test --tests "com.hoya.aicommerce.payment.*"
```

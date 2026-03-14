# API 명세

> Base URL: `http://localhost:9090`
> 인증: `Authorization: Bearer {JWT_TOKEN}`
> 응답 형식: `{"success": boolean, "data": T, "message": string}`

---

## Member API

### POST /members — 회원 가입

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "MEMBER",
    "status": "ACTIVE"
  }
}
```

**Error (400)** — 중복 이메일
```json
{"success": false, "data": null, "message": "Email already in use"}
```

---

### POST /members/login — 로그인

**Request**
```json
{"email": "user@example.com", "password": "password123"}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "memberId": 1,
    "email": "user@example.com",
    "name": "홍길동"
  }
}
```

---

### GET /members/{memberId} — 회원 조회

**Response (200)** — Member 객체 (위와 동일 구조)

---

## Seller API

### POST /sellers — 판매자 등록 🔒

**Request**
```json
{"businessName": "테스트 사업자", "settlementAccount": "국민은행 123-456"}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "memberId": 1,
    "businessName": "테스트 사업자",
    "settlementAccount": "국민은행 123-456",
    "status": "PENDING"
  }
}
```

---

### GET /sellers/{sellerId} — 판매자 조회

### PATCH /sellers/{sellerId}/approve — 판매자 승인 (관리자)

---

## Product API

### POST /products — 상품 등록 🔒 (승인된 판매자)

**Request**
```json
{
  "name": "나이키 에어맥스",
  "description": "편안한 러닝화",
  "price": 150000,
  "stockQuantity": 100
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "나이키 에어맥스",
    "price": 150000,
    "stockQuantity": 100,
    "status": "ON_SALE",
    "sellerId": 1
  }
}
```

---

### GET /products/{productId} — 상품 조회

### PATCH /products/{productId}/status — 상품 상태 변경

**Request**
```json
{"status": "DISCONTINUED"}
```

---

## Cart API (🔒 모든 엔드포인트 인증 필요)

### GET /cart — 장바구니 조회

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "memberId": 1,
    "items": [
      {"productId": 1, "name": "나이키 에어맥스", "price": 150000, "quantity": 2}
    ]
  }
}
```

### POST /cart/items — 상품 추가

**Request**
```json
{"productId": 1, "quantity": 2}
```

### PATCH /cart/items/{productId} — 수량 변경

**Request**
```json
{"quantity": 5}
```

### DELETE /cart/items/{productId} — 상품 삭제

---

## Order API

### POST /orders — 주문 생성 🔒

**Request**
```json
{
  "items": [
    {"productId": 1, "quantity": 2}
  ]
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "memberId": 1,
    "status": "CREATED",
    "totalAmount": 300000,
    "items": [
      {"productId": 1, "name": "나이키 에어맥스", "price": 150000, "quantity": 2}
    ]
  }
}
```

**Error (400)** — 재고 부족 시 `ProductException`

### POST /orders/from-cart — 장바구니 기반 주문 🔒

### GET /orders/{orderId} — 주문 조회

### POST /orders/{orderId}/cancel — 주문 취소 (재고 복구)

---

## Payment API

### POST /payments — 결제 요청

**Request**
```json
{"orderId": 1, "method": "CARD"}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "paymentId": 1,
    "orderId": 1,
    "amount": 300000,
    "method": "CARD",
    "status": "READY",
    "approvedAt": null
  }
}
```

### POST /payments/confirm — 결제 승인 (PG 콜백)

**Request**
```json
{"paymentId": 1}
```

### POST /payments/wallet — 예치금 결제 🔒

**Request**
```json
{"orderId": 1}
```

**Response (200)** — status: "APPROVED" (즉시 처리)

### POST /payments/{paymentId}/cancel — 결제 취소/환불 🔒

### POST /payments/fail — 결제 실패

---

## Wallet API (🔒 모든 엔드포인트 인증 필요)

### GET /wallets/me — 예치금 조회

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "memberId": 1,
    "balance": 50000
  }
}
```

### POST /wallets/charge — 예치금 충전

**Request**
```json
{"amount": 10000}
```

---

## Settlement API (🔒 모든 엔드포인트 인증 필요)

### GET /settlements/me — 내 정산 목록 (판매자)

**Response (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "sellerId": 1,
      "periodStart": "2026-03-01",
      "periodEnd": "2026-03-31",
      "grossAmount": 1000000,
      "feeAmount": 100000,
      "netAmount": 900000,
      "status": "PENDING"
    }
  ]
}
```

### GET /settlements/{id} — 정산 상세 조회 (판매자)

### POST /settlements/batch — 배치 수동 실행

**Request** (optional)
```json
{"targetDate": "2026-02-28"}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "jobExecutionId": 1,
    "processedCount": 5
  }
}
```

---

## 공통 에러 코드

| HTTP Status | 발생 조건 |
|-------------|---------|
| 400 | 도메인 예외 (재고 부족, 중복 가입, 잔액 부족 등) |
| 401 | JWT 없거나 유효하지 않음 (`@RequiresAuth` 엔드포인트) |
| 500 | 서버 내부 오류 |

---

## 인증 방식

1. `POST /members/login` → JWT 토큰 발급
2. 이후 요청 헤더에 `Authorization: Bearer {token}` 포함
3. `@RequiresAuth` 어노테이션이 붙은 엔드포인트는 토큰 없으면 401 반환

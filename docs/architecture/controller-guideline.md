# Controller Guideline

## 1. 목적

이 문서는 ai-commerce-platform 프로젝트에서 Controller(presentation layer)를 작성할 때 따르는 규칙을 정의한다.

Controller는 **비즈니스 로직을 수행하는 계층이 아니라 HTTP 요청을 처리하고 Application Service를 호출하는 얇은 계층(thin controller)**이다.

Controller의 핵심 역할:
- HTTP 요청 처리
- Request DTO validation
- Request DTO → Command DTO 변환
- Service 호출
- Result DTO → Response DTO 변환
- ApiResponse 반환

---

## 2. 아키텍처 구조

```
presentation   — HTTP 요청 처리, validation, DTO 변환
application    — 유스케이스 실행, 트랜잭션 관리, domain 객체 orchestration
domain         — 핵심 비즈니스 규칙, 상태 변경 로직, Aggregate / Entity / Value Object
infrastructure — DB 접근, 외부 API, Redis / Kafka / Search, Repository 구현체
```

---

## 3. Controller 책임

Controller는 아래 역할만 수행한다.

1. HTTP 요청 수신
2. Request DTO validation
3. Request DTO → Command DTO 변환
4. Service 호출
5. Result DTO → Response DTO 변환
6. ApiResponse 반환

즉 Controller는 **orchestration만 수행한다.**

---

## 4. Controller가 하면 안 되는 것

### 비즈니스 로직

❌ 나쁜 예
```java
if (product.getStock() < quantity) {
    throw new IllegalArgumentException();
}
```

비즈니스 규칙은 domain 또는 service 계층에 위치해야 한다.

### Repository 직접 호출

❌ 나쁜 예
```java
productRepository.findById(productId);
```

Repository는 application service에서만 호출한다.

### Domain 상태 직접 변경

❌ 나쁜 예
```java
order.setStatus(OrderStatus.PAID);
product.setStock(product.getStock() - quantity);
```

✅ 좋은 예
```java
order.markPaid();
product.decreaseStock(quantity);
```

---

## 5. Controller 코드 흐름

```
Request DTO
    ↓ validation
Command DTO 변환
    ↓
Service 호출
    ↓
Result DTO 반환
    ↓
Response DTO 변환
    ↓
ApiResponse 반환
```

---

## 6. 패키지 구조

```
presentation/
├── common/
│   ├── ApiResponse
│   └── GlobalExceptionHandler
├── catalog/
│   ├── ProductController
│   ├── request/
│   └── response/
├── cart/
│   ├── CartController
│   ├── request/
│   └── response/
├── order/
│   ├── OrderController
│   ├── request/
│   └── response/
└── payment/
    ├── PaymentController
    ├── request/
    └── response/
```

---

## 7. Controller 코드 예시

```java
@Tag(name = "Cart", description = "장바구니 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "장바구니 상품 추가")
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        AddCartItemCommand command = new AddCartItemCommand(
                request.memberId(), request.productId(), request.quantity()
        );
        return ApiResponse.success(CartResponse.from(cartService.addItem(command)));
    }
}
```

특징:
- Service만 호출
- Repository 호출 없음
- 비즈니스 로직 없음
- Request → Command 변환
- `@Tag`, `@Operation`으로 Swagger 문서화

---

## 8. Request DTO 규칙

Request DTO는 presentation 계층에 위치한다.

- validation annotation 사용
- service command와 분리

```java
public record AddCartItemRequest(
    @NotNull Long memberId,
    @NotNull Long productId,
    @Min(1) int quantity
) {}
```

---

## 9. Response DTO 규칙

Response DTO는 프론트 응답 전용 DTO이다.

- service result DTO와 분리
- 필요한 필드만 포함
- `static from(Result)` 팩토리 메서드 사용

```java
public record CartItemResponse(
    Long cartItemId,
    Long productId,
    String productName,
    int quantity
) {
    public static CartItemResponse from(CartItemResult result) { ... }
}
```

---

## 10. ApiResponse 규칙

모든 API 응답은 `ApiResponse`로 감싼다.

성공 응답:
```json
{
  "success": true,
  "data": {},
  "message": "OK"
}
```

실패 응답:
```json
{
  "success": false,
  "data": null,
  "message": "주문을 찾을 수 없습니다"
}
```

---

## 11. REST API 경로 규칙

| API | Method | 경로 |
|---|---|---|
| Product | GET | `/products/{productId}` |
| Product | POST | `/products` |
| Product | PATCH | `/products/{productId}/status` |
| Cart | GET | `/cart?memberId=` |
| Cart | POST | `/cart/items` |
| Cart | PATCH | `/cart/items/{productId}` |
| Cart | DELETE | `/cart/items/{productId}?memberId=` |
| Order | POST | `/orders` |
| Order | GET | `/orders/{orderId}` |
| Order | POST | `/orders/{orderId}/cancel` |
| Payment | POST | `/payments` |
| Payment | POST | `/payments/confirm` |
| Payment | POST | `/payments/fail` |

---

## 12. Controller 작성 체크리스트

- [ ] Controller에 비즈니스 로직이 없는가
- [ ] Repository를 직접 호출하지 않는가
- [ ] Request DTO validation이 있는가 (`@Valid`)
- [ ] Request DTO → Command 변환이 있는가
- [ ] ApiResponse로 응답하는가
- [ ] Service만 호출하는가
- [ ] `@Tag`, `@Operation` Swagger 어노테이션이 있는가

---

## 13. 핵심 원칙 요약

- Controller는 **thin controller**
- Service만 호출
- Repository 호출 금지
- Request → Command 변환
- Result → Response 변환
- 모든 응답은 `ApiResponse` 사용
- `@Tag`, `@Operation`으로 Swagger 문서화

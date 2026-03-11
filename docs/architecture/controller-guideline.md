Controller Development Guideline
1. 목적

이 문서는 ai-commerce-platform 프로젝트에서 Controller(presentation layer)를 작성할 때 따르는 규칙을 정의한다.

Controller는 **비즈니스 로직을 수행하는 계층이 아니라 HTTP 요청을 처리하고 Application Service를 호출하는 얇은 계층(thin controller)**이다.

Controller의 핵심 역할은 다음과 같다.

HTTP 요청 처리

Request DTO validation

Request DTO → Command DTO 변환

Service 호출

Result DTO → Response DTO 변환

ApiResponse 반환

2. 아키텍처 구조

프로젝트는 Layered Architecture를 사용한다.

presentation
application
domain
infrastructure
presentation

역할

HTTP 요청 처리

Request DTO validation

Request DTO → Command 변환

Service 호출

Result DTO → Response DTO 변환

application

역할

유스케이스 실행

트랜잭션 관리

여러 domain 객체 orchestration

domain

역할

핵심 비즈니스 규칙

상태 변경 로직

Aggregate / Entity / Value Object

infrastructure

역할

DB 접근

외부 API

Redis / Kafka / Search

Repository 구현체

3. Controller 책임

Controller는 아래 역할만 수행한다.

HTTP 요청 수신

Request DTO validation

Request DTO → Command DTO 변환

Service 호출

Result DTO → Response DTO 변환

ApiResponse 반환

즉 Controller는 orchestration만 수행한다.

4. Controller가 하면 안 되는 것

Controller에는 다음 로직이 존재하면 안 된다.

비즈니스 로직

❌ 나쁜 예

if(product.getStock() < quantity){
throw new IllegalArgumentException();
}

비즈니스 규칙은 domain 또는 service 계층에 위치해야 한다.

Repository 호출

❌ 나쁜 예

productRepository.findById(productId);

Repository는 application service에서만 호출한다.

Domain 상태 직접 변경

❌ 나쁜 예

order.setStatus(OrderStatus.PAID);
product.setStock(product.getStock() - quantity);

상태 변경은 반드시 도메인 메서드를 사용한다.

✔ 좋은 예

order.markPaid();
product.decreaseStock(quantity);
5. Controller 코드 흐름

Controller의 기본 흐름은 아래와 같다.

Request DTO
↓
validation
↓
Command DTO 변환
↓
Service 호출
↓
Result DTO 반환
↓
Response DTO 변환
↓
ApiResponse 반환
6. 패키지 구조

추천 패키지 구조

presentation
├ common
│  ├ ApiResponse
│  └ GlobalExceptionHandler
│
├ product
│  ├ ProductController
│  ├ request
│  └ response
│
├ cart
│  ├ CartController
│  ├ request
│  └ response
│
├ order
│  ├ OrderController
│  ├ request
│  └ response
│
└ payment
├ PaymentController
├ request
└ response
7. Controller 코드 예시
   @RestController
   @RequiredArgsConstructor
   @RequestMapping("/cart")
   public class CartController {

   private final CartService cartService;

   @PostMapping("/items")
   public ApiResponse<Void> addItem(
   @Valid @RequestBody AddCartItemRequest request
   ) {

        AddCartItemCommand command =
            new AddCartItemCommand(
                request.memberId(),
                request.productId(),
                request.quantity()
            );

        cartService.addItem(command);

        return ApiResponse.success(null);
   }
   }

Controller 특징

Service만 호출

Repository 호출 없음

비즈니스 로직 없음

Request → Command 변환

8. Request DTO 규칙

Request DTO는 presentation 계층에 위치한다.

특징

validation annotation 사용

service command와 분리

예시

public record AddCartItemRequest(

    @NotNull
    UUID memberId,

    @NotNull
    UUID productId,

    @Min(1)
    int quantity
) {}
9. Response DTO 규칙

Response DTO는 프론트 응답 전용 DTO이다.

특징

service result DTO와 분리

필요한 필드만 포함

예시

public record CartItemResponse(
UUID cartItemId,
UUID productId,
String productName,
int quantity,
int price
) {}
10. ApiResponse 규칙

모든 API 응답은 ApiResponse로 감싼다.

성공 응답

{
"success": true,
"data": {},
"message": "OK"
}

실패 응답

{
"success": false,
"data": null,
"message": "주문을 찾을 수 없습니다"
}
11. REST API 경로 규칙

권장 REST 경로

Product API
GET /products
GET /products/{productId}
Cart API
POST /cart/items
PATCH /cart/items/{cartItemId}
DELETE /cart/items/{cartItemId}
GET /cart
Order API
POST /orders
GET /orders/{orderId}
POST /orders/{orderId}/cancel
Payment API
POST /payments
POST /payments/confirm
12. Controller 작성 체크리스트

Controller 작성 후 아래 항목을 확인한다.

Controller에 비즈니스 로직이 없는가

Repository를 직접 호출하지 않는가

Request DTO validation이 있는가

Request DTO → Command 변환이 있는가

ApiResponse로 응답하는가

Service만 호출하는가

13. 현재 단계에서 하지 않는 것

현재 프로젝트 단계에서는 아래 기능을 추가하지 않는다.

인증 / 인가

Swagger 설정

Pagination

검색 조건

캐싱

이 기능들은 이후 단계에서 확장한다.

14. 핵심 원칙 요약

Controller는 thin controller

Service만 호출

Repository 호출 금지

Request → Command 변환

Result → Response 변환

모든 응답은 ApiResponse 사용
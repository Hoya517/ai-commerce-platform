# MSA 전환 로드맵 (ISSUE-36~40)

---

## M1: 서비스 분리 기준 정의

### 분리 판단 기준

1. **변경 빈도 차이**: Catalog는 상품 수시 업데이트, Auth는 안정적
2. **팀 구조**: 결제팀과 정산팀이 분리되면 Payment/Settlement 분리
3. **장애 격리**: 결제 장애가 검색에 영향 주면 안 됨
4. **성능 요구사항 차이**: 검색은 고TPS, 정산은 저TPS
5. **데이터 일관성**: 강한 일관성이 필요하면 같은 서비스

### 1단계 분리 후보

```
ai-commerce-platform (현재)
    │
    ├─ auth-service          ← Member, JWT, Wallet
    ├─ catalog-service       ← Product, Cart
    ├─ order-payment-service ← Order, Payment
    └─ settlement-service    ← Settlement (비동기, 독립적)
```

**분리 기준 근거**:
- Settlement: Kafka 소비자로 운영, 독립 DB 적합
- Catalog+Cart: 읽기 많음 → 캐시 전략 필요
- Order+Payment: 강한 일관성 필요 → 함께 유지

---

## M2: API Gateway 설계

### 역할

- 인증/인가 (JWT 검증을 Gateway에서 일원화)
- 라우팅 (서비스별 URL 매핑)
- Rate Limiting
- 로드 밸런싱
- Circuit Breaker

### 기술 선택: Spring Cloud Gateway

```yaml
# application.yml (gateway)
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/members/**, /wallets/**
        - id: catalog-service
          uri: lb://catalog-service
          predicates:
            - Path=/products/**, /cart/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/orders/**, /payments/**
        - id: settlement-service
          uri: lb://settlement-service
          predicates:
            - Path=/settlements/**
```

### JWT 검증 흐름

```
클라이언트 → API Gateway (JWT 검증) → 서비스 (memberId 헤더로 전달)
                │
                └─ 유효하지 않으면 401 즉시 반환
```

---

## M3: 서비스 간 통신 전략

### 동기 통신 (REST/gRPC)

**사용 상황**: 응답이 즉시 필요한 경우
- Order → Product (재고 확인/차감)
- Settlement → Seller (판매자 검증)

**권장**: gRPC (타입 안전, 성능 우수)

```protobuf
service ProductService {
  rpc DecreaseStock(DecreaseStockRequest) returns (DecreaseStockResponse);
}

message DecreaseStockRequest {
  int64 product_id = 1;
  int32 quantity = 2;
}
```

### 비동기 통신 (Kafka)

**사용 상황**: 결과를 기다릴 필요 없는 경우
- Payment → Settlement (정산 누적)
- Order → Notification (주문 알림)

### 통신 결정 매트릭스

| 발신 서비스 | 수신 서비스 | 방식 | 이유 |
|------------|-----------|------|------|
| Order | Product | 동기 REST | 재고 차감 즉시 확인 필요 |
| Payment | Settlement | 비동기 Kafka | 정산은 지연 허용 |
| Payment | Wallet | 동기 REST | 잔액 부족 즉시 응답 필요 |
| Order | Notification | 비동기 Kafka | 알림은 지연 허용 |

---

## M4: 서비스별 데이터베이스 분리 전략

### Database-per-Service 패턴

```
auth-service      → PostgreSQL (members, wallets)
catalog-service   → PostgreSQL (products, carts)
                     + Elasticsearch (product 검색)
order-service     → PostgreSQL (orders, payments)
settlement-service → PostgreSQL (settlements)
```

### 분산 데이터 일관성 전략

**Saga 패턴 (주문-결제 시나리오)**:

```
Order Service      Payment Service    Inventory Service
     │                   │                   │
  createOrder()          │                   │
     │──── OrderCreatedEvent ───────────────▶│
     │                   │         decreaseStock()
     │                   │                   │
     │◀──────────────────│── StockDecreasedEvent
     │         requestPayment()               │
     │                   │                   │
     │◀────────── PaymentApprovedEvent        │
  markPaid()             │                   │
```

보상 트랜잭션: 결제 실패 시 재고 복구 이벤트 발행

---

## M5: Kubernetes 배포 전략

### 서비스별 리소스 요구사항

| 서비스 | CPU | Memory | Replicas |
|--------|-----|--------|---------|
| auth-service | 0.5 | 512Mi | 2 |
| catalog-service | 1.0 | 1Gi | 3 |
| order-service | 1.0 | 1Gi | 3 |
| settlement-service | 0.5 | 512Mi | 1 |
| api-gateway | 0.5 | 512Mi | 2 |

### 배포 전략

- **Rolling Update**: 무중단 배포 (기본)
- **Blue-Green**: 중요 서비스 (Payment)
- **Canary**: 새 기능 점진 배포

### 헬스체크

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 9090
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 9090
```

### 설정 관리

- ConfigMap: 환경별 설정
- Secret: JWT_SECRET, DB 비밀번호
- Horizontal Pod Autoscaler: CPU 60% 기준 스케일아웃

---

## 전환 로드맵 타임라인

```
Phase 0 (현재): 모놀리식, H2, Spring Events
Phase 1: PostgreSQL 전환 + Docker 컨테이너화
Phase 2: Kafka 이벤트 전환 (Settlement 비동기)
Phase 3: API Gateway 도입 (인증 일원화)
Phase 4: Settlement 서비스 분리
Phase 5: Catalog+Search 서비스 분리 (Elasticsearch)
Phase 6: Kubernetes 전환
```

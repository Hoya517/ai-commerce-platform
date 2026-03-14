# Gap Checklist

> 기준일: 2026-03-13
> 목표 대비 현재 구현 상태를 점검하는 체크리스트.

---

## A. 필수 기능

### Member / Auth

- [x] Member Entity
- [x] 회원가입 API
- [x] 로그인 API
- [x] JWT 인증
- [x] 인증 필터 적용

### Product / Catalog

- [ ] 상품 목록 조회
- [ ] 상품 검색 / 필터

### Cart

- [x] 장바구니 → 주문 변환 (POST /orders/from-cart)

### Order

- [ ] 회원별 주문 조회
- [x] 주문 취소 시 재고 복구
- [ ] 주문 상태 관리

### Payment

- [x] PG 연동 — MockPgGateway 구현 (prepare/confirm/cancel 시뮬레이션) (ISSUE-13)
- [x] 결제 취소
- [x] 결제 환불 (WALLET 자동 환불 포함)
- [x] OrderStatus / PaymentStatus 상태 머신 가드 강화 (ISSUE-11)
- [x] 민감정보 암호화 — AES-256 DB 암호화, API 응답 마스킹, 환경변수 이전 (ISSUE-12)

### Wallet (예치금)

- [x] 예치금 Entity
- [x] 예치금 충전
- [x] 예치금 차감 (도메인 메서드, 결제 연동 시 사용)
- [x] 예치금 차감 결제 (POST /payments/wallet, ISSUE-09)

### Seller

- [x] 판매자 등록
- [x] 판매자 승인/정지 상태 관리
- [x] 상품 등록 시 승인된 판매자 검증
- [x] Product에 sellerId 연결 (응답에 sellerId 포함)

### Settlement

- [x] 정산 Entity — Settlement (PENDING→COMPLETED), SettlementStatus, SettlementRepository (ISSUE-17)
- [x] 수수료 정책 — FeePolicy (STANDARD_RATE 10%, calculateFee/Net) (ISSUE-18)
- [x] 정산 대상 적재 — SettlementService.accumulate(), SettlementEventListener 연결 (ISSUE-19)
- [x] 판매자 정산 조회 — GET /settlements/me, GET /settlements/{id} (ISSUE-21)
- [x] 정산 배치 — Spring Batch 6 Job, @Scheduled 월말 자동 실행, POST /settlements/batch 수동 API (ISSUE-20)

### Event Architecture

- [x] OrderCreatedEvent (ISSUE-14)
- [x] PaymentConfirmedEvent (ISSUE-14)
- [x] PaymentCanceledEvent (ISSUE-14)
- [x] Event Listener 구현 — SettlementEventListener (@TransactionalEventListener + @Async) (ISSUE-15)
- [x] 이벤트 기반 정합성 전략 문서화 (ISSUE-16) → docs/architecture/event-driven.md
- [x] Kafka POC 구현 — KafkaEventBridge, SettlementKafkaConsumer, EventIdempotencyService (ISSUE-31)
  - KRaft 모드 Kafka + Kafka UI docker-compose 추가
  - spring-kafka 의존성, kafka.enabled 조건부 활성화
  - SettlementEventListener: kafka.enabled=false 시만 활성화
  - ProcessedEvent 테이블로 at-least-once 중복 방지

### Auth

- [x] 인증 경로 하드코딩 제거 — @RequiresAuth 어노테이션 기반 선언적 관리 (ISSUE-27)

### CI/CD

- [x] Github Actions CI — .github/workflows/ci.yml (ISSUE-22)
- [x] 테스트 자동 실행 — push/PR to main 시 자동 빌드+테스트

---

## B. 품질 보강

- [ ] 실 DB (MySQL/PostgreSQL)
- [ ] Flyway/Liquibase
- [x] Redis 선차감 재고 관리 — RedisStockRepository, StockService (ISSUE-30)
- [x] Queue 적재 및 Worker DB 반영 — StockDecreaseEvent, StockDecreaseWorker (@Async+@TransactionalEventListener) (ISSUE-30)
- [x] OrderService Redis 선차감 통합 — Optional<StockService> 주입, reserve()/fallback 흐름, 이벤트 발행 (ISSUE-30)
- [x] Dockerfile + docker-compose.yml (ISSUE-23)
- [x] 프로파일 분리 — application-local.yaml, application-test.yaml (ISSUE-23)

---

## D. 테스트

- [x] 핵심 플로우 통합 테스트 — MemberIntegrationTest, AuthFilterIntegrationTest, SellerFlowIntegrationTest, OrderFlowIntegrationTest (ISSUE-28)

---

## E. 문서화

- [x] 아키텍처 다이어그램 — docs/architecture/overview.md (ISSUE-24)
- [x] API 명세 — docs/api-spec.md (ISSUE-25)
- [x] 고민 포인트 문서화 — docs/presentation/talking-points.md (ISSUE-26)
- [x] Kafka 이벤트 설계 — docs/architecture/kafka-event-design.md (ISSUE-31)
- [x] Elasticsearch 검색 설계 — docs/architecture/elasticsearch-design.md (ES1-4)
- [x] MSA 전환 로드맵 — docs/architecture/msa-roadmap.md (M1-5)

---

## C. 최종 프로젝트 확장 후보

- [ ] ElasticSearch
- [ ] Spring AI
- [ ] RAG
- [ ] 추천 시스템
- [ ] 리뷰 / 평점
- [ ] 알림 시스템

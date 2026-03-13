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

- [ ] 장바구니 → 주문 변환

### Order

- [ ] 회원별 주문 조회
- [ ] 주문 취소 시 재고 복구
- [ ] 주문 상태 관리

### Payment

- [ ] PG 연동 (Mock 또는 실제)
- [ ] 결제 취소
- [ ] 결제 환불

### Wallet (예치금)

- [ ] 예치금 Entity
- [ ] 예치금 충전
- [ ] 예치금 차감 결제

### Seller

- [x] 판매자 등록
- [x] 판매자 승인/정지 상태 관리
- [x] 상품 등록 시 승인된 판매자 검증

### Settlement

- [ ] 정산 Entity
- [ ] 수수료 정책
- [ ] 정산 배치
- [ ] 판매자 정산 조회

### Event Architecture

- [ ] OrderCreatedEvent
- [ ] PaymentConfirmedEvent
- [ ] Event Listener 구현

### CI/CD

- [ ] Github Actions CI
- [ ] 테스트 자동 실행

---

## B. 품질 보강

- [ ] 실 DB (MySQL/PostgreSQL)
- [ ] Flyway/Liquibase
- [ ] Redis 캐싱
- [ ] Dockerfile
- [ ] 프로파일 분리

---

## C. 최종 프로젝트 확장 후보

- [ ] ElasticSearch
- [ ] Spring AI
- [ ] RAG
- [ ] 추천 시스템
- [ ] 리뷰 / 평점
- [ ] 알림 시스템

# 고민 포인트 — 설계 판단 근거

> "왜 이렇게 설계했는지" 발표에서 말할 수 있도록 정리한 문서

---

## 1. 회원을 별도 도메인으로 분리한 이유

**문제**: 회원 정보(이메일·비밀번호·이름)를 어디에서 관리할 것인가.

**판단**: Member 도메인을 별도 Bounded Context로 분리.

**근거**:
- 인증(JWT 발급)과 상품/주문 도메인은 책임이 다름
- 추후 MSA 전환 시 Auth Service로 독립 분리 가능
- 다른 도메인은 `memberId`로만 참조 → 의존 방향 단방향 유지
- `Member.promoteToSeller()`처럼 역할 변경 로직이 Member에 응집됨

---

## 2. 예치금(Wallet) 도입 이유

**문제**: 실제 PG(결제 게이트웨이) 연동 없이 결제 플로우를 구현해야 함.

**판단**: Wallet(예치금) 도메인 도입. 사전 충전 후 즉시 차감 방식.

**근거**:
- 실 PG 없이도 완전한 결제→정산 흐름 구현 가능
- `PaymentMethod.WALLET` 분기로 즉시 APPROVED (PG 콜백 불필요)
- 잔액 부족 시 `WalletException` → 원자적 트랜잭션 처리
- 학습 목적상 "실제처럼" 동작하는 플로우를 만드는 것이 핵심

---

## 3. 정산(Settlement)을 별도 도메인으로 분리한 이유

**문제**: 결제가 완료될 때마다 판매자 정산 금액을 집계해야 함.

**판단**: Settlement 도메인 + 이벤트 리스너 분리.

**근거**:
- Payment 도메인이 정산 로직까지 알면 단일 책임 원칙 위반
- `PaymentConfirmedEvent`를 구독해 비동기 처리 → 결합도 ↓
- 월말 배치로 PENDING → COMPLETED 상태 전환 (실무 정산 패턴 반영)
- 수수료 정책(`FeePolicy.STANDARD_RATE=10%`)을 Settlement에서 중앙 관리

---

## 4. 이벤트 기반 아키텍처 도입 이유

**문제**: 결제 완료 시 정산 누적, 재고 복구 등 후속 처리를 어디서 할 것인가.

**판단**: Spring ApplicationEvent + @TransactionalEventListener.

**근거**:
- 서비스 간 직접 호출 시 의존성 순환 위험 (Payment ↔ Settlement)
- `AFTER_COMMIT` 시점 리스닝으로 트랜잭션 원자성 보장
- 추후 Kafka 전환 시 이벤트 클래스 재사용 가능 → 전환 비용 최소화
- 이벤트에 `sellerId`를 포함해 Settlement가 memberId 조회 없이 처리 가능

---

## 5. 재고 비관적 락 선택 이유

**문제**: 동시 주문 시 재고 Overselling 방지.

**판단**: 비관적 락(`@Lock(PESSIMISTIC_WRITE)`, SELECT FOR UPDATE).

**근거**:
- 낙관적 락: 충돌 시 retry 폭발 → 재고 경쟁이 심한 상황에서 비효율
- 비관적 락: 직렬 처리 보장, 구현 단순
- 트레이드오프: DB 병목 발생 가능 → 고트래픽 대비는 Redis 선차감(ISSUE-30)으로 대응 예정
- `ProductRepository.findByIdWithLock()` 메서드 분리로 의도 명시

---

## 6. 인증 방식: 커스텀 JWT 필터 + @RequiresAuth 어노테이션

**문제**: Spring Security 풀 도입 vs 직접 구현.

**판단**: 직접 JWT 필터 + `@RequiresAuth` HandlerInterceptor.

**근거**:
- 학습 목적: JWT 검증 메커니즘을 직접 이해하는 것이 핵심
- Spring Security auto-configuration의 복잡성 배제
- `@RequiresAuth` 어노테이션으로 선언적 인증 관리 → 하드코딩 제거
- 인증이 필요한 엔드포인트를 코드 레벨에서 즉시 파악 가능

---

## 7. 추후 MSA 확장 포인트

| 현재 (모놀리식) | MSA 전환 후 |
|----------------|-----------|
| Spring Events | Kafka Topics |
| 단일 H2 DB | 서비스별 PostgreSQL |
| 직접 서비스 호출 | gRPC / REST API |
| 단일 배포 | Kubernetes Pod 분리 |
| JWT 직접 검증 | API Gateway OAuth2 |

**전환 순서 (권장)**:
1. PostgreSQL 전환 (실DB)
2. Kafka 이벤트 전환 (Settlement, 알림)
3. API Gateway 도입 (인증 통합)
4. 서비스별 DB 분리
5. Kubernetes 배포

**분리 기준**: 변경 빈도·팀 구조·장애 격리 필요성이 다른 도메인부터.
- 가장 먼저: Settlement (독립적, I/O 집중)
- 다음: Catalog + Cart (검색 트래픽 집중)
- 마지막: Member (다른 서비스가 의존하므로 신중하게)

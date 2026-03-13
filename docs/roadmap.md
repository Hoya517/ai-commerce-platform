# Development Roadmap

> AI Commerce Platform의 주요 기능 구현 순서 및 시스템 발전 방향을 정의합니다.

---

## Phase 1 — Member & Authentication

**Goal:** Introduce a proper user system and remove direct memberId parameters from APIs.

- [x] Member Entity
- [x] 회원가입 API
- [x] 로그인 API
- [x] JWT Authentication
- [x] Security Filter

---

## Phase 2 — Seller & Product Ownership

**Goal:** Introduce seller concept and connect products to sellers.

- [ ] Seller Entity
- [ ] 판매자 등록
- [ ] Product ↔ Seller relationship

---

## Phase 3 — Wallet (Balance System)

**Goal:** Introduce a wallet system for user balance and internal payments.

- [ ] Wallet Entity
- [ ] Balance charge API
- [ ] Balance deduction for orders

---

## Phase 4 — Order Flow Improvements

**Goal:** Improve order lifecycle and cart integration.

- [ ] Cart → Order conversion
- [ ] Order history API
- [ ] Cancel order with stock restoration

---

## Phase 5 — Settlement System

**Goal:** Introduce seller settlement and commission handling.

- [ ] Settlement Entity
- [ ] Commission policy
- [ ] Settlement batch job
- [ ] Seller settlement API

---

## Phase 6 — Event Driven Architecture

**Goal:** Introduce domain events to decouple services.

- [ ] OrderCreatedEvent
- [ ] PaymentConfirmedEvent
- [ ] Event listener

---

## Phase 7 — CI/CD and Infrastructure

**Goal:** Introduce automated build and deployment preparation.

- [ ] Github Actions CI
- [ ] Dockerfile
- [ ] Environment profiles

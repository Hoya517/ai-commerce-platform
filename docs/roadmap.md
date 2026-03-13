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

## Phase 2 — Seller & Product Ownership ✅

**Goal:** Introduce seller concept and connect products to sellers.

- [x] Seller Entity
- [x] 판매자 등록 (등록·조회·승인·정지 API)
- [x] 상품 등록 시 승인된 판매자 검증
- [x] Product에 sellerId 연결 (ISSUE-06)

---

## Phase 3 — Wallet (Balance System) ✅

**Goal:** Introduce a wallet system for user balance and internal payments.

- [x] Wallet Entity
- [x] Auto-create wallet on member registration
- [x] Balance charge API
- [x] Balance deduction domain method
- [x] Wallet payment API (POST /payments/wallet, ISSUE-09)

---

## Phase 4 — Order Flow Improvements ✅ (partial)

**Goal:** Improve order lifecycle and cart integration.

- [x] Cart → Order conversion (POST /orders/from-cart, clears cart after order)
- [ ] Order history API
- [x] Cancel order with stock restoration
- [x] Payment cancel/refund API (POST /payments/{id}/cancel, ISSUE-10)

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

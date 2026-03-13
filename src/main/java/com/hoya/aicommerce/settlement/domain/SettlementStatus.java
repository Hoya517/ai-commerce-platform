package com.hoya.aicommerce.settlement.domain;

public enum SettlementStatus {
    PENDING,    // 정산 대상 적재 중 (결제 건 누적)
    COMPLETED   // 정산 확정 완료
}

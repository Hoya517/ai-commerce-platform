package com.hoya.aicommerce.settlement.presentation.response;

import com.hoya.aicommerce.settlement.application.dto.SettlementResult;
import com.hoya.aicommerce.settlement.domain.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementResponse(
        Long id,
        Long sellerId,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal netAmount,
        SettlementStatus status
) {
    public static SettlementResponse from(SettlementResult result) {
        return new SettlementResponse(
                result.id(),
                result.sellerId(),
                result.periodStart(),
                result.periodEnd(),
                result.grossAmount(),
                result.feeAmount(),
                result.netAmount(),
                result.status()
        );
    }
}

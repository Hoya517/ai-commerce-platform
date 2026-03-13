package com.hoya.aicommerce.settlement.application.dto;

import com.hoya.aicommerce.settlement.domain.Settlement;
import com.hoya.aicommerce.settlement.domain.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementResult(
        Long id,
        Long sellerId,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal grossAmount,
        BigDecimal feeAmount,
        BigDecimal netAmount,
        SettlementStatus status
) {
    public static SettlementResult from(Settlement settlement) {
        return new SettlementResult(
                settlement.getId(),
                settlement.getSellerId(),
                settlement.getPeriodStart(),
                settlement.getPeriodEnd(),
                settlement.getGrossAmount().getValue(),
                settlement.getFeeAmount().getValue(),
                settlement.getNetAmount().getValue(),
                settlement.getStatus()
        );
    }
}

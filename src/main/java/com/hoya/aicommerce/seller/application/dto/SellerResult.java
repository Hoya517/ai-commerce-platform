package com.hoya.aicommerce.seller.application.dto;

import com.hoya.aicommerce.seller.domain.Seller;
import com.hoya.aicommerce.seller.domain.SellerStatus;

public record SellerResult(
        Long id,
        Long memberId,
        String businessName,
        String settlementAccount,
        SellerStatus status
) {
    public static SellerResult from(Seller seller) {
        return new SellerResult(
                seller.getId(),
                seller.getMemberId(),
                seller.getBusinessName(),
                seller.getSettlementAccount(),
                seller.getStatus()
        );
    }
}

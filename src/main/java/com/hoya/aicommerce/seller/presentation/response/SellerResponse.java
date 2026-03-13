package com.hoya.aicommerce.seller.presentation.response;

import com.hoya.aicommerce.seller.application.dto.SellerResult;
import com.hoya.aicommerce.seller.domain.SellerStatus;

public record SellerResponse(
        Long id,
        Long memberId,
        String businessName,
        String settlementAccount,
        SellerStatus status
) {
    public static SellerResponse from(SellerResult result) {
        return new SellerResponse(
                result.id(),
                result.memberId(),
                result.businessName(),
                result.settlementAccount(),
                result.status()
        );
    }
}

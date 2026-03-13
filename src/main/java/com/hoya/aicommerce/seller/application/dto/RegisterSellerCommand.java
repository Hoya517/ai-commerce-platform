package com.hoya.aicommerce.seller.application.dto;

public record RegisterSellerCommand(
        Long memberId,
        String businessName,
        String settlementAccount
) {}

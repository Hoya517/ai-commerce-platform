package com.hoya.aicommerce.payment.application.dto;

public record PayWithWalletCommand(
        Long orderId,
        Long memberId
) {}

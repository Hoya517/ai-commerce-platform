package com.hoya.aicommerce.wallet.presentation.response;

import com.hoya.aicommerce.wallet.application.dto.WalletResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        Long memberId,
        BigDecimal balance,
        LocalDateTime updatedAt
) {
    public static WalletResponse from(WalletResult result) {
        return new WalletResponse(
                result.id(),
                result.memberId(),
                result.balance(),
                result.updatedAt()
        );
    }
}

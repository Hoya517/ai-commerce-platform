package com.hoya.aicommerce.wallet.application.dto;

import com.hoya.aicommerce.wallet.domain.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResult(
        Long id,
        Long memberId,
        BigDecimal balance,
        LocalDateTime updatedAt
) {
    public static WalletResult from(Wallet wallet) {
        return new WalletResult(
                wallet.getId(),
                wallet.getMemberId(),
                wallet.getBalance().getValue(),
                wallet.getUpdatedAt()
        );
    }
}

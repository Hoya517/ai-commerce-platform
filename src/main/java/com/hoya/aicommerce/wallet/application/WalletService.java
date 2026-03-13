package com.hoya.aicommerce.wallet.application;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.wallet.application.dto.ChargeWalletCommand;
import com.hoya.aicommerce.wallet.application.dto.WalletResult;
import com.hoya.aicommerce.wallet.domain.Wallet;
import com.hoya.aicommerce.wallet.domain.WalletRepository;
import com.hoya.aicommerce.wallet.exception.WalletException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public void createWallet(Long memberId) {
        walletRepository.save(Wallet.create(memberId));
    }

    @Transactional(readOnly = true)
    public WalletResult getWallet(Long memberId) {
        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new WalletException("지갑을 찾을 수 없습니다"));
        return WalletResult.from(wallet);
    }

    @Transactional
    public WalletResult charge(Long memberId, ChargeWalletCommand command) {
        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new WalletException("지갑을 찾을 수 없습니다"));
        wallet.charge(Money.of(command.amount()));
        return WalletResult.from(wallet);
    }
}

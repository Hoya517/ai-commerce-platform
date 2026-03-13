package com.hoya.aicommerce.wallet.application;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.wallet.application.dto.ChargeWalletCommand;
import com.hoya.aicommerce.wallet.application.dto.WalletResult;
import com.hoya.aicommerce.wallet.domain.Wallet;
import com.hoya.aicommerce.wallet.domain.WalletRepository;
import com.hoya.aicommerce.wallet.exception.WalletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    @Test
    void 지갑이_생성된다() {
        given(walletRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        walletService.createWallet(1L);

        verify(walletRepository).save(any());
    }

    @Test
    void 지갑을_조회한다() {
        Wallet wallet = Wallet.create(1L);
        given(walletRepository.findByMemberId(1L)).willReturn(Optional.of(wallet));

        WalletResult result = walletService.getWallet(1L);

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void 존재하지_않는_지갑_조회시_예외가_발생한다() {
        given(walletRepository.findByMemberId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(99L))
                .isInstanceOf(WalletException.class);
    }

    @Test
    void 예치금이_충전된다() {
        Wallet wallet = Wallet.create(1L);
        given(walletRepository.findByMemberId(1L)).willReturn(Optional.of(wallet));

        WalletResult result = walletService.charge(1L, new ChargeWalletCommand(BigDecimal.valueOf(10000)));

        assertThat(result.balance()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    void 존재하지_않는_지갑_충전시_예외가_발생한다() {
        given(walletRepository.findByMemberId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.charge(99L, new ChargeWalletCommand(BigDecimal.valueOf(1000))))
                .isInstanceOf(WalletException.class);
    }
}

package com.hoya.aicommerce.wallet.domain;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.wallet.exception.WalletException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    @Test
    void 지갑이_생성되면_잔액이_0이다() {
        Wallet wallet = Wallet.create(1L);

        assertThat(wallet.getMemberId()).isEqualTo(1L);
        assertThat(wallet.getBalance()).isEqualTo(Money.zero());
    }

    @Test
    void 예치금이_정상_충전된다() {
        Wallet wallet = Wallet.create(1L);
        wallet.charge(Money.of(10000L));

        assertThat(wallet.getBalance()).isEqualTo(Money.of(10000L));
    }

    @Test
    void 예치금을_여러_번_충전할_수_있다() {
        Wallet wallet = Wallet.create(1L);
        wallet.charge(Money.of(5000L));
        wallet.charge(Money.of(3000L));

        assertThat(wallet.getBalance()).isEqualTo(Money.of(8000L));
    }

    @Test
    void 예치금이_정상_차감된다() {
        Wallet wallet = Wallet.create(1L);
        wallet.charge(Money.of(10000L));
        wallet.deduct(Money.of(3000L));

        assertThat(wallet.getBalance()).isEqualTo(Money.of(7000L));
    }

    @Test
    void 잔액이_부족하면_차감시_예외가_발생한다() {
        Wallet wallet = Wallet.create(1L);
        wallet.charge(Money.of(1000L));

        assertThatThrownBy(() -> wallet.deduct(Money.of(5000L)))
                .isInstanceOf(WalletException.class);
    }
}

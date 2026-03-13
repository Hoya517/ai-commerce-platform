package com.hoya.aicommerce.settlement.domain;

import com.hoya.aicommerce.common.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeePolicyTest {

    @Test
    void 수수료_10퍼센트_계산() {
        Money fee = FeePolicy.calculateFee(Money.of(100_000L));

        assertThat(fee).isEqualTo(Money.of(10_000L));
    }

    @Test
    void 수수료_소수점_절사() {
        // 10% of 100,001 = 10,000.1 → 절사 → 10,000
        Money fee = FeePolicy.calculateFee(Money.of(100_001L));

        assertThat(fee).isEqualTo(Money.of(10_000L));
    }

    @Test
    void 순정산액_계산() {
        Money net = FeePolicy.calculateNet(Money.of(100_000L));

        assertThat(net).isEqualTo(Money.of(90_000L));
    }

    @Test
    void 표준_수수료율은_10퍼센트다() {
        assertThat(FeePolicy.STANDARD_RATE).isEqualByComparingTo("0.10");
    }
}

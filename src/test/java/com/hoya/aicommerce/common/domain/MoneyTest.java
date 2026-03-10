package com.hoya.aicommerce.common.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void 유효한_금액으로_생성된다() {
        Money money = Money.of(1000L);
        assertThat(money.getValue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void 음수_금액은_거부된다() {
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zero로_생성된다() {
        Money zero = Money.zero();
        assertThat(zero.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void add_연산이_정상_동작한다() {
        Money result = Money.of(1000L).add(Money.of(500L));
        assertThat(result).isEqualTo(Money.of(1500L));
    }

    @Test
    void multiply_연산이_정상_동작한다() {
        Money result = Money.of(1000L).multiply(3);
        assertThat(result).isEqualTo(Money.of(3000L));
    }

    @Test
    void isGreaterThan_비교가_정상_동작한다() {
        assertThat(Money.of(1500L).isGreaterThan(Money.of(1000L))).isTrue();
        assertThat(Money.of(1000L).isGreaterThan(Money.of(1500L))).isFalse();
    }

    @Test
    void 동일_금액은_equals가_true다() {
        assertThat(Money.of(1000L)).isEqualTo(Money.of(1000L));
    }
}

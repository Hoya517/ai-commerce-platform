package com.hoya.aicommerce.common.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    void 유효한_수량으로_생성된다() {
        Quantity qty = Quantity.of(1);
        assertThat(qty.getValue()).isEqualTo(1);
    }

    @Test
    void 수량_0은_거부된다() {
        assertThatThrownBy(() -> Quantity.of(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 음수_수량은_거부된다() {
        assertThatThrownBy(() -> Quantity.of(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void increase_연산이_정상_동작한다() {
        Quantity result = Quantity.of(3).increase(2);
        assertThat(result).isEqualTo(Quantity.of(5));
    }

    @Test
    void decrease_연산이_정상_동작한다() {
        Quantity result = Quantity.of(3).decrease(1);
        assertThat(result).isEqualTo(Quantity.of(2));
    }

    @Test
    void decrease_결과가_1미만이면_거부된다() {
        assertThatThrownBy(() -> Quantity.of(1).decrease(1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

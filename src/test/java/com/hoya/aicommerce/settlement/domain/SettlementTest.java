package com.hoya.aicommerce.settlement.domain;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.settlement.exception.SettlementException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementTest {

    private static final Long SELLER_ID = 1L;
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 3, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 3, 31);
    private static final BigDecimal FEE_RATE = new BigDecimal("0.10"); // 10%

    @Test
    void 정산_생성_시_초기_상태는_PENDING이고_금액은_0이다() {
        Settlement settlement = Settlement.create(SELLER_ID, PERIOD_START, PERIOD_END);

        assertThat(settlement.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.getGrossAmount()).isEqualTo(Money.zero());
        assertThat(settlement.getFeeAmount()).isEqualTo(Money.zero());
        assertThat(settlement.getNetAmount()).isEqualTo(Money.zero());
    }

    @Test
    void 결제_추가_시_gross_fee_net이_올바르게_계산된다() {
        Settlement settlement = Settlement.create(SELLER_ID, PERIOD_START, PERIOD_END);

        settlement.addPayment(Money.of(100_000L), FEE_RATE);

        assertThat(settlement.getGrossAmount()).isEqualTo(Money.of(100_000L));
        assertThat(settlement.getFeeAmount()).isEqualTo(Money.of(10_000L));  // 10%
        assertThat(settlement.getNetAmount()).isEqualTo(Money.of(90_000L));  // 100000 - 10000
    }

    @Test
    void 결제_여러_건_추가_시_누적_합산된다() {
        Settlement settlement = Settlement.create(SELLER_ID, PERIOD_START, PERIOD_END);

        settlement.addPayment(Money.of(100_000L), FEE_RATE);
        settlement.addPayment(Money.of(200_000L), FEE_RATE);

        assertThat(settlement.getGrossAmount()).isEqualTo(Money.of(300_000L));
        assertThat(settlement.getFeeAmount()).isEqualTo(Money.of(30_000L));  // 10%
        assertThat(settlement.getNetAmount()).isEqualTo(Money.of(270_000L));
    }

    @Test
    void complete_호출_시_COMPLETED_상태가_된다() {
        Settlement settlement = Settlement.create(SELLER_ID, PERIOD_START, PERIOD_END);
        settlement.addPayment(Money.of(100_000L), FEE_RATE);

        settlement.complete();

        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
    }

    @Test
    void COMPLETED_정산에_결제_추가_시_예외가_발생한다() {
        Settlement settlement = Settlement.create(SELLER_ID, PERIOD_START, PERIOD_END);
        settlement.complete();

        assertThatThrownBy(() -> settlement.addPayment(Money.of(50_000L), FEE_RATE))
                .isInstanceOf(SettlementException.class)
                .hasMessageContaining("완료된 정산에는 결제를 추가할 수 없습니다");
    }

    @Test
    void COMPLETED_정산을_다시_complete_시_예외가_발생한다() {
        Settlement settlement = Settlement.create(SELLER_ID, PERIOD_START, PERIOD_END);
        settlement.complete();

        assertThatThrownBy(settlement::complete)
                .isInstanceOf(SettlementException.class)
                .hasMessageContaining("PENDING 상태의 정산만 완료 처리할 수 있습니다");
    }

    @Test
    void 수수료율_소수점_절사_처리된다() {
        Settlement settlement = Settlement.create(SELLER_ID, PERIOD_START, PERIOD_END);

        // 10% of 100,001 = 10,000.1 → 절사 → 10,000
        settlement.addPayment(Money.of(100_001L), FEE_RATE);

        assertThat(settlement.getFeeAmount()).isEqualTo(Money.of(10_000L));
        assertThat(settlement.getNetAmount()).isEqualTo(Money.of(90_001L));
    }
}

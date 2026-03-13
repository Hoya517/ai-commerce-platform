package com.hoya.aicommerce.settlement.domain;

import com.hoya.aicommerce.common.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 정산 수수료 정책.
 *
 * 현재 표준 수수료율: 10%
 * 수수료 계산 시 소수점 이하 절사(FLOOR) 처리.
 */
public class FeePolicy {

    public static final BigDecimal STANDARD_RATE = new BigDecimal("0.10");

    private FeePolicy() {}

    /**
     * 결제 금액에서 수수료를 계산한다.
     *
     * @param paymentAmount 결제 금액
     * @return 수수료 금액 (소수점 이하 절사)
     */
    public static Money calculateFee(Money paymentAmount) {
        BigDecimal fee = paymentAmount.getValue()
                .multiply(STANDARD_RATE)
                .setScale(0, RoundingMode.DOWN);
        return Money.of(fee);
    }

    /**
     * 결제 금액에서 순 정산액을 계산한다.
     *
     * @param paymentAmount 결제 금액
     * @return 순 정산액 (결제 금액 - 수수료)
     */
    public static Money calculateNet(Money paymentAmount) {
        return paymentAmount.subtract(calculateFee(paymentAmount));
    }
}

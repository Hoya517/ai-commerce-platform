package com.hoya.aicommerce.settlement.domain;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.settlement.exception.SettlementException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 판매자 정산 Aggregate Root.
 *
 * 하나의 Settlement = 특정 판매자의 특정 기간(periodStart ~ periodEnd) 정산 내역.
 * 결제 건이 누적될 때마다 addPayment()로 금액을 합산하고,
 * 정산 배치 실행 시 complete()로 확정한다.
 *
 * 모델 분리 결정: 단일 모델 사용 (정산 대상/결과 미분리)
 *   - 정산 대상 적재(ISSUE-19)와 정산 결과가 같은 엔티티에서 관리된다.
 *   - 데이터 규모가 커지면 SettlementItem(건별) + Settlement(집계) 분리를 고려한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sellerId;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "gross_amount"))
    private Money grossAmount;   // 총 판매 금액 (수수료 차감 전)

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "fee_amount"))
    private Money feeAmount;     // 수수료 금액

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "net_amount"))
    private Money netAmount;     // 순 정산 금액 (gross - fee)

    @Enumerated(EnumType.STRING)
    private SettlementStatus status;

    private Settlement(Long sellerId, LocalDate periodStart, LocalDate periodEnd) {
        this.sellerId = sellerId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.grossAmount = Money.zero();
        this.feeAmount = Money.zero();
        this.netAmount = Money.zero();
        this.status = SettlementStatus.PENDING;
    }

    public static Settlement create(Long sellerId, LocalDate periodStart, LocalDate periodEnd) {
        return new Settlement(sellerId, periodStart, periodEnd);
    }

    /**
     * 결제 건을 정산 대상에 추가한다.
     * grossAmount 누적 → feeAmount 재계산 → netAmount = gross - fee
     *
     * @param paymentAmount 결제 금액
     * @param feeRate       수수료율 (예: 0.10 = 10%)
     */
    public void addPayment(Money paymentAmount, BigDecimal feeRate) {
        if (status != SettlementStatus.PENDING) {
            throw new SettlementException("완료된 정산에는 결제를 추가할 수 없습니다");
        }
        Money fee = Money.of(paymentAmount.getValue().multiply(feeRate).setScale(0, RoundingMode.DOWN));
        this.grossAmount = this.grossAmount.add(paymentAmount);
        this.feeAmount = this.feeAmount.add(fee);
        this.netAmount = this.grossAmount.subtract(this.feeAmount);
    }

    /**
     * 결제 취소 건을 정산 대상에서 차감한다.
     * grossAmount 차감 → feeAmount 재계산 → netAmount = gross - fee
     *
     * @param paymentAmount 취소된 결제 금액
     * @param feeRate       수수료율 (예: 0.10 = 10%)
     */
    public void removePayment(Money paymentAmount, BigDecimal feeRate) {
        if (status != SettlementStatus.PENDING) {
            throw new SettlementException("완료된 정산에서는 결제를 차감할 수 없습니다");
        }
        Money fee = Money.of(paymentAmount.getValue().multiply(feeRate).setScale(0, RoundingMode.DOWN));
        this.grossAmount = this.grossAmount.subtract(paymentAmount);
        this.feeAmount = this.feeAmount.subtract(fee);
        this.netAmount = this.grossAmount.subtract(this.feeAmount);
    }

    /**
     * 정산을 확정한다. PENDING → COMPLETED
     */
    public void complete() {
        if (status != SettlementStatus.PENDING) {
            throw new SettlementException("PENDING 상태의 정산만 완료 처리할 수 있습니다");
        }
        this.status = SettlementStatus.COMPLETED;
    }
}

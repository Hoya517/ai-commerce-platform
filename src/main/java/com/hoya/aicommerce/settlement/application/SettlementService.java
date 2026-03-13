package com.hoya.aicommerce.settlement.application;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.settlement.application.dto.SettlementResult;
import com.hoya.aicommerce.settlement.domain.FeePolicy;
import com.hoya.aicommerce.settlement.domain.Settlement;
import com.hoya.aicommerce.settlement.domain.SettlementRepository;
import com.hoya.aicommerce.settlement.exception.SettlementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;

    /**
     * 결제 금액을 판매자의 당월 정산에 누적한다.
     * 해당 기간 PENDING 정산이 없으면 새로 생성한다.
     *
     * @param sellerId      판매자 ID
     * @param paymentAmount 결제 금액
     */
    @Transactional
    public void accumulate(Long sellerId, Money paymentAmount) {
        if (sellerId == null) {
            return;
        }
        LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());

        Settlement settlement = settlementRepository
                .findBySellerIdAndPeriodStartAndPeriodEnd(sellerId, periodStart, periodEnd)
                .orElseGet(() -> settlementRepository.save(
                        Settlement.create(sellerId, periodStart, periodEnd)));

        settlement.addPayment(paymentAmount, FeePolicy.STANDARD_RATE);
    }

    @Transactional(readOnly = true)
    public List<SettlementResult> getSettlements(Long sellerId) {
        return settlementRepository.findBySellerId(sellerId).stream()
                .map(SettlementResult::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementResult getSettlement(Long settlementId, Long sellerId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new SettlementException("정산 내역을 찾을 수 없습니다"));
        if (!settlement.getSellerId().equals(sellerId)) {
            throw new SettlementException("본인의 정산 내역만 조회할 수 있습니다");
        }
        return SettlementResult.from(settlement);
    }
}

package com.hoya.aicommerce.settlement.application;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.settlement.application.dto.SettlementResult;
import com.hoya.aicommerce.settlement.domain.Settlement;
import com.hoya.aicommerce.settlement.domain.SettlementRepository;
import com.hoya.aicommerce.settlement.domain.SettlementStatus;
import com.hoya.aicommerce.settlement.exception.SettlementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private SettlementService settlementService;

    private static final Long SELLER_ID = 1L;

    @Test
    void 기존_정산이_없으면_새로_생성하고_누적한다() {
        given(settlementRepository.findBySellerIdAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .willReturn(Optional.empty());
        Settlement newSettlement = Settlement.create(SELLER_ID,
                LocalDate.now().withDayOfMonth(1),
                LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        given(settlementRepository.save(any(Settlement.class))).willReturn(newSettlement);

        settlementService.accumulate(SELLER_ID, Money.of(100_000L));

        then(settlementRepository).should().save(any(Settlement.class));
    }

    @Test
    void 기존_PENDING_정산이_있으면_기존_정산에_누적한다() {
        LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        Settlement existing = Settlement.create(SELLER_ID, periodStart, periodEnd);
        existing.addPayment(Money.of(50_000L), new java.math.BigDecimal("0.10"));
        given(settlementRepository.findBySellerIdAndPeriodStartAndPeriodEnd(SELLER_ID, periodStart, periodEnd))
                .willReturn(Optional.of(existing));

        settlementService.accumulate(SELLER_ID, Money.of(100_000L));

        assertThat(existing.getGrossAmount()).isEqualTo(Money.of(150_000L));
        then(settlementRepository).should(never()).save(any(Settlement.class));
    }

    @Test
    void sellerId가_null이면_누적하지_않는다() {
        settlementService.accumulate(null, Money.of(100_000L));

        then(settlementRepository).should(never()).findBySellerIdAndPeriodStartAndPeriodEnd(any(), any(), any());
    }

    @Test
    void 판매자_정산_목록_조회() {
        LocalDate periodStart = LocalDate.of(2026, 3, 1);
        LocalDate periodEnd = LocalDate.of(2026, 3, 31);
        Settlement s = Settlement.create(SELLER_ID, periodStart, periodEnd);
        ReflectionTestUtils.setField(s, "id", 1L);
        given(settlementRepository.findBySellerId(SELLER_ID)).willReturn(List.of(s));

        List<SettlementResult> results = settlementService.getSettlements(SELLER_ID);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).sellerId()).isEqualTo(SELLER_ID);
        assertThat(results.get(0).status()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    void 정산_상세_조회_성공() {
        LocalDate periodStart = LocalDate.of(2026, 3, 1);
        LocalDate periodEnd = LocalDate.of(2026, 3, 31);
        Settlement s = Settlement.create(SELLER_ID, periodStart, periodEnd);
        ReflectionTestUtils.setField(s, "id", 1L);
        given(settlementRepository.findById(1L)).willReturn(Optional.of(s));

        SettlementResult result = settlementService.getSettlement(1L, SELLER_ID);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sellerId()).isEqualTo(SELLER_ID);
    }

    @Test
    void 다른_판매자의_정산_조회_시_예외() {
        LocalDate periodStart = LocalDate.of(2026, 3, 1);
        LocalDate periodEnd = LocalDate.of(2026, 3, 31);
        Settlement s = Settlement.create(SELLER_ID, periodStart, periodEnd);
        ReflectionTestUtils.setField(s, "id", 1L);
        given(settlementRepository.findById(1L)).willReturn(Optional.of(s));

        assertThatThrownBy(() -> settlementService.getSettlement(1L, 999L))
                .isInstanceOf(SettlementException.class)
                .hasMessageContaining("본인의 정산 내역만 조회할 수 있습니다");
    }

    @Test
    void deductPayment_당월_PENDING_정산에서_차감된다() {
        LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        Settlement existing = Settlement.create(SELLER_ID, periodStart, periodEnd);
        existing.addPayment(Money.of(200_000L), new java.math.BigDecimal("0.10"));
        given(settlementRepository.findBySellerIdAndPeriodStartAndPeriodEnd(SELLER_ID, periodStart, periodEnd))
                .willReturn(Optional.of(existing));

        settlementService.deductPayment(SELLER_ID, Money.of(100_000L));

        assertThat(existing.getGrossAmount()).isEqualTo(Money.of(100_000L));
    }

    @Test
    void deductPayment_당월_정산_없으면_로그만_남기고_예외없음() {
        given(settlementRepository.findBySellerIdAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .willReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatCode(
                () -> settlementService.deductPayment(SELLER_ID, Money.of(50_000L)))
                .doesNotThrowAnyException();
    }

    @Test
    void deductPayment_COMPLETED_정산이면_로그만_남기고_예외없음() {
        LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        Settlement completed = Settlement.create(SELLER_ID, periodStart, periodEnd);
        completed.addPayment(Money.of(100_000L), new java.math.BigDecimal("0.10"));
        completed.complete();
        given(settlementRepository.findBySellerIdAndPeriodStartAndPeriodEnd(SELLER_ID, periodStart, periodEnd))
                .willReturn(Optional.of(completed));

        org.assertj.core.api.Assertions.assertThatCode(
                () -> settlementService.deductPayment(SELLER_ID, Money.of(50_000L)))
                .doesNotThrowAnyException();
    }

    @Test
    void 존재하지_않는_정산_조회_시_예외() {
        given(settlementRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.getSettlement(999L, SELLER_ID))
                .isInstanceOf(SettlementException.class)
                .hasMessageContaining("정산 내역을 찾을 수 없습니다");
    }
}

package com.hoya.aicommerce.payment.infrastructure.pg;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgCancelResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgConfirmResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgPrepareResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockPgGatewayTest {

    private final MockPgGateway mockPgGateway = new MockPgGateway();

    @Test
    void prepare_호출시_paymentKey가_발급된다() {
        PgPrepareResponse response = mockPgGateway.prepare(1L, Money.of(50000L), PaymentMethod.CARD);

        assertThat(response.paymentKey()).isNotNull();
        assertThat(response.paymentKey()).startsWith("mock-");
    }

    @Test
    void prepare_호출마다_다른_paymentKey가_발급된다() {
        PgPrepareResponse response1 = mockPgGateway.prepare(1L, Money.of(50000L), PaymentMethod.CARD);
        PgPrepareResponse response2 = mockPgGateway.prepare(1L, Money.of(50000L), PaymentMethod.CARD);

        assertThat(response1.paymentKey()).isNotEqualTo(response2.paymentKey());
    }

    @Test
    void 백만원_미만은_승인에_성공한다() {
        PgConfirmResponse response = mockPgGateway.confirm("mock-key", Money.of(999_999L));

        assertThat(response.success()).isTrue();
    }

    @Test
    void 백만원_이상은_한도초과로_실패한다() {
        PgConfirmResponse response = mockPgGateway.confirm("mock-key", Money.of(1_000_000L));

        assertThat(response.success()).isFalse();
        assertThat(response.failureCode()).isEqualTo("CARD_LIMIT_EXCEEDED");
        assertThat(response.failureMessage()).isEqualTo("카드 한도가 초과되었습니다");
    }

    @Test
    void 취소는_항상_성공한다() {
        PgCancelResponse response = mockPgGateway.cancel("mock-key", Money.of(50000L));

        assertThat(response.success()).isTrue();
    }
}

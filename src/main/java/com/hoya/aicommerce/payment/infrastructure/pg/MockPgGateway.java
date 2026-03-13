package com.hoya.aicommerce.payment.infrastructure.pg;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgCancelResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgConfirmResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgPrepareResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPgGateway implements PgGateway {

    private static final BigDecimal CARD_LIMIT = BigDecimal.valueOf(1_000_000);

    @Override
    public PgPrepareResponse prepare(Long orderId, Money amount, PaymentMethod method) {
        String paymentKey = "mock-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return new PgPrepareResponse(paymentKey);
    }

    @Override
    public PgConfirmResponse confirm(String paymentKey, Money amount) {
        if (amount.getValue().compareTo(CARD_LIMIT) >= 0) {
            return new PgConfirmResponse(false, "CARD_LIMIT_EXCEEDED", "카드 한도가 초과되었습니다");
        }
        return new PgConfirmResponse(true, null, null);
    }

    @Override
    public PgCancelResponse cancel(String paymentKey, Money amount) {
        return new PgCancelResponse(true, null, null);
    }
}

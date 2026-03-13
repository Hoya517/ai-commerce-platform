package com.hoya.aicommerce.payment.infrastructure.pg;

import com.hoya.aicommerce.common.domain.Money;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgCancelResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgConfirmResponse;
import com.hoya.aicommerce.payment.infrastructure.pg.dto.PgPrepareResponse;

public interface PgGateway {

    /** 결제 준비: PG가 paymentKey를 발급 */
    PgPrepareResponse prepare(Long orderId, Money amount, PaymentMethod method);

    /** 결제 승인: 서버가 저장한 paymentKey로 PG에 최종 승인 요청 */
    PgConfirmResponse confirm(String paymentKey, Money amount);

    /** 결제 취소: PG에 결제 취소 요청 */
    PgCancelResponse cancel(String paymentKey, Money amount);
}

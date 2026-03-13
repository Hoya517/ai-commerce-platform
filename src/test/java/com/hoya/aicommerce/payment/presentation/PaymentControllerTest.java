package com.hoya.aicommerce.payment.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.auth.JwtProvider;
import com.hoya.aicommerce.order.exception.OrderException;
import com.hoya.aicommerce.payment.application.PaymentService;
import com.hoya.aicommerce.payment.application.dto.PaymentResult;
import com.hoya.aicommerce.payment.domain.PaymentMethod;
import com.hoya.aicommerce.payment.domain.PaymentStatus;
import com.hoya.aicommerce.payment.exception.PaymentException;
import com.hoya.aicommerce.payment.presentation.request.ConfirmPaymentRequest;
import com.hoya.aicommerce.payment.presentation.request.FailPaymentRequest;
import com.hoya.aicommerce.payment.presentation.request.RequestPaymentRequest;
import com.hoya.aicommerce.payment.presentation.request.WalletPaymentRequest;
import com.hoya.aicommerce.wallet.exception.WalletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthContext authContext;

    private PaymentResult readyPaymentResult() {
        return new PaymentResult(1L, 1L, BigDecimal.valueOf(2000), PaymentMethod.CARD, PaymentStatus.READY, null);
    }

    private PaymentResult approvedPaymentResult() {
        return new PaymentResult(1L, 1L, BigDecimal.valueOf(2000), PaymentMethod.CARD, PaymentStatus.APPROVED, LocalDateTime.now());
    }

    private PaymentResult walletApprovedResult() {
        return new PaymentResult(1L, 1L, BigDecimal.valueOf(5000), PaymentMethod.WALLET, PaymentStatus.APPROVED, LocalDateTime.now());
    }

    private PaymentResult canceledPaymentResult() {
        return new PaymentResult(1L, 1L, BigDecimal.valueOf(2000), PaymentMethod.CARD, PaymentStatus.CANCELED, LocalDateTime.now());
    }

    @Test
    void 결제_요청_성공() throws Exception {
        given(paymentService.requestPayment(any())).willReturn(readyPaymentResult());

        RequestPaymentRequest request = new RequestPaymentRequest(1L, PaymentMethod.CARD);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.method").value("CARD"));
    }

    @Test
    void 결제_요청_유효성_실패() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 존재하지_않는_주문_결제_요청_실패() throws Exception {
        given(paymentService.requestPayment(any())).willThrow(new OrderException("Order not found"));

        RequestPaymentRequest request = new RequestPaymentRequest(99L, PaymentMethod.CARD);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void 결제_승인_성공() throws Exception {
        given(paymentService.confirmPayment(any())).willReturn(approvedPaymentResult());

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(1L);

        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void 결제_승인_유효성_실패() throws Exception {
        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 존재하지_않는_결제_승인_실패() throws Exception {
        given(paymentService.confirmPayment(any())).willThrow(new PaymentException("Payment not found"));

        ConfirmPaymentRequest request = new ConfirmPaymentRequest(99L);

        mockMvc.perform(post("/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Payment not found"));
    }

    @Test
    void 결제_실패_처리_성공() throws Exception {
        willDoNothing().given(paymentService).failPayment(any());

        FailPaymentRequest request = new FailPaymentRequest(1L, "CARD_DECLINED", "카드 한도 초과");

        mockMvc.perform(post("/payments/fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 예치금_결제_성공() throws Exception {
        given(authContext.getMemberId()).willReturn(1L);
        given(paymentService.payWithWallet(any())).willReturn(walletApprovedResult());

        WalletPaymentRequest request = new WalletPaymentRequest(1L);

        mockMvc.perform(post("/payments/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.method").value("WALLET"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void 결제_취소_성공() throws Exception {
        given(jwtProvider.validateToken("test-token")).willReturn(true);
        given(jwtProvider.getMemberId("test-token")).willReturn(1L);
        given(authContext.getMemberId()).willReturn(1L);
        given(paymentService.cancelPayment(eq(1L), eq(1L))).willReturn(canceledPaymentResult());

        mockMvc.perform(post("/payments/1/cancel")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    @Test
    void 결제_취소_인증_없으면_401() throws Exception {
        given(jwtProvider.validateToken(any())).willReturn(false);

        mockMvc.perform(post("/payments/1/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 잔액_부족_시_예치금_결제_실패() throws Exception {
        given(authContext.getMemberId()).willReturn(1L);
        given(paymentService.payWithWallet(any())).willThrow(new WalletException("잔액이 부족합니다"));

        WalletPaymentRequest request = new WalletPaymentRequest(1L);

        mockMvc.perform(post("/payments/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("잔액이 부족합니다"));
    }
}

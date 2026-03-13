package com.hoya.aicommerce.wallet.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.auth.JwtProvider;
import com.hoya.aicommerce.wallet.application.WalletService;
import com.hoya.aicommerce.wallet.application.dto.WalletResult;
import com.hoya.aicommerce.wallet.exception.WalletException;
import com.hoya.aicommerce.wallet.presentation.request.ChargeWalletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private WalletService walletService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthContext authContext;

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken("test-token")).willReturn(true);
        given(jwtProvider.getMemberId("test-token")).willReturn(1L);
        given(authContext.getMemberId()).willReturn(1L);
    }

    private WalletResult sampleResult() {
        return new WalletResult(1L, 1L, BigDecimal.ZERO, null);
    }

    @Test
    void 예치금_조회_성공() throws Exception {
        given(walletService.getWallet(1L)).willReturn(sampleResult());

        mockMvc.perform(get("/wallets/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(1))
                .andExpect(jsonPath("$.data.balance").value(0));
    }

    @Test
    void 지갑_없으면_조회_실패() throws Exception {
        given(walletService.getWallet(1L)).willThrow(new WalletException("지갑을 찾을 수 없습니다"));

        mockMvc.perform(get("/wallets/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("지갑을 찾을 수 없습니다"));
    }

    @Test
    void 예치금_충전_성공() throws Exception {
        WalletResult charged = new WalletResult(1L, 1L, BigDecimal.valueOf(10000), null);
        given(walletService.charge(eq(1L), any())).willReturn(charged);

        ChargeWalletRequest request = new ChargeWalletRequest(BigDecimal.valueOf(10000));

        mockMvc.perform(post("/wallets/charge")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(10000));
    }

    @Test
    void 충전_금액이_없으면_유효성_실패() throws Exception {
        mockMvc.perform(post("/wallets/charge")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

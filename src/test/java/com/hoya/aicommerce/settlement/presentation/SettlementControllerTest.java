package com.hoya.aicommerce.settlement.presentation;

import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.auth.JwtProvider;
import com.hoya.aicommerce.seller.application.SellerService;
import com.hoya.aicommerce.seller.exception.SellerException;
import com.hoya.aicommerce.settlement.application.SettlementService;
import com.hoya.aicommerce.settlement.application.dto.SettlementResult;
import com.hoya.aicommerce.settlement.domain.SettlementStatus;
import com.hoya.aicommerce.settlement.exception.SettlementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettlementController.class)
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettlementService settlementService;

    @MockitoBean
    private SellerService sellerService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AuthContext authContext;

    private static final Long MEMBER_ID = 1L;
    private static final Long SELLER_ID = 10L;

    @BeforeEach
    void setUp() {
        given(jwtProvider.validateToken("test-token")).willReturn(true);
        given(jwtProvider.getMemberId("test-token")).willReturn(MEMBER_ID);
        given(authContext.getMemberId()).willReturn(MEMBER_ID);
        given(sellerService.verifyApprovedSeller(MEMBER_ID)).willReturn(SELLER_ID);
    }

    private SettlementResult sampleResult(Long id) {
        return new SettlementResult(id, SELLER_ID,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                BigDecimal.valueOf(100_000), BigDecimal.valueOf(10_000), BigDecimal.valueOf(90_000),
                SettlementStatus.PENDING);
    }

    @Test
    void 정산_목록_조회_성공() throws Exception {
        given(settlementService.getSettlements(SELLER_ID)).willReturn(List.of(sampleResult(1L)));

        mockMvc.perform(get("/settlements/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sellerId").value(SELLER_ID))
                .andExpect(jsonPath("$.data[0].grossAmount").value(100000))
                .andExpect(jsonPath("$.data[0].feeAmount").value(10000))
                .andExpect(jsonPath("$.data[0].netAmount").value(90000))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void 정산_목록_조회_인증_없으면_401() throws Exception {
        mockMvc.perform(get("/settlements/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 정산_상세_조회_성공() throws Exception {
        given(settlementService.getSettlement(1L, SELLER_ID)).willReturn(sampleResult(1L));

        mockMvc.perform(get("/settlements/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.netAmount").value(90000));
    }

    @Test
    void 판매자_아니면_정산_조회_실패() throws Exception {
        given(sellerService.verifyApprovedSeller(MEMBER_ID))
                .willThrow(new SellerException("판매자 등록이 필요합니다"));

        mockMvc.perform(get("/settlements/me")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("판매자 등록이 필요합니다"));
    }

    @Test
    void 다른_판매자_정산_조회_시_실패() throws Exception {
        given(settlementService.getSettlement(1L, SELLER_ID))
                .willThrow(new SettlementException("본인의 정산 내역만 조회할 수 있습니다"));

        mockMvc.perform(get("/settlements/1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

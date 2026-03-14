package com.hoya.aicommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoya.aicommerce.member.application.MemberService;
import com.hoya.aicommerce.member.application.dto.LoginResult;
import com.hoya.aicommerce.member.application.dto.RegisterMemberCommand;
import com.hoya.aicommerce.seller.application.SellerService;
import com.hoya.aicommerce.seller.application.dto.RegisterSellerCommand;
import com.hoya.aicommerce.seller.application.dto.SellerResult;
import com.hoya.aicommerce.catalog.presentation.request.CreateProductRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SellerFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MemberService memberService;

    @Autowired
    private SellerService sellerService;

    private Long memberId;
    private String token;

    @BeforeEach
    void setUp() {
        memberService.registerMember(new RegisterMemberCommand("seller@example.com", "password123", "판매자"));
        LoginResult login = memberService.login(
                new com.hoya.aicommerce.member.application.dto.LoginMemberCommand("seller@example.com", "password123")
        );
        memberId = login.memberId();
        token = login.token();
    }

    @Test
    void 판매자_등록_후_승인_후_상품_등록_성공() throws Exception {
        // 판매자 등록
        RegisterSellerCommand sellerCommand = new RegisterSellerCommand(memberId, "테스트 사업자", "국민은행 123-456");
        SellerResult seller = sellerService.registerSeller(sellerCommand);

        // 판매자 승인
        sellerService.approveSeller(seller.id());

        // 상품 등록
        CreateProductRequest productRequest = new CreateProductRequest("테스트 상품", "상품 설명", BigDecimal.valueOf(10000), 100);

        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.price").value(10000));
    }

    @Test
    void 미승인_판매자_상품_등록_400() throws Exception {
        // 판매자 등록만 (승인 없음)
        RegisterSellerCommand sellerCommand = new RegisterSellerCommand(memberId, "테스트 사업자", "국민은행 123-456");
        sellerService.registerSeller(sellerCommand);

        CreateProductRequest productRequest = new CreateProductRequest("테스트 상품", "상품 설명", BigDecimal.valueOf(10000), 100);

        mockMvc.perform(post("/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

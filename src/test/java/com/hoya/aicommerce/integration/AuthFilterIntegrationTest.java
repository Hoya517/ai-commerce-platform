package com.hoya.aicommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoya.aicommerce.common.auth.JwtProvider;
import com.hoya.aicommerce.member.application.MemberService;
import com.hoya.aicommerce.member.application.dto.LoginResult;
import com.hoya.aicommerce.member.application.dto.RegisterMemberCommand;
import com.hoya.aicommerce.member.presentation.request.LoginRequest;
import com.hoya.aicommerce.member.presentation.request.RegisterMemberRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MemberService memberService;

    @Autowired
    private JwtProvider jwtProvider;

    private String registerAndLogin(String email) throws Exception {
        memberService.registerMember(new RegisterMemberCommand(email, "password123", "홍길동"));
        LoginResult loginResult = memberService.login(
                new com.hoya.aicommerce.member.application.dto.LoginMemberCommand(email, "password123")
        );
        return loginResult.token();
    }

    @Test
    void JWT_없이_보호_API_접근하면_401() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 유효한_JWT로_보호_API_접근하면_통과() throws Exception {
        String token = registerAndLogin("auth-test@example.com");

        // 인증 통과 확인: 401이 아닌 응답(카트 없으면 400)이면 필터를 통과한 것
        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 위조된_토큰으로_보호_API_접근하면_401() throws Exception {
        mockMvc.perform(get("/cart")
                        .header("Authorization", "Bearer forged.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 보호되지_않은_API는_인증_없이_접근_가능() throws Exception {
        mockMvc.perform(get("/members/999"))
                .andExpect(status().isBadRequest());
    }
}

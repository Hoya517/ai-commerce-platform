package com.hoya.aicommerce.common.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(
                "test-secret-key-for-jwt-authentication-hs256",
                86400000L
        );
    }

    @Test
    void 토큰을_생성하고_memberId를_추출한다() {
        String token = jwtProvider.generateToken(1L);

        assertThat(token).isNotBlank();
        assertThat(jwtProvider.getMemberId(token)).isEqualTo(1L);
    }

    @Test
    void 유효한_토큰은_검증에_성공한다() {
        String token = jwtProvider.generateToken(1L);

        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void 잘못된_토큰은_검증에_실패한다() {
        assertThat(jwtProvider.validateToken("invalid.token.value")).isFalse();
    }

    @Test
    void 빈_문자열은_검증에_실패한다() {
        assertThat(jwtProvider.validateToken("")).isFalse();
    }
}

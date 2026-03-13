package com.hoya.aicommerce.member.presentation.response;

import com.hoya.aicommerce.member.application.dto.LoginResult;

public record LoginResponse(
        String token,
        Long memberId,
        String email,
        String name
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.token(), result.memberId(), result.email(), result.name());
    }
}

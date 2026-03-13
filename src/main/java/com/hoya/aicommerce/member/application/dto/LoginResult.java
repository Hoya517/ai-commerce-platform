package com.hoya.aicommerce.member.application.dto;

public record LoginResult(
        String token,
        Long memberId,
        String email,
        String name
) {}

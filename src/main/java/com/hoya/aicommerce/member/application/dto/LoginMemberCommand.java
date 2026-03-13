package com.hoya.aicommerce.member.application.dto;

public record LoginMemberCommand(
        String email,
        String password
) {}

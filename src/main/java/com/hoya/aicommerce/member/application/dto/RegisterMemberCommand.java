package com.hoya.aicommerce.member.application.dto;

public record RegisterMemberCommand(
        String email,
        String password,
        String name
) {}

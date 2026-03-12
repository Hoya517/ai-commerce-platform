package com.hoya.aicommerce.member.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterMemberRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name
) {}

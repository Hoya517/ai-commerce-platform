package com.hoya.aicommerce.member.presentation.response;

import com.hoya.aicommerce.member.application.dto.MemberResult;
import com.hoya.aicommerce.member.domain.MemberRole;
import com.hoya.aicommerce.member.domain.MemberStatus;

public record MemberResponse(
        Long id,
        String email,
        String name,
        MemberRole role,
        MemberStatus status
) {
    public static MemberResponse from(MemberResult result) {
        return new MemberResponse(
                result.id(),
                result.email(),
                result.name(),
                result.role(),
                result.status()
        );
    }
}

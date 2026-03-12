package com.hoya.aicommerce.member.application.dto;

import com.hoya.aicommerce.member.domain.Member;
import com.hoya.aicommerce.member.domain.MemberRole;
import com.hoya.aicommerce.member.domain.MemberStatus;

public record MemberResult(
        Long id,
        String email,
        String name,
        MemberRole role,
        MemberStatus status
) {
    public static MemberResult from(Member member) {
        return new MemberResult(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.getStatus()
        );
    }
}

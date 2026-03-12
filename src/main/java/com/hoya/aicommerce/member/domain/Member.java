package com.hoya.aicommerce.member.domain;

import com.hoya.aicommerce.member.exception.MemberException;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String name;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private Member(String email, String password, String name) {
        if (email == null || email.isBlank()) {
            throw new MemberException("Email must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new MemberException("Password must not be blank");
        }
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = MemberRole.MEMBER;
        this.status = MemberStatus.ACTIVE;
    }

    public static Member create(String email, String password, String name) {
        return new Member(email, password, name);
    }
}

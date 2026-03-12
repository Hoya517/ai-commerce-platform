package com.hoya.aicommerce.member.domain;

import com.hoya.aicommerce.member.exception.MemberException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    void 정상_생성된다() {
        Member member = Member.create("test@example.com", "password", "홍길동");

        assertThat(member.getEmail()).isEqualTo("test@example.com");
        assertThat(member.getName()).isEqualTo("홍길동");
        assertThat(member.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void 이메일이_빈값이면_예외가_발생한다() {
        assertThatThrownBy(() -> Member.create("", "password", "홍길동"))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void 이메일이_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> Member.create(null, "password", "홍길동"))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void 비밀번호가_빈값이면_예외가_발생한다() {
        assertThatThrownBy(() -> Member.create("test@example.com", "", "홍길동"))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void 비밀번호가_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> Member.create("test@example.com", null, "홍길동"))
                .isInstanceOf(MemberException.class);
    }
}

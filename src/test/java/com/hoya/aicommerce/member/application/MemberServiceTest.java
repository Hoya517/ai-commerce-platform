package com.hoya.aicommerce.member.application;

import com.hoya.aicommerce.member.application.dto.MemberResult;
import com.hoya.aicommerce.member.application.dto.RegisterMemberCommand;
import com.hoya.aicommerce.member.domain.Member;
import com.hoya.aicommerce.member.domain.MemberRepository;
import com.hoya.aicommerce.member.domain.MemberRole;
import com.hoya.aicommerce.member.domain.MemberStatus;
import com.hoya.aicommerce.member.exception.MemberException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 회원이_정상_등록된다() {
        Member member = Member.create("test@example.com", "password", "홍길동");
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.empty());
        given(memberRepository.save(any())).willReturn(member);

        RegisterMemberCommand command = new RegisterMemberCommand("test@example.com", "password", "홍길동");
        MemberResult result = memberService.registerMember(command);

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.role()).isEqualTo(MemberRole.MEMBER);
        assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
        verify(memberRepository).save(any());
    }

    @Test
    void 이미_사용중인_이메일로_가입하면_예외가_발생한다() {
        Member existing = Member.create("test@example.com", "password", "기존회원");
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(existing));

        RegisterMemberCommand command = new RegisterMemberCommand("test@example.com", "password", "새회원");
        assertThatThrownBy(() -> memberService.registerMember(command))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void 회원을_조회한다() {
        Member member = Member.create("test@example.com", "password", "홍길동");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        MemberResult result = memberService.getMember(1L);

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("홍길동");
    }

    @Test
    void 존재하지_않는_회원_조회시_예외가_발생한다() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(99L))
                .isInstanceOf(MemberException.class);
    }
}

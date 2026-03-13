package com.hoya.aicommerce.member.application;

import com.hoya.aicommerce.common.auth.JwtProvider;
import com.hoya.aicommerce.member.application.dto.LoginMemberCommand;
import com.hoya.aicommerce.member.application.dto.LoginResult;
import com.hoya.aicommerce.member.application.dto.MemberResult;
import com.hoya.aicommerce.member.application.dto.RegisterMemberCommand;
import com.hoya.aicommerce.member.domain.Member;
import com.hoya.aicommerce.member.domain.MemberRepository;
import com.hoya.aicommerce.member.domain.MemberRole;
import com.hoya.aicommerce.member.domain.MemberStatus;
import com.hoya.aicommerce.member.exception.MemberException;
import com.hoya.aicommerce.wallet.application.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private MemberService memberService;

    @Test
    void 회원이_정상_등록된다() {
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password")).willReturn("encoded-password");
        Member member = Member.create("test@example.com", "encoded-password", "홍길동");
        given(memberRepository.save(any())).willReturn(member);

        RegisterMemberCommand command = new RegisterMemberCommand("test@example.com", "password", "홍길동");
        MemberResult result = memberService.registerMember(command);

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.role()).isEqualTo(MemberRole.MEMBER);
        assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
        verify(passwordEncoder).encode("password");
        verify(memberRepository).save(any());
    }

    @Test
    void 이미_사용중인_이메일로_가입하면_예외가_발생한다() {
        Member existing = Member.create("test@example.com", "encoded", "기존회원");
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(existing));

        RegisterMemberCommand command = new RegisterMemberCommand("test@example.com", "password", "새회원");
        assertThatThrownBy(() -> memberService.registerMember(command))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void 회원을_조회한다() {
        Member member = Member.create("test@example.com", "encoded", "홍길동");
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

    @Test
    void 로그인에_성공하고_토큰을_반환한다() {
        Member member = Member.create("test@example.com", "encoded-password", "홍길동");
        ReflectionTestUtils.setField(member, "id", 1L);
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("password", "encoded-password")).willReturn(true);
        given(jwtProvider.generateToken(1L)).willReturn("jwt-token");

        LoginMemberCommand command = new LoginMemberCommand("test@example.com", "password");
        LoginResult result = memberService.login(command);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("test@example.com");
    }

    @Test
    void 존재하지_않는_이메일로_로그인하면_예외가_발생한다() {
        given(memberRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(new LoginMemberCommand("none@example.com", "password")))
                .isInstanceOf(MemberException.class);
    }

    @Test
    void 비밀번호가_틀리면_예외가_발생한다() {
        Member member = Member.create("test@example.com", "encoded-password", "홍길동");
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> memberService.login(new LoginMemberCommand("test@example.com", "wrong")))
                .isInstanceOf(MemberException.class);
    }
}

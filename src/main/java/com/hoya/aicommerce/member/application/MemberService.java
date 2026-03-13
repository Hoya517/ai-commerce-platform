package com.hoya.aicommerce.member.application;

import com.hoya.aicommerce.common.auth.JwtProvider;
import com.hoya.aicommerce.member.application.dto.LoginMemberCommand;
import com.hoya.aicommerce.member.application.dto.LoginResult;
import com.hoya.aicommerce.member.application.dto.MemberResult;
import com.hoya.aicommerce.member.application.dto.RegisterMemberCommand;
import com.hoya.aicommerce.member.domain.Member;
import com.hoya.aicommerce.member.domain.MemberRepository;
import com.hoya.aicommerce.member.exception.MemberException;
import com.hoya.aicommerce.wallet.application.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final WalletService walletService;

    @Transactional
    public MemberResult registerMember(RegisterMemberCommand command) {
        if (memberRepository.findByEmail(command.email()).isPresent()) {
            throw new MemberException("Email already in use");
        }
        Member member = Member.create(command.email(), passwordEncoder.encode(command.password()), command.name());
        MemberResult result = MemberResult.from(memberRepository.save(member));
        walletService.createWallet(result.id());
        return result;
    }

    @Transactional(readOnly = true)
    public MemberResult getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("Member not found"));
        return MemberResult.from(member);
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginMemberCommand command) {
        Member member = memberRepository.findByEmail(command.email())
                .orElseThrow(() -> new MemberException("이메일 또는 비밀번호가 올바르지 않습니다"));
        if (!passwordEncoder.matches(command.password(), member.getPassword())) {
            throw new MemberException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        String token = jwtProvider.generateToken(member.getId());
        return new LoginResult(token, member.getId(), member.getEmail(), member.getName());
    }
}

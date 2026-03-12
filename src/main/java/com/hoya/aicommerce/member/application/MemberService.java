package com.hoya.aicommerce.member.application;

import com.hoya.aicommerce.member.application.dto.MemberResult;
import com.hoya.aicommerce.member.application.dto.RegisterMemberCommand;
import com.hoya.aicommerce.member.domain.Member;
import com.hoya.aicommerce.member.domain.MemberRepository;
import com.hoya.aicommerce.member.exception.MemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public MemberResult registerMember(RegisterMemberCommand command) {
        if (memberRepository.findByEmail(command.email()).isPresent()) {
            throw new MemberException("Email already in use");
        }
        Member member = Member.create(command.email(), command.password(), command.name());
        return MemberResult.from(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public MemberResult getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("Member not found"));
        return MemberResult.from(member);
    }
}

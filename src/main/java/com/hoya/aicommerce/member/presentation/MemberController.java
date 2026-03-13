package com.hoya.aicommerce.member.presentation;

import com.hoya.aicommerce.common.presentation.ApiResponse;
import com.hoya.aicommerce.member.application.MemberService;
import com.hoya.aicommerce.member.application.dto.LoginMemberCommand;
import com.hoya.aicommerce.member.application.dto.RegisterMemberCommand;
import com.hoya.aicommerce.member.presentation.request.LoginRequest;
import com.hoya.aicommerce.member.presentation.request.RegisterMemberRequest;
import com.hoya.aicommerce.member.presentation.response.LoginResponse;
import com.hoya.aicommerce.member.presentation.response.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 가입")
    @PostMapping
    public ApiResponse<MemberResponse> registerMember(@Valid @RequestBody RegisterMemberRequest request) {
        RegisterMemberCommand command = new RegisterMemberCommand(
                request.email(), request.password(), request.name()
        );
        return ApiResponse.success(MemberResponse.from(memberService.registerMember(command)));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginMemberCommand command = new LoginMemberCommand(request.email(), request.password());
        return ApiResponse.success(LoginResponse.from(memberService.login(command)));
    }

    @Operation(summary = "회원 단건 조회")
    @GetMapping("/{memberId}")
    public ApiResponse<MemberResponse> getMember(@PathVariable Long memberId) {
        return ApiResponse.success(MemberResponse.from(memberService.getMember(memberId)));
    }
}

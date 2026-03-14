package com.hoya.aicommerce.wallet.presentation;

import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.auth.RequiresAuth;
import com.hoya.aicommerce.common.presentation.ApiResponse;
import com.hoya.aicommerce.wallet.application.WalletService;
import com.hoya.aicommerce.wallet.application.dto.ChargeWalletCommand;
import com.hoya.aicommerce.wallet.presentation.request.ChargeWalletRequest;
import com.hoya.aicommerce.wallet.presentation.response.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wallet", description = "예치금 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/wallets")
@RequiresAuth
public class WalletController {

    private final WalletService walletService;
    private final AuthContext authContext;

    @Operation(summary = "예치금 조회")
    @GetMapping("/me")
    public ApiResponse<WalletResponse> getWallet() {
        return ApiResponse.success(WalletResponse.from(walletService.getWallet(authContext.getMemberId())));
    }

    @Operation(summary = "예치금 충전")
    @PostMapping("/charge")
    public ApiResponse<WalletResponse> charge(@Valid @RequestBody ChargeWalletRequest request) {
        ChargeWalletCommand command = new ChargeWalletCommand(request.amount());
        return ApiResponse.success(WalletResponse.from(walletService.charge(authContext.getMemberId(), command)));
    }
}

package com.hoya.aicommerce.seller.presentation;

import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.presentation.ApiResponse;
import com.hoya.aicommerce.seller.application.SellerService;
import com.hoya.aicommerce.seller.application.dto.RegisterSellerCommand;
import com.hoya.aicommerce.seller.presentation.request.RegisterSellerRequest;
import com.hoya.aicommerce.seller.presentation.response.SellerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Seller", description = "판매자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers")
public class SellerController {

    private final SellerService sellerService;
    private final AuthContext authContext;

    @Operation(summary = "판매자 등록")
    @PostMapping
    public ApiResponse<SellerResponse> registerSeller(@Valid @RequestBody RegisterSellerRequest request) {
        RegisterSellerCommand command = new RegisterSellerCommand(
                authContext.getMemberId(), request.businessName(), request.settlementAccount()
        );
        return ApiResponse.success(SellerResponse.from(sellerService.registerSeller(command)));
    }

    @Operation(summary = "판매자 단건 조회")
    @GetMapping("/{sellerId}")
    public ApiResponse<SellerResponse> getSeller(@PathVariable Long sellerId) {
        return ApiResponse.success(SellerResponse.from(sellerService.getSeller(sellerId)));
    }

    @Operation(summary = "판매자 승인 (관리자)")
    @PatchMapping("/{sellerId}/approve")
    public ApiResponse<SellerResponse> approveSeller(@PathVariable Long sellerId) {
        return ApiResponse.success(SellerResponse.from(sellerService.approveSeller(sellerId)));
    }
}

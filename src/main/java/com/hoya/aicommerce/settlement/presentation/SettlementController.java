package com.hoya.aicommerce.settlement.presentation;

import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.presentation.ApiResponse;
import com.hoya.aicommerce.seller.application.SellerService;
import com.hoya.aicommerce.settlement.application.SettlementService;
import com.hoya.aicommerce.settlement.presentation.response.SettlementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Settlement", description = "정산 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final SellerService sellerService;
    private final AuthContext authContext;

    @Operation(summary = "내 정산 목록 조회 (판매자 전용)")
    @GetMapping("/me")
    public ApiResponse<List<SettlementResponse>> getMySettlements() {
        Long sellerId = sellerService.verifyApprovedSeller(authContext.getMemberId());
        List<SettlementResponse> responses = settlementService.getSettlements(sellerId).stream()
                .map(SettlementResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @Operation(summary = "정산 상세 조회 (판매자 전용)")
    @GetMapping("/{id}")
    public ApiResponse<SettlementResponse> getSettlement(@PathVariable Long id) {
        Long sellerId = sellerService.verifyApprovedSeller(authContext.getMemberId());
        return ApiResponse.success(
                SettlementResponse.from(settlementService.getSettlement(id, sellerId)));
    }
}

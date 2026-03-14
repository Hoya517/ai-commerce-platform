package com.hoya.aicommerce.settlement.presentation;

import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.common.auth.RequiresAuth;
import com.hoya.aicommerce.common.presentation.ApiResponse;
import com.hoya.aicommerce.seller.application.SellerService;
import com.hoya.aicommerce.settlement.application.SettlementService;
import com.hoya.aicommerce.settlement.batch.dto.SettlementBatchResult;
import com.hoya.aicommerce.settlement.exception.SettlementException;
import com.hoya.aicommerce.settlement.presentation.request.SettlementBatchRequest;
import com.hoya.aicommerce.settlement.presentation.response.SettlementResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Settlement", description = "정산 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/settlements")
@RequiresAuth
public class SettlementController {

    private final SettlementService settlementService;
    private final SellerService sellerService;
    private final AuthContext authContext;
    private final JobLauncher jobLauncher;
    private final Job settlementJob;

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

    @Operation(summary = "정산 배치 수동 실행")
    @PostMapping("/batch")
    public ApiResponse<SettlementBatchResult> runBatch(
            @RequestBody(required = false) SettlementBatchRequest request) {
        LocalDate targetDate = (request != null && request.targetDate() != null)
                ? request.targetDate()
                : LocalDate.now().minusMonths(1)
                        .withDayOfMonth(LocalDate.now().minusMonths(1).lengthOfMonth());
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("targetDate", targetDate.toString())
                    .addString("runTime", LocalDateTime.now().toString())
                    .toJobParameters();
            JobExecution execution = jobLauncher.run(settlementJob, params);
            int count = execution.getStepExecutions().stream()
                    .mapToInt(s -> (int) s.getWriteCount())
                    .sum();
            return ApiResponse.success(new SettlementBatchResult(execution.getId(), count));
        } catch (Exception e) {
            throw new SettlementException("배치 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

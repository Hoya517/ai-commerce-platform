package com.hoya.aicommerce.payment.presentation;

import com.hoya.aicommerce.common.auth.AuthContext;
import com.hoya.aicommerce.payment.application.PaymentService;
import com.hoya.aicommerce.payment.application.dto.ConfirmPaymentCommand;
import com.hoya.aicommerce.payment.application.dto.FailPaymentCommand;
import com.hoya.aicommerce.payment.application.dto.PayWithWalletCommand;
import com.hoya.aicommerce.payment.application.dto.RequestPaymentCommand;
import com.hoya.aicommerce.common.presentation.ApiResponse;
import com.hoya.aicommerce.payment.presentation.request.ConfirmPaymentRequest;
import com.hoya.aicommerce.payment.presentation.request.FailPaymentRequest;
import com.hoya.aicommerce.payment.presentation.request.RequestPaymentRequest;
import com.hoya.aicommerce.payment.presentation.request.WalletPaymentRequest;
import com.hoya.aicommerce.payment.presentation.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthContext authContext;

    @Operation(summary = "결제 요청")
    @PostMapping
    public ApiResponse<PaymentResponse> requestPayment(@Valid @RequestBody RequestPaymentRequest request) {
        RequestPaymentCommand command = new RequestPaymentCommand(request.orderId(), request.method());
        return ApiResponse.success(PaymentResponse.from(paymentService.requestPayment(command)));
    }

    @Operation(summary = "결제 승인")
    @PostMapping("/confirm")
    public ApiResponse<PaymentResponse> confirmPayment(@Valid @RequestBody ConfirmPaymentRequest request) {
        ConfirmPaymentCommand command = new ConfirmPaymentCommand(request.paymentId());
        return ApiResponse.success(PaymentResponse.from(paymentService.confirmPayment(command)));
    }

    @Operation(summary = "예치금 결제")
    @PostMapping("/wallet")
    public ApiResponse<PaymentResponse> payWithWallet(@Valid @RequestBody WalletPaymentRequest request) {
        Long memberId = authContext.getMemberId();
        PayWithWalletCommand command = new PayWithWalletCommand(request.orderId(), memberId);
        return ApiResponse.success(PaymentResponse.from(paymentService.payWithWallet(command)));
    }

    @Operation(summary = "결제 취소/환불")
    @PostMapping("/{paymentId}/cancel")
    public ApiResponse<PaymentResponse> cancelPayment(@PathVariable Long paymentId) {
        Long memberId = authContext.getMemberId();
        return ApiResponse.success(PaymentResponse.from(paymentService.cancelPayment(paymentId, memberId)));
    }

    @Operation(summary = "결제 실패")
    @PostMapping("/fail")
    public ApiResponse<Void> failPayment(@Valid @RequestBody FailPaymentRequest request) {
        FailPaymentCommand command = new FailPaymentCommand(
                request.paymentId(), request.failureCode(), request.failureMessage()
        );
        paymentService.failPayment(command);
        return ApiResponse.success(null);
    }
}

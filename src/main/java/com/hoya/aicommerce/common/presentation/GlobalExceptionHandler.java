package com.hoya.aicommerce.common.presentation;

import com.hoya.aicommerce.cart.exception.CartException;
import com.hoya.aicommerce.catalog.exception.ProductException;
import com.hoya.aicommerce.common.auth.AuthException;
import com.hoya.aicommerce.member.exception.MemberException;
import com.hoya.aicommerce.order.exception.OrderException;
import com.hoya.aicommerce.payment.exception.PaymentException;
import com.hoya.aicommerce.seller.exception.SellerException;
import com.hoya.aicommerce.settlement.exception.SettlementException;
import com.hoya.aicommerce.wallet.exception.WalletException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler({ProductException.class, CartException.class, OrderException.class, PaymentException.class, MemberException.class, SellerException.class, WalletException.class, SettlementException.class})
    public ResponseEntity<ApiResponse<Void>> handleDomainException(RuntimeException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
    }
}

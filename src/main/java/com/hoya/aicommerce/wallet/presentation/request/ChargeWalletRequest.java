package com.hoya.aicommerce.wallet.presentation.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ChargeWalletRequest(
        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount
) {}

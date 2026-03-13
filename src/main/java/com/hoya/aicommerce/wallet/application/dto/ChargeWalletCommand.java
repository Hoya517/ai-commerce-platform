package com.hoya.aicommerce.wallet.application.dto;

import java.math.BigDecimal;

public record ChargeWalletCommand(
        BigDecimal amount
) {}

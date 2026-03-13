package com.hoya.aicommerce.seller.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterSellerRequest(
        @NotBlank String businessName,
        @NotBlank String settlementAccount
) {}

package com.hoya.aicommerce.catalog.presentation.request;

import com.hoya.aicommerce.catalog.domain.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeProductStatusRequest(
        @NotNull ProductStatus status
) {}

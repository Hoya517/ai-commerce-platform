package com.hoya.aicommerce.order.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull Long memberId,
        @NotEmpty @Valid List<OrderItemRequest> items
) {}

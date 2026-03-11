package com.hoya.aicommerce.order.application.dto;

import java.util.List;

public record CreateOrderCommand(
        Long memberId,
        List<OrderItemCommand> items
) {}

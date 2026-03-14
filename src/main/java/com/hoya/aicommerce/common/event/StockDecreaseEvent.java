package com.hoya.aicommerce.common.event;

public record StockDecreaseEvent(Long productId, int quantity, Long orderId) {}

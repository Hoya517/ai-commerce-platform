package com.hoya.aicommerce.common.kafka.message;

import com.hoya.aicommerce.common.event.StockDecreaseEvent;

public record StockDecreaseEventMessage(
        String eventId,
        Long productId,
        int quantity,
        Long orderId
) {
    public static StockDecreaseEventMessage from(StockDecreaseEvent event) {
        return new StockDecreaseEventMessage(
                "stock-decrease-" + event.productId() + "-" + event.orderId(),
                event.productId(),
                event.quantity(),
                event.orderId()
        );
    }
}

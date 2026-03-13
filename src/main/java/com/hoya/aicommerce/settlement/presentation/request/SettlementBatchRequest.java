package com.hoya.aicommerce.settlement.presentation.request;

import java.time.LocalDate;

public record SettlementBatchRequest(LocalDate targetDate) {
}

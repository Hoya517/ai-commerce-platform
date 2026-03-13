package com.hoya.aicommerce.payment.infrastructure.pg.dto;

public record PgCancelResponse(boolean success, String failureCode, String failureMessage) {}

package com.hoya.aicommerce.payment.infrastructure.pg.dto;

public record PgConfirmResponse(boolean success, String failureCode, String failureMessage) {}

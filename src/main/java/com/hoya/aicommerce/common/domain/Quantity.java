package com.hoya.aicommerce.common.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Quantity {

    private final int value;

    private Quantity(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        this.value = value;
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity increase(int amount) {
        return new Quantity(this.value + amount);
    }

    public Quantity decrease(int amount) {
        return new Quantity(this.value - amount);
    }
}

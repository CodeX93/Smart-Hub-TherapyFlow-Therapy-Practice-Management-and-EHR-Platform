package com.smart.therapy.flow.billing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Discount types for billing records
 */
public enum DiscountType {
    NONE("none", "No Discount"),
    PERCENTAGE("percentage", "Percentage Discount"),
    FIXED_AMOUNT("fixed", "Fixed Amount Discount");

    private final String value;
    private final String displayName;

    DiscountType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static DiscountType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(DiscountType.values())
                .filter(type -> type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid discount type: " + value));
    }

    public boolean hasDiscount() {
        return this != NONE;
    }
}

package com.smart.therapy.flow.billing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Payment status for billing records
 */
public enum PaymentStatus {
    PENDING("pending", "Payment pending"),
    PAID("paid", "Payment received"),
    FAILED("failed", "Payment failed"),
    REFUNDED("refunded", "Payment refunded"),
    PARTIAL("partial", "Partial payment received");

    private final String value;
    private final String displayName;

    PaymentStatus(String value, String displayName) {
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
    public static PaymentStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(PaymentStatus.values())
                .filter(status -> status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment status: " + value));
    }

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isOutstanding() {
        return this == PENDING || this == PARTIAL;
    }
}

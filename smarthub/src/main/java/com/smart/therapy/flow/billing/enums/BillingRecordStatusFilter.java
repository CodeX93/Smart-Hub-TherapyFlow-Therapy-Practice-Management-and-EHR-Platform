package com.smart.therapy.flow.billing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Unified status filter for billing records list UI.
 * Matches tenant-admin billing dropdown values.
 */
public enum BillingRecordStatusFilter {
    PENDING("pending"),
    PARTIAL("partial"),
    PAID("paid"),
    DENIED("denied"),
    REFUNDED("refunded");

    private final String value;

    BillingRecordStatusFilter(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static BillingRecordStatusFilter fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(BillingRecordStatusFilter.values())
                .filter(v -> v.value.equalsIgnoreCase(value) || v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid billing record status filter: " + value));
    }
}


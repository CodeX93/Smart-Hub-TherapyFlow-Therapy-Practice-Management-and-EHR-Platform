package com.smart.therapy.flow.billing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Billing status for tracking billing workflow
 */
public enum BillingStatus {
    PENDING("pending", "Pending - Not yet billed"),
    BILLED("billed", "Billed - Invoice sent"),
    PARTIAL("partial", "Partial - Partially paid"),
    PAID("paid", "Paid - Payment received"),
    DENIED("denied", "Denied - Payment/claim denied"),
    FOLLOW_UP("follow_up", "Follow Up Required"),
    CANCELLED("cancelled", "Cancelled");

    private final String value;
    private final String displayName;

    BillingStatus(String value, String displayName) {
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
    public static BillingStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(BillingStatus.values())
                .filter(status -> status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid billing status: " + value));
    }

    public boolean isFinal() {
        return this == PAID || this == DENIED || this == CANCELLED;
    }

    public boolean requiresAction() {
        return this == FOLLOW_UP || this == DENIED;
    }
}

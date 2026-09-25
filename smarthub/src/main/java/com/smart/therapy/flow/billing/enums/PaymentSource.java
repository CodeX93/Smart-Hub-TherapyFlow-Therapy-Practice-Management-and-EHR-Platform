package com.smart.therapy.flow.billing.enums;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentSource {
    STRIPE("stripe", "Stripe"),
    MANUAL("manual", "Manual"),
    INSURANCE_PORTAL("insurance_portal", "Insurance Portal"),
    CREDIT_BALANCE_TRANSFER("credit_balance_transfer", "Credit Balance Transfer");

    private final String value;
    private final String displayName;

    PaymentSource(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }      @JsonCreator
    public static PaymentSource fromValue(String value) {
        if (value == null) return null;
        return Arrays.stream(PaymentSource.values())
                .filter(source -> source.value.equalsIgnoreCase(value) || source.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment source: " + value));
    }

    public static PaymentSource fromDisplayName(String displayName) {
        if (displayName == null) return null;
        return Arrays.stream(PaymentSource.values())
                .filter(source -> source.displayName.equalsIgnoreCase(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment source: " + displayName));
    }
}

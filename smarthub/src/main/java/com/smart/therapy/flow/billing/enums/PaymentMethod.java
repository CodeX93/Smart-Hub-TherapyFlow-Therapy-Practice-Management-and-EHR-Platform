package com.smart.therapy.flow.billing.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Payment methods for billing records
 */
public enum PaymentMethod {
    CASH("cash", "Cash"),
    CHECK("check", "Check"),
    CREDIT_CARD("credit_card", "Credit Card"),
    DEBIT_CARD("debit_card", "Debit Card"),
    INSURANCE("insurance", "Insurance"),
    BANK_TRANSFER("bank_transfer", "Bank Transfer"),
    ONLINE_PAYMENT("online_payment", "Online Payment"),
    CREDIT_BALANCE("credit_balance", "Credit Balance");

    private final String value;
    private final String displayName;

    PaymentMethod(String value, String displayName) {
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
    public static PaymentMethod fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(PaymentMethod.values())
                .filter(method -> method.value.equalsIgnoreCase(value) || method.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment method: " + value));
    }

    public boolean isElectronic() {
        return this == CREDIT_CARD || this == DEBIT_CARD || this == BANK_TRANSFER || this == ONLINE_PAYMENT;
    }

    public boolean requiresReference() {
        return this != CASH;
    }
}

package com.smart.therapy.flow.billing.enums;

public enum TransactionType {
    CHARGE("charge", "Charge"),
    REFUND("refund", "Refund"),
    ADJUSTMENT("adjustment", "Adjustment");

    private final String value;
    private final String displayName;

    TransactionType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;   
    }

    public String getDisplayName() {
        return displayName;
    }
}

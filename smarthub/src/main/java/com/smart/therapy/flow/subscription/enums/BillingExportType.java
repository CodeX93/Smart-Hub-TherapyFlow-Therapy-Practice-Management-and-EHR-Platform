package com.smart.therapy.flow.subscription.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum BillingExportType {
    BILLING_INVOICES_CSV,
    REVENUE_ANALYTICS_CSV;

    @JsonCreator
    public static BillingExportType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return BillingExportType.valueOf(normalized);
    }
}

package com.smart.therapy.flow.subscription.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum AddonBillingCycle {
    MONTHLY,
    ANNUAL,
    ONE_TIME;

    @JsonCreator
    public static AddonBillingCycle fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return AddonBillingCycle.valueOf(normalized);
    }
}

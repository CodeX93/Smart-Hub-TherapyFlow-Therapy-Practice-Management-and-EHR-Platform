package com.smart.therapy.flow.subscription.enums;

import java.util.Locale;

public enum SubscriptionPlanStatus {
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    public static SubscriptionPlanStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return SubscriptionPlanStatus.valueOf(normalized);
    }
}

package com.smart.therapy.flow.subscription.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum AddonCatalogStatus {
    ACTIVE,
    INACTIVE;

    @JsonCreator
    public static AddonCatalogStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return AddonCatalogStatus.valueOf(normalized);
    }
}

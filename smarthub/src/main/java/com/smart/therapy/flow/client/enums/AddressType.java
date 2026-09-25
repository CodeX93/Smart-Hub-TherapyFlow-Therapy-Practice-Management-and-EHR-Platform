package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enum for address type classification
 * Defines the purpose/category of an address
 */
@Getter
public enum AddressType {
    HOME("Home", "Primary residential address"),
    WORK("Work", "Work or business address"),
    BILLING("Billing", "Address for billing and invoices"),
    MAILING("Mailing", "Mailing address if different from home"),
    TEMPORARY("Temporary", "Temporary or seasonal address"),
    OTHER("Other", "Other type of address");

    private final String displayName;
    private final String description;

    AddressType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }

    @JsonCreator
    public static AddressType fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return AddressType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid AddressType: " + value + 
                ". Valid values are: HOME, WORK, BILLING, MAILING, TEMPORARY, OTHER");
        }
    }

    public boolean isPrimary() {
        return this == HOME;
    }

    public boolean isBusiness() {
        return this == WORK;
    }
}


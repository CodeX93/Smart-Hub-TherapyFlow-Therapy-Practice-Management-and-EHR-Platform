package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enum for contact type classification
 * Defines how contact information should be categorized and prioritized
 */
@Getter
public enum ContactType {
    PHONE("Phone", true),
    EMAIL("Email", true),
    EMERGENCY_CONTACT("Emergency Contact", false),
    WORK_PHONE("Work Phone", true),
    FAX("Fax", true);

    private final String displayName;
    private final boolean canBeUsedForNotifications;

    ContactType(String displayName, boolean canBeUsedForNotifications) {
        this.displayName = displayName;
        this.canBeUsedForNotifications = canBeUsedForNotifications;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }

    @JsonCreator
    public static ContactType fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ContactType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ContactType: " + value + 
                ". Valid values are: PHONE, EMAIL, EMERGENCY_CONTACT, WORK_PHONE, FAX");
        }
    }

    public boolean isPhone() {
        return this == PHONE || this == WORK_PHONE;
    }

    public boolean isEmail() {
        return this == EMAIL;
    }

    public boolean isEmergency() {
        return this == EMERGENCY_CONTACT;
    }
}


package com.smart.therapy.flow.document.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.smart.therapy.flow.common.exception.BadRequestException;

import java.util.Arrays;

public enum ConnectionType {
    RELATED("Related"),
    REFERENCE("Reference"),
    DERIVED("Derived"),
    SUPPLEMENT("Supplement"),
    OTHER("Other");

    private final String displayName;

    ConnectionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static ConnectionType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(normalized)
                        || type.displayName.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Invalid connectionType '" + value
                                + "'. Allowed values: Related, Reference, Derived, Supplement, Other"));
    }

    @Override
    public String toString() {
        return displayName;
    }
}

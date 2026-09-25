package com.smart.therapy.flow.session.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Room type for sessions.
 * PHYSICAL: in-office / on-site rooms
 * VIRTUAL: online / telehealth rooms
 */
public enum RoomType {
    PHYSICAL("physical"),
    VIRTUAL("virtual");

    private final String value;

    RoomType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RoomType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (RoomType type : RoomType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid room type: " + value);
    }
}


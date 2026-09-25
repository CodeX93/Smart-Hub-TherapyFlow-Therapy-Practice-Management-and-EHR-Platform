package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.smart.therapy.flow.common.util.EnumParsing;

/**
 * Employment status for socioeconomic tracking
 */
public enum EmploymentStatus {

    EMPLOYED_FULL_TIME("Employed Full-Time"),
    EMPLOYED_PART_TIME("Employed Part-Time"),
    SELF_EMPLOYED("Self-Employed"),
    UNEMPLOYED("Unemployed"),
    RETIRED("Retired"),
    STUDENT("Student"),
    DISABLED("Disabled"),
    HOMEMAKER("Homemaker"),
    PREFER_NOT_TO_SAY("Prefer Not to Say");

    private final String displayName;

    EmploymentStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static EmploymentStatus fromValue(String value) {
        return EnumParsing.parse(EmploymentStatus.class, value, EmploymentStatus::getDisplayName, "employment status");
    }
}

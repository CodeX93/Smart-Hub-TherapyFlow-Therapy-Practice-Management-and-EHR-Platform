package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.smart.therapy.flow.common.util.EnumParsing;

/**
 * Educational level for socioeconomic tracking
 */
public enum EducationLevel {

    LESS_THAN_HIGH_SCHOOL("Less than High School"),
    HIGH_SCHOOL("High School Diploma/GED"),
    SOME_COLLEGE("Some College"),
    ASSOCIATE("Associate Degree"),
    BACHELOR("Bachelor's Degree"),
    MASTER("Master's Degree"),
    DOCTORATE("Doctorate/Professional Degree"),
    PREFER_NOT_TO_SAY("Prefer Not to Say"),
    OTHER("Other");

    private final String displayName;

    EducationLevel(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static EducationLevel fromValue(String value) {
        return EnumParsing.parse(EducationLevel.class, value, EducationLevel::getDisplayName, "education level");
    }
}

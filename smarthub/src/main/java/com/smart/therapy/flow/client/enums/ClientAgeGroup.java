package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;

/**
 * Client age classification.
 * Derived primarily from date of birth and used for clinical / reporting logic.
 */
public enum ClientAgeGroup {

    CHILD("Child", 0, 12, true, true),
    ADOLESCENT("Adolescent", 13, 17, true, true),
    YOUNG_ADULT("Young Adult", 18, 25, false, false),
    ADULT("Adult", 26, 64, false, false),
    GERIATRIC("Geriatric", 65, 200, false, false);

    private final String displayName;
    private final int minAgeInclusive;
    private final int maxAgeInclusive;
    private final boolean minor;
    private final boolean requiresParentalConsent;

    ClientAgeGroup(String displayName,
                   int minAgeInclusive,
                   int maxAgeInclusive,
                   boolean minor,
                   boolean requiresParentalConsent) {
        this.displayName = displayName;
        this.minAgeInclusive = minAgeInclusive;
        this.maxAgeInclusive = maxAgeInclusive;
        this.minor = minor;
        this.requiresParentalConsent = requiresParentalConsent;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public boolean isMinor() {
        return minor;
    }

    public boolean requiresParentalConsent() {
        return requiresParentalConsent;
    }

    public int getMinAgeInclusive() {
        return minAgeInclusive;
    }

    public int getMaxAgeInclusive() {
        return maxAgeInclusive;
    }

    /**
     * Map from API / UI value (display name) to enum.
     */
    @JsonCreator
    public static ClientAgeGroup fromValue(String value) {
        return Arrays.stream(ClientAgeGroup.values())
                .filter(group -> group.displayName.equalsIgnoreCase(value)
                        || group.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid client age group: " + value)
                );
    }

    /**
     * Derive age group from a date of birth.
     */
    public static ClientAgeGroup fromDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        int years = Period.between(dateOfBirth, LocalDate.now()).getYears();
        for (ClientAgeGroup group : ClientAgeGroup.values()) {
            if (years >= group.minAgeInclusive && years <= group.maxAgeInclusive) {
                return group;
            }
        }
        // Fallback: treat as adult if out of configured range
        return ADULT;
    }
}



package com.smart.therapy.flow.document.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ReviewStatus {
    PENDING,
    THERAPIST_REVIEW,
    SUPERVISOR_REVIEW,
    APPROVED,
    REJECTED,
    OVERDUE;

    @JsonCreator
    public static ReviewStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        return ReviewStatus.valueOf(value.trim().toUpperCase());
    }
}

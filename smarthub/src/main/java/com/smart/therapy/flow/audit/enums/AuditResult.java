package com.smart.therapy.flow.audit.enums;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Canonical result values for audit logs.
 */
public enum AuditResult {
    SUCCESS("success"),
    FAILURE("failure"),
    FAILED("failed"),
    BLOCKED("blocked");

    private final String dbValue;

    AuditResult(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public boolean isFailureLike() {
        return this == FAILURE || this == FAILED || this == BLOCKED;
    }

    public static AuditResult fromDbValueOrDefault(String value, AuditResult defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        String normalized = normalize(value);
        for (AuditResult result : values()) {
            if (result.dbValue.equals(normalized)) {
                return result;
            }
        }
        return defaultValue;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}

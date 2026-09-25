package com.smart.therapy.flow.audit.enums;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Risk levels used by audit log records and filters.
 */
public enum AuditRiskLevel {
    ALL("all"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical");

    private final String dbValue;

    AuditRiskLevel(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public boolean isAll() {
        return this == ALL;
    }

    public static AuditRiskLevel fromDbValue(String value) {
        if (!StringUtils.hasText(value)) {
            return ALL;
        }
        String normalized = normalize(value);
        for (AuditRiskLevel level : values()) {
            if (level.dbValue.equals(normalized)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown audit risk level: " + value);
    }

    public static Optional<AuditRiskLevel> tryFromFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.of(ALL);
        }
        try {
            return Optional.of(fromDbValue(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}

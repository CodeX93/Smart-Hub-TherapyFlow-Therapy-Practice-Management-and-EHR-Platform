package com.smart.therapy.flow.client.enums;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Supported status filters for admin consent management.
 */
public enum ConsentManagementStatusFilter {
    ALL,
    GRANTED,
    DENIED;

    public static Optional<ConsentManagementStatusFilter> tryParse(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return Optional.of(ALL);
        }

        String normalized = normalize(rawValue);
        return switch (normalized) {
            case "ALL", "ALL_CONSENT_STATUSES" -> Optional.of(ALL);
            case "GRANTED" -> Optional.of(GRANTED);
            case "DENIED", "DENIED_WITHDRAWN", "WITHDRAWN" -> Optional.of(DENIED);
            default -> Optional.empty();
        };
    }

    private static String normalize(String value) {
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}

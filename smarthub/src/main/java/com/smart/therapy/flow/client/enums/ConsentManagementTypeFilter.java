package com.smart.therapy.flow.client.enums;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Supported consent-type filters for admin consent management.
 */
public enum ConsentManagementTypeFilter {
    ALL,
    AI_PROCESSING,
    DATA_SHARING,
    RESEARCH,
    MARKETING;

    public static Optional<ConsentManagementTypeFilter> tryParse(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return Optional.of(ALL);
        }

        String normalized = normalize(rawValue);
        return switch (normalized) {
            case "ALL", "ALL_CONSENT_TYPES" -> Optional.of(ALL);
            case "AI_PROCESSING", "AI" -> Optional.of(AI_PROCESSING);
            case "DATA_SHARING" -> Optional.of(DATA_SHARING);
            case "RESEARCH", "RESEARCH_PARTICIPATION" -> Optional.of(RESEARCH);
            case "MARKETING", "MARKETING_COMMUNICATIONS" -> Optional.of(MARKETING);
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

package com.smart.therapy.flow.audit.util;

import org.springframework.util.StringUtils;

import java.util.Locale;

public final class AuditLogLevelResolver {

    private AuditLogLevelResolver() {
    }

    public static String normalizeFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TRACE", "DEBUG", "INFO", "WARN", "ERROR" -> normalized;
            case "ALL" -> null;
            default -> null;
        };
    }

    public static String resolveTenantLogLevel(String action, String result, String details) {
        String normalizedResult = result == null ? "" : result.trim().toLowerCase(Locale.ROOT);
        if ("failure".equals(normalizedResult) || "failed".equals(normalizedResult)) {
            return "ERROR";
        }
        if ("blocked".equals(normalizedResult)) {
            return "WARN";
        }
        if (hasServerError(details)) {
            return "ERROR";
        }
        if (hasClientWarning(details) || "unauthorized_access".equalsIgnoreCase(action)) {
            return "WARN";
        }
        if (isTrace(action, details)) {
            return "TRACE";
        }
        if (isDebug(action)) {
            return "DEBUG";
        }
        return "INFO";
    }

    public static String resolvePlatformLogLevel(String action, String details) {
        if (hasServerError(details) || hasErrorMarker(action, details)) {
            return "ERROR";
        }
        if (hasClientWarning(details)) {
            return "WARN";
        }
        if (isTrace(action, details)) {
            return "TRACE";
        }
        if (isDebug(action)) {
            return "DEBUG";
        }
        return "INFO";
    }

    private static boolean isDebug(String action) {
        return action != null && action.trim().toLowerCase(Locale.ROOT).startsWith("api_");
    }

    private static boolean isTrace(String action, String details) {
        if (!isDebug(action)) {
            return false;
        }
        if (!StringUtils.hasText(details)) {
            return false;
        }
        String normalized = details.toLowerCase(Locale.ROOT);
        return normalized.contains("details=[")
                || normalized.contains("\"details\":[")
                || normalized.contains("source=global_activity_aspect");
    }

    private static boolean hasServerError(String details) {
        if (!StringUtils.hasText(details)) {
            return false;
        }
        String normalized = details.toLowerCase(Locale.ROOT);
        return normalized.contains("status=5")
                || normalized.contains("\"status\":5")
                || normalized.contains("exception")
                || normalized.contains(" error=");
    }

    private static boolean hasClientWarning(String details) {
        if (!StringUtils.hasText(details)) {
            return false;
        }
        String normalized = details.toLowerCase(Locale.ROOT);
        return normalized.contains("status=4")
                || normalized.contains("\"status\":4")
                || normalized.contains("forbidden")
                || normalized.contains("unauthorized");
    }

    private static boolean hasErrorMarker(String action, String details) {
        String combined = ((action == null ? "" : action) + " " + (details == null ? "" : details))
                .toLowerCase(Locale.ROOT);
        return combined.contains("failed") || combined.contains("failure") || combined.contains("error");
    }
}


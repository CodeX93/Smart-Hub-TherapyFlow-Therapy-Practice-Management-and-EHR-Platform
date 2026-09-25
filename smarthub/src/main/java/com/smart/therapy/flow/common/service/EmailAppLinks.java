package com.smart.therapy.flow.common.service;

import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Absolute frontend URLs for transactional email CTAs.
 * Relative paths become {@code http:///...} in Gmail and break.
 */
public final class EmailAppLinks {

    public static final String DEFAULT_FRONTEND_BASE = "https://app.therapyflow.pro";

    public static final String CLIENT_LOGIN = "/auth/login";
    public static final String STAFF_LOGIN = "/auth/staff/login";
    public static final String CLIENT_INVOICES = "/user/invoices";
    public static final String CLIENT_SESSIONS = "/user/booked-sessions";
    public static final String STAFF_BILLINGS_ADMIN = "/admin/billings";
    public static final String STAFF_SCHEDULING = "/therapist/scheduling";

    private static final Set<String> URL_KEYS = Set.of(
            "paymentUrl",
            "invoiceUrl",
            "sessionUrl",
            "calendarUrl",
            "portalUrl",
            "actionUrl",
            "resetUrl",
            "activationUrl",
            "loginUrl");

    private EmailAppLinks() {
    }

    public static String normalizeBase(String configuredBaseOrLoginUrl) {
        if (!StringUtils.hasText(configuredBaseOrLoginUrl)) {
            return DEFAULT_FRONTEND_BASE;
        }
        try {
            URI uri = URI.create(configuredBaseOrLoginUrl.trim());
            if (uri.getScheme() != null && uri.getHost() != null) {
                int port = uri.getPort();
                if (port > 0) {
                    return uri.getScheme() + "://" + uri.getHost() + ":" + port;
                }
                return uri.getScheme() + "://" + uri.getHost();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return DEFAULT_FRONTEND_BASE;
    }

    public static String absolute(String frontendBase, String path) {
        String base = normalizeBase(frontendBase);
        if (!StringUtils.hasText(path)) {
            return base;
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            // Repair broken Gmail-style hosts like http:///path
            if (trimmed.matches("(?i)https?:///+.*") || trimmed.matches("(?i)https?:///[^/].*")) {
                String repairedPath = trimmed.replaceFirst("(?i)https?:///+", "/");
                return absolute(base, repairedPath);
            }
            return trimmed;
        }
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        // Legacy notification payloads used /client-portal/... which is not a real app route.
        trimmed = rewriteLegacyClientPortalPath(trimmed);
        return base + trimmed;
    }

    /**
     * Map dead /client-portal/* paths onto real client portal routes.
     */
    static String rewriteLegacyClientPortalPath(String path) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        String normalized = path.trim();
        if (normalized.matches("(?i)/client-portal/invoices(/.*)?")) {
            return CLIENT_INVOICES;
        }
        if (normalized.matches("(?i)/client-portal(/.*)?")) {
            return CLIENT_LOGIN;
        }
        return normalized;
    }

    /**
     * Client-facing CTAs: portal login / client portal pages.
     */
    public static Map<String, Object> forClientRecipient(Map<String, Object> payload, String frontendBase) {
        Map<String, Object> out = copy(payload);
        String base = normalizeBase(frontendBase);
        out.put("paymentUrl", absolute(base, CLIENT_INVOICES));
        out.put("invoiceUrl", absolute(base, CLIENT_INVOICES));
        out.put("sessionUrl", absolute(base, CLIENT_SESSIONS));
        out.put("loginUrl", absolute(base, CLIENT_LOGIN));
        absolutizeKnownUrlKeys(out, base);
        return out;
    }

    /**
     * Staff-facing CTAs: staff login / staff app pages.
     */
    public static Map<String, Object> forStaffRecipient(Map<String, Object> payload, String frontendBase) {
        Map<String, Object> out = copy(payload);
        String base = normalizeBase(frontendBase);
        out.put("paymentUrl", absolute(base, STAFF_LOGIN));
        out.put("invoiceUrl", absolute(base, STAFF_BILLINGS_ADMIN));
        out.put("sessionUrl", absolute(base, STAFF_SCHEDULING));
        out.put("loginUrl", absolute(base, STAFF_LOGIN));
        absolutizeKnownUrlKeys(out, base);
        return out;
    }

    public static void absolutizeKnownUrlKeys(Map<String, Object> payload, String frontendBase) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        String base = normalizeBase(frontendBase);
        for (String key : URL_KEYS) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            String asString = String.valueOf(value);
            if (!StringUtils.hasText(asString)) {
                continue;
            }
            // Leave Google Calendar / Zoom / external deep links alone when already absolute and valid.
            if ((asString.startsWith("http://") || asString.startsWith("https://"))
                    && !asString.matches("(?i)https?:///+.*")) {
                continue;
            }
            payload.put(key, absolute(base, asString));
        }
    }

    private static Map<String, Object> copy(Map<String, Object> payload) {
        return payload == null ? new HashMap<>() : new HashMap<>(payload);
    }
}

package com.smart.therapy.flow.auth.util;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Lightweight UA parsing and device fingerprinting (no external dependency).
 */
public final class DeviceInfoUtil {

    private DeviceInfoUtil() {}

    public static String deviceLabel(String userAgent) {
        String ua = userAgent == null ? "" : userAgent;
        String browser = detectBrowser(ua);
        String os = detectOs(ua);
        if ("Unknown".equals(browser) && "Unknown".equals(os)) {
            return "Unknown device";
        }
        return browser + " on " + os;
    }

    public static String fingerprintHash(Long authId, String userAgent) {
        String normalized = normalizeUserAgent(userAgent);
        String material = authId + "|" + normalized;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    static String normalizeUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent) || "unknown".equalsIgnoreCase(userAgent.trim())) {
            return "unknown";
        }
        // Keep browser/OS family signals; drop highly volatile build crumbs.
        return userAgent.trim()
                .replaceAll("(?i)\\s+AppleWebKit/[\\d.]+", "")
                .replaceAll("(?i)\\s+Safari/[\\d.]+", "")
                .replaceAll("(?i)\\s+Version/[\\d.]+", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static String detectBrowser(String ua) {
        String lower = ua.toLowerCase(Locale.ROOT);
        if (lower.contains("edg/")) return "Edge";
        if (lower.contains("chrome/") && !lower.contains("edg/")) return "Chrome";
        if (lower.contains("firefox/")) return "Firefox";
        if (lower.contains("safari/") && !lower.contains("chrome/")) return "Safari";
        if (lower.contains("opr/") || lower.contains("opera")) return "Opera";
        return "Unknown";
    }

    private static String detectOs(String ua) {
        String lower = ua.toLowerCase(Locale.ROOT);
        if (lower.contains("iphone") || lower.contains("ipad")) return "iOS";
        if (lower.contains("android")) return "Android";
        if (lower.contains("mac os x") || lower.contains("macintosh")) return "macOS";
        if (lower.contains("windows")) return "Windows";
        if (lower.contains("linux")) return "Linux";
        return "Unknown";
    }
}

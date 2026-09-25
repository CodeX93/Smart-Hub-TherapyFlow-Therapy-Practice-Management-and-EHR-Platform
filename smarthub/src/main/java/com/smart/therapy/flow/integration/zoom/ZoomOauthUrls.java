package com.smart.therapy.flow.integration.zoom;

/**
 * Builds Zoom Server-to-Server OAuth endpoints from a configurable base URL.
 * Accepts either {@code https://zoom.us} or {@code https://zoom.us/oauth} so
 * we never produce the invalid double path {@code /oauth/oauth/token}.
 */
public final class ZoomOauthUrls {

    private static final String DEFAULT_BASE = "https://zoom.us";

    private ZoomOauthUrls() {
    }

    /**
     * @param baseUrl {@code zoom.oauth.base-url} (host only, or host + {@code /oauth})
     * @return Zoom account credentials token URL, e.g. {@code https://zoom.us/oauth/token}
     */
    public static String tokenUrl(String baseUrl) {
        String base = normalizeBase(baseUrl);
        return base + "/oauth/token";
    }

    static String normalizeBase(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE;
        }
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.regionMatches(true, base.length() - "/oauth".length(), "/oauth", 0, "/oauth".length())) {
            base = base.substring(0, base.length() - "/oauth".length());
            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
        }
        return base.isEmpty() ? DEFAULT_BASE : base;
    }
}

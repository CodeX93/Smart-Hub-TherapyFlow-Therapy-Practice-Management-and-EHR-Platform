package com.smart.therapy.flow.common.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility class for extracting information from HTTP requests.
 * Centralizes common request processing logic to reduce code duplication.
 */
@Slf4j
@Component
public class HttpRequestUtil {

    /**
     * Extracts the client IP address from the HTTP request.
     * Handles proxy headers (X-Forwarded-For, X-Real-IP) and falls back to remote address.
     * 
     * @param request The HTTP servlet request
     * @return The client IP address, or "unknown" if unable to determine
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            log.warn("HttpServletRequest is null, returning 'unknown' for IP address");
            return "unknown";
        }

        String[] headerCandidates = {
                "CF-Connecting-IP",
                "True-Client-IP",
                "X-Azure-ClientIP",
                "X-Forwarded-For",
                "X-Real-IP",
                "Forwarded"
        };
        for (String headerName : headerCandidates) {
            String header = request.getHeader(headerName);
            String extracted = extractIpFromHeader(headerName, header);
            if (extracted != null) {
                return extracted;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private static String extractIpFromHeader(String headerName, String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.trim();
        if ("Forwarded".equalsIgnoreCase(headerName)) {
            // e.g. for=1.2.3.4;proto=https;by=...
            for (String part : value.split(";")) {
                String trimmed = part.trim();
                if (trimmed.regionMatches(true, 0, "for=", 0, 4)) {
                    value = trimmed.substring(4).trim();
                    break;
                }
            }
        }
        if (value.contains(",")) {
            value = value.split(",")[0].trim();
        }
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1);
        }
        if (value.startsWith("[") && value.contains("]")) {
            value = value.substring(1, value.indexOf(']'));
        }
        // Strip optional port for IPv4 host:port
        if (value.matches("^\\d{1,3}(\\.\\d{1,3}){3}:\\d+$")) {
            value = value.substring(0, value.lastIndexOf(':'));
        }
        return value.isEmpty() ? null : value;
    }

    /**
     * Extracts the User-Agent string from the HTTP request.
     * 
     * @param request The HTTP servlet request
     * @return The User-Agent string, or "unknown" if not available
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }

    /**
     * Extracts the referer URL from the HTTP request.
     * 
     * @param request The HTTP servlet request
     * @return The referer URL, or null if not available
     */
    public static String getReferer(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader("Referer");
    }
}


package com.smart.therapy.flow.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit for /api/v1/super-admin/** to reduce abuse. Per-IP, fixed window.
 * Lightweight notification polling endpoints are excluded so SPA dashboards are not blocked.
 */
@Component
@Order(2)
@Slf4j
public class SuperAdminRateLimitFilter implements Filter {

    private static final String SUPER_ADMIN_PATH_PREFIX = "/api/v1/super-admin/";

    @Value("${super-admin.rate-limit.per-minute:600}")
    private int limitPerMinute = 600;

    @Value("${super-admin.rate-limit.enabled:true}")
    private boolean enabled = true;

    /** key (IP) -> Window(count, windowStartMs) */
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 60_000L;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();
        if (!enabled || path == null || !path.startsWith(SUPER_ADMIN_PATH_PREFIX) || isExcluded(path)) {
            chain.doFilter(request, response);
            return;
        }
        String key = clientKey(req);
        if (!tryConsume(key)) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Too many requests. Try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isExcluded(String path) {
        // High-frequency SPA polling — do not consume the shared super-admin budget.
        return path.startsWith("/api/v1/super-admin/notifications/unread-count")
                || path.startsWith("/api/v1/super-admin/notifications/history")
                || path.endsWith("/notifications/unread-count")
                || path.contains("/notifications/unread-count");
    }

    private String clientKey(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "unknown";
    }

    private synchronized boolean tryConsume(String key) {
        long now = System.currentTimeMillis();
        Window w = windows.get(key);
        if (w == null || now - w.windowStartMs >= WINDOW_MS) {
            w = new Window(1, now);
            windows.put(key, w);
            return true;
        }
        if (w.count >= limitPerMinute) {
            return false;
        }
        w.count++;
        return true;
    }

    private static final class Window {
        int count;
        final long windowStartMs;

        Window(int count, long windowStartMs) {
            this.count = count;
            this.windowStartMs = windowStartMs;
        }
    }
}

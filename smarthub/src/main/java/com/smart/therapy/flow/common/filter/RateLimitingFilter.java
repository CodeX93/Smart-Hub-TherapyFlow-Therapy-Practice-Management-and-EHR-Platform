package com.smart.therapy.flow.common.filter;

import com.smart.therapy.flow.auth.security.AuthRefreshCookie;
import com.smart.therapy.flow.auth.service.AuthRefreshTokenService;
import com.smart.therapy.flow.common.config.RateLimitingConfig;
import com.smart.therapy.flow.common.metrics.AuthAbuseMetrics;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import io.github.bucket4j.Bucket;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(2)
public class RateLimitingFilter implements Filter {

    private static final long WINDOW_SECONDS = 60;
    private static final Set<String> REFRESH_COOKIES = Set.of(
            AuthRefreshCookie.Audience.STAFF.cookieName(), AuthRefreshCookie.Audience.PORTAL.cookieName());
    private static final EndpointLimit REFRESH_WITHOUT_SESSION = new EndpointLimit("refresh", 20, 20, WINDOW_SECONDS);
    private static final EndpointLimit REFRESH_PER_SESSION = new EndpointLimit("refresh-session", 30, 30, WINDOW_SECONDS);
    private static final EndpointLimit REFRESH_PER_IP = new EndpointLimit("refresh-ip", 600, 600, WINDOW_SECONDS);

    private final RateLimitingConfig rateLimitingConfig;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final AuthAbuseMetrics authAbuseMetrics;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    @Value("${rate-limiting.distributed.enabled:true}")
    private boolean distributedEnabled;

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled;

    public RateLimitingFilter(
            RateLimitingConfig rateLimitingConfig,
            Optional<RedisTemplate<String, Object>> redisTemplate,
            AuthAbuseMetrics authAbuseMetrics) {
        this.rateLimitingConfig = rateLimitingConfig;
        this.redisTemplate = redisTemplate;
        this.authAbuseMetrics = authAbuseMetrics;
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!rateLimitingConfig.isRateLimitingEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String key = getRateLimitKey(httpRequest);
        String path = httpRequest.getRequestURI();
        if (path != null && path.startsWith("/api/v1/super-admin/")) {
            chain.doFilter(request, response);
            return;
        }

        for (Check check : resolveChecks(httpRequest, key, path)) {
            if (!consume(check.key(), check.limit())) {
                reject(httpRequest, httpResponse, check.key(), check.limit());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String key, EndpointLimit limit)
            throws IOException {
        log.warn("Rate limit exceeded: key={}, path={}", key, httpRequest.getRequestURI());
        authAbuseMetrics.incrementRateLimited();
        httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(limit.capacity()));
        httpResponse.setHeader("X-RateLimit-Remaining", "0");
        httpResponse.setHeader("Retry-After", String.valueOf(limit.windowSeconds()));
        httpResponse.setContentType("application/json");
        httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests\"}");
    }

    private boolean consume(String key, EndpointLimit limit) {
        return useDistributedRateLimiting()
                ? tryConsumeDistributed(key, limit)
                : getBucket(key, limit).tryConsume(1);
    }

    /**
     * Every page load of the app now restores its session through a refresh, so a per-IP refresh cap
     * logs out a whole office behind one NAT address. A refresh carrying a session cookie is capped per
     * session instead, with a looser per-IP ceiling that still stops cookie-rotating abuse; one without
     * a cookie keeps the strict per-IP cap.
     */
    private List<Check> resolveChecks(HttpServletRequest request, String ipKey, String path) {
        if (!isRefreshPath(path)) {
            return List.of(new Check(ipKey, resolveLimit(path)));
        }
        String session = refreshCookieFingerprint(request);
        if (session == null) {
            return List.of(new Check(ipKey, REFRESH_WITHOUT_SESSION));
        }
        return List.of(
                new Check(ipKey + ":session:" + session, REFRESH_PER_SESSION),
                new Check(ipKey, REFRESH_PER_IP));
    }

    /** A short, non-reversible handle on the refresh cookie, so no token lands in a rate-limit key. */
    private static String refreshCookieFingerprint(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIES.contains(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return AuthRefreshTokenService.hashToken(cookie.getValue()).substring(0, 16);
            }
        }
        return null;
    }

    private boolean useDistributedRateLimiting() {
        return distributedEnabled && redisEnabled && redisTemplate.isPresent();
    }

    private boolean tryConsumeDistributed(String key, EndpointLimit limit) {
        long now = Instant.now().getEpochSecond();
        String redisKey = "rate:admin:" + key + ":" + limit.suffix() + ":" + (now / limit.windowSeconds());
        try {
            Long count = redisTemplate.get().opsForValue().increment(redisKey);
            if (count != null && count == 1L) {
                redisTemplate.get().expire(redisKey, limit.windowSeconds() + 5, TimeUnit.SECONDS);
            }
            return count == null || count <= limit.capacity();
        } catch (Exception ex) {
            log.warn("Distributed rate limit check failed, falling back to in-memory: {}", ex.getMessage());
            return tryConsumeInMemory(key, limit);
        }
    }

    private boolean tryConsumeInMemory(String key, EndpointLimit limit) {
        String counterKey = key + ":" + limit.suffix();
        long now = Instant.now().getEpochSecond();
        Counter counter = counters.computeIfAbsent(counterKey, ignored -> new Counter(now, 0));
        synchronized (counter) {
            if (now - counter.windowStart >= limit.windowSeconds()) {
                counter.windowStart = now;
                counter.count = 0;
            }
            counter.count++;
            return counter.count <= limit.capacity();
        }
    }

    private String getRateLimitKey(HttpServletRequest request) {
        return "rate-limit:" + HttpRequestUtil.getClientIp(request);
    }

    private Bucket getBucket(String key, EndpointLimit limit) {
        return rateLimitingConfig.getBucket(
                key + ":" + limit.suffix(),
                limit.capacity(),
                limit.refillTokens(),
                Duration.ofSeconds(limit.windowSeconds()));
    }

    private EndpointLimit resolveLimit(String path) {
        if (matchesEndpoint(path, "/api/v1/auth/login")) {
            return new EndpointLimit("login", 10, 10, WINDOW_SECONDS);
        }
        return new EndpointLimit(
                "default",
                rateLimitingConfig.getDefaultCapacity(),
                rateLimitingConfig.getDefaultRefillTokens(),
                rateLimitingConfig.getDefaultRefillDurationSeconds());
    }

    private static boolean isRefreshPath(String path) {
        return matchesEndpoint(path, "/api/v1/auth/refresh") || matchesEndpoint(path, "/api/v1/portal/refresh");
    }

    private static boolean matchesEndpoint(String path, String endpoint) {
        return path != null && (path.equals(endpoint) || path.startsWith(endpoint + "/"));
    }

    private record EndpointLimit(String suffix, int capacity, int refillTokens, long windowSeconds) {
    }

    private record Check(String key, EndpointLimit limit) {
    }

    private static final class Counter {
        private long windowStart;
        private int count;

        private Counter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

package com.smart.therapy.flow.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PublicEndpointRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;
    private static final int TOO_MANY_REQUESTS = 429;
    private static final String RATE_LIMIT_MESSAGE = "{\"error\":\"Too many requests\",\"code\":\"RATE_LIMITED\"}";

    @Value("${app.security.public-rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.public-rate-limit.requests-per-minute:300}")
    private int requestsPerMinute;

    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public PublicEndpointRateLimitFilter(Optional<RedisTemplate<String, Object>> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        if (!StringUtils.hasText(path)) {
            return true;
        }
        boolean isPublic = path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/portal/")
                || path.startsWith("/api/v1/public/")
                || path.startsWith("/api/public/")
                || path.startsWith("/api/v1/notifications/templates");
        return !isPublic;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = buildClientKey(request);
        if (isRateLimited(clientKey)) {
            response.setStatus(TOO_MANY_REQUESTS);
            response.setContentType("application/json");
            response.getWriter().write(RATE_LIMIT_MESSAGE);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key) {
        long now = Instant.now().getEpochSecond();
        if (redisTemplate.isPresent()) {
            try {
                String redisKey = "rate:public:" + key + ":" + (now / WINDOW_SECONDS);
                Long count = redisTemplate.get().opsForValue().increment(redisKey);
                if (count != null && count == 1L) {
                    redisTemplate.get().expire(redisKey, WINDOW_SECONDS + 5, TimeUnit.SECONDS);
                }
                return count != null && count > requestsPerMinute;
            } catch (Exception ex) {
                log.warn("Rate limit check failed, falling back to in-memory: {}", ex.getMessage());
            }
        }
        Counter counter = counters.computeIfAbsent(key, k -> new Counter(now, 0));
        synchronized (counter) {
            if (now - counter.windowStart >= WINDOW_SECONDS) {
                counter.windowStart = now;
                counter.count = 0;
            }
            counter.count++;
            return counter.count > requestsPerMinute;
        }
    }

    private static String buildClientKey(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        return ip + "|" + request.getRequestURI();
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

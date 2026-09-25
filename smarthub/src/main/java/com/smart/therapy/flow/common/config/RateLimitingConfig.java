package com.smart.therapy.flow.common.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    @Value("${rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${rate-limiting.default.capacity:300}")
    private int defaultCapacity;

    @Value("${rate-limiting.default.refill-tokens:300}")
    private int defaultRefillTokens;

    @Value("${rate-limiting.default.refill-duration-seconds:60}")
    private int defaultRefillDurationSeconds;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Bean
    public Map<String, Bucket> rateLimitBuckets() {
        return buckets;
    }

    public Bucket getBucket(String key) {
        return buckets.computeIfAbsent(key, k -> createDefaultBucket());
    }

    public Bucket getBucket(String key, int capacity, int refillTokens, Duration refillDuration) {
        return buckets.computeIfAbsent(key, k -> createBucket(capacity, refillTokens, refillDuration));
    }

    private Bucket createDefaultBucket() {
        return createBucket(defaultCapacity, defaultRefillTokens, Duration.ofSeconds(defaultRefillDurationSeconds));
    }

    private Bucket createBucket(int capacity, int refillTokens, Duration refillDuration) {
        // Use new Bucket4j API (non-deprecated)
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillTokens, refillDuration)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean isRateLimitingEnabled() {
        return rateLimitingEnabled;
    }

    public int getDefaultCapacity() {
        return defaultCapacity;
    }

    public int getDefaultRefillTokens() {
        return defaultRefillTokens;
    }

    public int getDefaultRefillDurationSeconds() {
        return defaultRefillDurationSeconds;
    }
}


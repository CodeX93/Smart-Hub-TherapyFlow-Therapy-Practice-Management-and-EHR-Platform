package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.common.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class TranscriptChunkRateLimiter {

    private final Optional<RedisTemplate<String, Object>> redisTemplate;

    @Value("${app.transcripts.chunk-rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.transcripts.chunk-rate-limit.max-requests:120}")
    private int maxRequests;

    @Value("${app.transcripts.chunk-rate-limit.window-seconds:600}")
    private int windowSeconds;

    private final Map<String, Counter> inMemoryCounters = new ConcurrentHashMap<>();

    public TranscriptChunkRateLimiter(Optional<RedisTemplate<String, Object>> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkAndIncrement(Long userId, Long sessionId) {
        if (!enabled || userId == null || sessionId == null) {
            return;
        }

        String key = "transcript_chunk:" + userId + ":" + sessionId + ":" + (Instant.now().getEpochSecond() / windowSeconds);

        if (redisTemplate.isPresent()) {
            try {
                Long count = redisTemplate.get().opsForValue().increment(key);
                if (count != null && count == 1L) {
                    redisTemplate.get().expire(key, windowSeconds + 30L, TimeUnit.SECONDS);
                }
                if (count != null && count > maxRequests) {
                    throw new RateLimitExceededException(
                            "Chunk upload rate limit exceeded. Try again later.",
                            windowSeconds);
                }
                return;
            } catch (RateLimitExceededException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("Transcript chunk rate limit redis fallback: {}", ex.getMessage());
            }
        }

        Counter counter = inMemoryCounters.computeIfAbsent(key, k -> new Counter(Instant.now().getEpochSecond(), 0));
        synchronized (counter) {
            long now = Instant.now().getEpochSecond();
            long windowStart = now - (now % windowSeconds);
            if (counter.windowStart != windowStart) {
                counter.windowStart = windowStart;
                counter.count = 0;
            }
            counter.count++;
            if (counter.count > maxRequests) {
                throw new RateLimitExceededException(
                        "Chunk upload rate limit exceeded. Try again later.",
                        windowSeconds);
            }
        }
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

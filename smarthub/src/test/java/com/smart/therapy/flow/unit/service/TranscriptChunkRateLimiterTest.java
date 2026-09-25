package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.exception.RateLimitExceededException;
import com.smart.therapy.flow.session.service.TranscriptChunkRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TranscriptChunkRateLimiter Unit Tests")
class TranscriptChunkRateLimiterTest {

    private TranscriptChunkRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new TranscriptChunkRateLimiter(Optional.empty());
        ReflectionTestUtils.setField(rateLimiter, "enabled", true);
        ReflectionTestUtils.setField(rateLimiter, "maxRequests", 3);
        ReflectionTestUtils.setField(rateLimiter, "windowSeconds", 600);
    }

    @Test
    @DisplayName("Should allow requests under the configured limit")
    void shouldAllowRequestsUnderLimit() {
        assertThatCode(() -> {
            rateLimiter.checkAndIncrement(1L, 21L);
            rateLimiter.checkAndIncrement(1L, 21L);
            rateLimiter.checkAndIncrement(1L, 21L);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject requests above the configured limit")
    void shouldRejectRequestsAboveLimit() {
        rateLimiter.checkAndIncrement(1L, 21L);
        rateLimiter.checkAndIncrement(1L, 21L);
        rateLimiter.checkAndIncrement(1L, 21L);

        assertThatThrownBy(() -> rateLimiter.checkAndIncrement(1L, 21L))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("rate limit exceeded");
    }

    @Test
    @DisplayName("Should track limits separately per session")
    void shouldTrackLimitsSeparatelyPerSession() {
        rateLimiter.checkAndIncrement(1L, 21L);
        rateLimiter.checkAndIncrement(1L, 21L);
        rateLimiter.checkAndIncrement(1L, 21L);

        assertThatCode(() -> rateLimiter.checkAndIncrement(1L, 22L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should skip enforcement when disabled")
    void shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(rateLimiter, "enabled", false);

        assertThatCode(() -> {
            for (int i = 0; i < 10; i++) {
                rateLimiter.checkAndIncrement(1L, 21L);
            }
        }).doesNotThrowAnyException();
    }
}

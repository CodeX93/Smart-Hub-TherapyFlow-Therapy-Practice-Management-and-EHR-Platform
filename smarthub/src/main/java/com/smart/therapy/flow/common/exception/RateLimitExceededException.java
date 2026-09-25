package com.smart.therapy.flow.common.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    private final int retryAfterSeconds;

    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}

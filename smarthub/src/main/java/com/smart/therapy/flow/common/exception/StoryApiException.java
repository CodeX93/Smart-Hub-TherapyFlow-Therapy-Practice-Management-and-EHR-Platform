package com.smart.therapy.flow.common.exception;

import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StoryApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;
    private final Integer retryAfterSeconds;

    public StoryApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null, null);
    }

    public StoryApiException(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details,
            Integer retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}

package com.smart.therapy.flow.common.exception;

import lombok.Getter;

/**
 * Transcription provider is unreachable, misconfigured, rate-limited, or temporarily down.
 */
@Getter
public class TranscriptionServiceUnavailableException extends RuntimeException {

    private final ErrorCode errorCode;
    private final boolean retryable;
    private final String category;

    public TranscriptionServiceUnavailableException(String message) {
        this(message, ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, true, "service_unavailable", null);
    }

    public TranscriptionServiceUnavailableException(String message, Throwable cause) {
        this(message, ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, true, "service_unavailable", cause);
    }

    public TranscriptionServiceUnavailableException(String message,
                                                    ErrorCode errorCode,
                                                    boolean retryable,
                                                    String category) {
        this(message, errorCode, retryable, category, null);
    }

    public TranscriptionServiceUnavailableException(String message,
                                                    ErrorCode errorCode,
                                                    boolean retryable,
                                                    String category,
                                                    Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE;
        this.retryable = retryable;
        this.category = category != null ? category : "service_unavailable";
    }
}

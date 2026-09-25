package com.smart.therapy.flow.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Transcription could not be completed (invalid audio, upstream rejection, etc.).
 */
@Getter
public class TranscriptionFailedException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    private final boolean retryable;
    private final String category;

    public TranscriptionFailedException(String message) {
        this(message, ErrorCode.EXTERNAL_OPENAI_ERROR, HttpStatus.BAD_GATEWAY, false, "transcription_failed", null);
    }

    public TranscriptionFailedException(String message, Throwable cause) {
        this(message, ErrorCode.EXTERNAL_OPENAI_ERROR, HttpStatus.BAD_GATEWAY, false, "transcription_failed", cause);
    }

    public TranscriptionFailedException(String message,
                                        ErrorCode errorCode,
                                        HttpStatus httpStatus,
                                        boolean retryable,
                                        String category) {
        this(message, errorCode, httpStatus, retryable, category, null);
    }

    public TranscriptionFailedException(String message,
                                        ErrorCode errorCode,
                                        HttpStatus httpStatus,
                                        boolean retryable,
                                        String category,
                                        Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : ErrorCode.EXTERNAL_OPENAI_ERROR;
        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.BAD_GATEWAY;
        this.retryable = retryable;
        this.category = category != null ? category : "transcription_failed";
    }
}

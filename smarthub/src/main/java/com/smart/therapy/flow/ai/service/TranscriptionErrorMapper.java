package com.smart.therapy.flow.ai.service;

import com.smart.therapy.flow.common.exception.ErrorCode;
import com.smart.therapy.flow.common.exception.TranscriptionFailedException;
import com.smart.therapy.flow.common.exception.TranscriptionServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Maps upstream OpenAI transcription failures to user-facing API exceptions.
 */
public final class TranscriptionErrorMapper {

    private static final String PROVIDER = "openai";

    private TranscriptionErrorMapper() {
    }

    public static TranscriptionServiceUnavailableException apiKeyNotConfigured() {
        return new TranscriptionServiceUnavailableException(
                "Speech transcription is not configured on the server. Please contact support.",
                ErrorCode.SYSTEM_CONFIGURATION_ERROR,
                false,
                "configuration_error");
    }

    public static TranscriptionServiceUnavailableException circuitOpen(Throwable cause) {
        return new TranscriptionServiceUnavailableException(
                "Transcription is temporarily unavailable. Please wait a moment and try again.",
                ErrorCode.EXTERNAL_OPENAI_ERROR,
                true,
                "circuit_open",
                cause);
    }

    static TranscriptionServiceUnavailableException timeout(Throwable cause) {
        return new TranscriptionServiceUnavailableException(
                "Transcription timed out. Please try again.",
                ErrorCode.EXTERNAL_TIMEOUT,
                true,
                "timeout",
                cause);
    }

    static TranscriptionServiceUnavailableException networkFailure(Throwable cause) {
        return new TranscriptionServiceUnavailableException(
                "Could not reach the transcription service. Please check your connection and try again.",
                ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                true,
                "network_error",
                cause);
    }

    public static RuntimeException fromHttpClientError(HttpClientErrorException ex) {
        int status = ex.getStatusCode().value();
        String responseBody = ex.getResponseBodyAsString();

        if (status == 429 || containsQuotaError(responseBody)) {
            return new TranscriptionServiceUnavailableException(
                    "Transcription is temporarily unavailable due to service limits. Please try again later.",
                    ErrorCode.EXTERNAL_OPENAI_ERROR,
                    true,
                    "quota_exceeded",
                    ex);
        }
        if (status == 401 || status == 403) {
            return new TranscriptionServiceUnavailableException(
                    "Speech transcription is unavailable due to a server configuration issue. Please contact support.",
                    ErrorCode.SYSTEM_CONFIGURATION_ERROR,
                    false,
                    "authentication_error",
                    ex);
        }
        if (status == 400 || status == 422) {
            return new TranscriptionFailedException(
                    "The audio chunk could not be processed. Try recording again (WebM/Opus is recommended).",
                    ErrorCode.EXTERNAL_OPENAI_ERROR,
                    HttpStatus.BAD_REQUEST,
                    false,
                    "invalid_audio",
                    ex);
        }
        return new TranscriptionServiceUnavailableException(
                "Transcription is temporarily unavailable. Please try again later.",
                ErrorCode.EXTERNAL_OPENAI_ERROR,
                true,
                "upstream_http_" + status,
                ex);
    }

    public static RuntimeException fromRestClientError(RestClientException ex) {
        if (isTimeout(ex)) {
            return timeout(ex);
        }
        return networkFailure(ex);
    }

    static TranscriptionFailedException fromUnexpectedFailure(Throwable cause) {
        return new TranscriptionFailedException(
                "Transcription failed. Please try again.",
                ErrorCode.EXTERNAL_OPENAI_ERROR,
                HttpStatus.BAD_GATEWAY,
                true,
                "upstream_error",
                cause);
    }

    static String provider() {
        return PROVIDER;
    }

    private static boolean containsQuotaError(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return false;
        }
        String lower = responseBody.toLowerCase();
        return lower.contains("insufficient_quota") || lower.contains("exceeded your current quota");
    }

    private static boolean isTimeout(Throwable ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("timeout") || lower.contains("timed out");
    }
}

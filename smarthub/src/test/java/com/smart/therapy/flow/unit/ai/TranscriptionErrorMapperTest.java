package com.smart.therapy.flow.unit.ai;

import com.smart.therapy.flow.ai.service.TranscriptionErrorMapper;
import com.smart.therapy.flow.common.exception.ErrorCode;
import com.smart.therapy.flow.common.exception.TranscriptionFailedException;
import com.smart.therapy.flow.common.exception.TranscriptionServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptionErrorMapperTest {

    @Test
    @DisplayName("Should map OpenAI quota errors to retryable service unavailable response")
    void shouldMapQuotaError() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, "{\"error\":\"insufficient_quota\"}".getBytes(), null);

        RuntimeException mapped = TranscriptionErrorMapper.fromHttpClientError(ex);

        assertThat(mapped).isInstanceOf(TranscriptionServiceUnavailableException.class);
        TranscriptionServiceUnavailableException unavailable = (TranscriptionServiceUnavailableException) mapped;
        assertThat(unavailable.getMessage()).contains("service limits");
        assertThat(unavailable.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_OPENAI_ERROR);
        assertThat(unavailable.isRetryable()).isTrue();
        assertThat(unavailable.getCategory()).isEqualTo("quota_exceeded");
    }

    @Test
    @DisplayName("Should map invalid audio to client-facing bad request")
    void shouldMapInvalidAudio() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null, "{\"error\":\"invalid file\"}".getBytes(), null);

        RuntimeException mapped = TranscriptionErrorMapper.fromHttpClientError(ex);

        assertThat(mapped).isInstanceOf(TranscriptionFailedException.class);
        TranscriptionFailedException failed = (TranscriptionFailedException) mapped;
        assertThat(failed.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(failed.getMessage()).contains("audio chunk");
        assertThat(failed.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("Should map network timeout to retryable unavailable response")
    void shouldMapTimeout() {
        RuntimeException mapped = TranscriptionErrorMapper.fromRestClientError(
                new ResourceAccessException("Read timed out"));

        assertThat(mapped).isInstanceOf(TranscriptionServiceUnavailableException.class);
        assertThat(((TranscriptionServiceUnavailableException) mapped).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_TIMEOUT);
        assertThat(((TranscriptionServiceUnavailableException) mapped).isRetryable()).isTrue();
    }
}

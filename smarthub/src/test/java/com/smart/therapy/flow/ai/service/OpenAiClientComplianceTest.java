package com.smart.therapy.flow.ai.service;

import com.smart.therapy.flow.common.exception.TranscriptionServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiClientComplianceTest {
    private OpenAiClient client;

    @BeforeEach
    void setUp() {
        client = new OpenAiClient(
                new RestTemplateBuilder(),
                "test-api-key",
                "https://provider.invalid/v1",
                100,
                100
        );
        ReflectionTestUtils.setField(client, "appEnvironment", "prod");
        ReflectionTestUtils.setField(client, "phiProcessingApproved", false);
    }

    @Test
    void blocksClinicalChatWhenProviderApprovalIsMissingInProduction() {
        assertThatThrownBy(() -> client.createChatCompletion(
                "model",
                List.of(Map.of("role", "user", "content", "clinical narrative")),
                0.1,
                100
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("compliance approval");
    }

    @Test
    void blocksTranscriptionWhenProviderApprovalIsMissingInProduction() {
        assertThatThrownBy(() -> client.transcribeAudio(new byte[]{1}, "session.wav"))
                .isInstanceOf(TranscriptionServiceUnavailableException.class)
                .hasMessageContaining("compliance approval");
    }
}

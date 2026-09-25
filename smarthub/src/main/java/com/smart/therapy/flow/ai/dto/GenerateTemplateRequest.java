package com.smart.therapy.flow.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTemplateRequest {
    @NotNull
    private Long clientId;
    private Long sessionId;
    /** Saved therapist template; instructions loaded server-side when customInstructions is blank. */
    private Long templateId;
    private Map<String, String> formData;
    private String customInstructions;
}


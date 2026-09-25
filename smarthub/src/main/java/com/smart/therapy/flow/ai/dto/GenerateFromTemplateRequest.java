package com.smart.therapy.flow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateFromTemplateRequest {
    @NotBlank
    private String templateId;
    @NotBlank
    private String field;
    private String context;
}


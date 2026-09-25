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
public class ConnectedSuggestionsRequest {
    @NotBlank
    private String templateId;
    @NotBlank
    private String sourceField;
    @NotBlank
    private String sourceValue;
    private Long clientId;
}


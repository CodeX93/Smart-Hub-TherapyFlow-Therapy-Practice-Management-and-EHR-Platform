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
public class GenerateSuggestionsRequest {
    @NotBlank
    private String field;
    @NotBlank
    private String context;
    private Long clientId;
}


package com.smart.therapy.flow.client.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkDuplicateRequest {
    @NotNull(message = "Duplicate of client ID is required")
    private Long duplicateOfClientId;
}


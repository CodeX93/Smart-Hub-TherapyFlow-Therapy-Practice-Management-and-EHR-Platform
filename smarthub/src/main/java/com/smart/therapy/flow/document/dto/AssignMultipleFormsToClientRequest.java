package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class AssignMultipleFormsToClientRequest {
    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotEmpty(message = "At least one template ID is required")
    private List<Long> templateIds;

    private Instant dueDate;

    private String instructions;
}

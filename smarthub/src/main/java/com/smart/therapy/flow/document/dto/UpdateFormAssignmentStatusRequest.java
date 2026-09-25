package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateFormAssignmentStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;
}

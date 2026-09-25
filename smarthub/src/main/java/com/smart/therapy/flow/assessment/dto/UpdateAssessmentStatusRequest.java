package com.smart.therapy.flow.assessment.dto;

import com.smart.therapy.flow.assessment.enums.AssessmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to update assessment assignment status")
public class UpdateAssessmentStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New assessment status", example = "CLIENT_IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
    private AssessmentStatus status;

    @Schema(description = "Optional notes about the status change", example = "Client has started the assessment")
    private String notes;
}

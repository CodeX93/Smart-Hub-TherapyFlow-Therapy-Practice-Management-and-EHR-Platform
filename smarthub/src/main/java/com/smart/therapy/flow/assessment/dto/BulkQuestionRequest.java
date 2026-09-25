package com.smart.therapy.flow.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request for bulk question operations")
public class BulkQuestionRequest {

    @NotEmpty(message = "Questions list cannot be empty")
    @Schema(description = "List of questions to create/update", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<AssessmentQuestionRequest> questions;
}

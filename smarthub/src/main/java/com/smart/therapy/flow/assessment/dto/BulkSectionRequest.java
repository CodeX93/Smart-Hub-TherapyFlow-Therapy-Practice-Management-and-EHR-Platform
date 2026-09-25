package com.smart.therapy.flow.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request for bulk section operations")
public class BulkSectionRequest {

    @NotEmpty(message = "Sections list cannot be empty")
    @Schema(description = "List of sections to create/update", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<AssessmentSectionRequest> sections;
}

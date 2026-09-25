package com.smart.therapy.flow.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request for bulk delete operations")
public class BulkDeleteRequest {

    @NotEmpty(message = "IDs list cannot be empty")
    @Schema(description = "List of IDs to delete", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2, 3]")
    private List<Long> ids;
}

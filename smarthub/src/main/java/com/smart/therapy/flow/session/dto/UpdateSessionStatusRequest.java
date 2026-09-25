package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update session status")
public class UpdateSessionStatusRequest {
    @NotNull(message = "Status is required")
    @Schema(description = "Session status option key", example = "completed", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}


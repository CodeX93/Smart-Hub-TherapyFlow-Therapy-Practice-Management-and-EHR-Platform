package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Force password reset request")
public class SuperAdminForcePasswordResetRequest {

    @Schema(description = "When to enforce reset. Null or past means execute immediately.")
    private Instant effectiveAt;

    @Size(max = 500)
    @Schema(description = "Optional incident reason for audit trail")
    private String reason;
}

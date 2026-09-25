package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request to create a new billing service")
public class CreateServiceRequest {

    @NotBlank(message = "Service code is required")
    @Schema(description = "Unique service code/identifier (REQUIRED)", example = "PSY-60", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceCode;

    @NotBlank(message = "Service name is required")
    @Schema(description = "Service name (REQUIRED)", example = "Psychotherapy Session - 60 minutes", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serviceName;

    @Schema(description = "Service description (optional)", example = "Standard 60-minute psychotherapy session")
    private String description;

    @Schema(description = "Duration of service in minutes (optional)", example = "60")
    private Integer durationInMinutes;

    @NotNull(message = "Base rate is required")
    @Schema(description = "Base rate/price for the service (REQUIRED)", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED, type = "number")
    private BigDecimal baseRate;

    @Schema(description = "Whether the service is active (optional, defaults to true)", example = "true", defaultValue = "true")
    private Boolean isActive = true;

    @Schema(description = "Whether service is visible to therapists (optional, defaults to true)", example = "true", defaultValue = "true")
    private Boolean therapistVisible = true;

    @Schema(description = "Whether service is visible in client portal (optional, defaults to false)", example = "false", defaultValue = "false")
    private Boolean clientPortalVisible = false;

    @Schema(description = "Whether service is enabled for the public marketing site (optional, defaults to false)", example = "false", defaultValue = "false")
    private Boolean publicSiteEnabled = false;
}


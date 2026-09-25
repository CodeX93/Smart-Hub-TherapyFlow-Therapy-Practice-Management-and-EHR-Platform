package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request to update an existing billing service. All fields are optional.")
public class UpdateServiceRequest {

    @Schema(description = "Unique service code/identifier", example = "PSY-60")
    private String serviceCode;

    @Schema(description = "Service name", example = "Psychotherapy Session - 60 minutes")
    private String serviceName;

    @Schema(description = "Service description", example = "Standard 60-minute psychotherapy session")
    private String description;

    @Schema(description = "Duration of service in minutes", example = "60")
    private Integer durationInMinutes;

    @Schema(description = "Base rate/price for the service", example = "150.00", type = "number")
    private BigDecimal baseRate;

    @Schema(description = "Whether the service is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Whether service is visible to therapists", example = "true")
    private Boolean therapistVisible;

    @Schema(description = "Whether service is visible in client portal", example = "false")
    private Boolean clientPortalVisible;

    @Schema(description = "Whether service is enabled for the public marketing site", example = "false")
    private Boolean publicSiteEnabled;
}

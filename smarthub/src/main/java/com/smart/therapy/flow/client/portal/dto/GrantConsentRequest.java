package com.smart.therapy.flow.client.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to grant consent")
public class GrantConsentRequest {
    @NotBlank(message = "Consent type is required")
    @Schema(description = "Type of consent (REQUIRED)", example = "treatment", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"treatment", "privacy", "billing", "communication"})
    private String consentType;
    
    @NotNull(message = "Granted status is required")
    @Schema(description = "Whether consent is granted (REQUIRED)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean granted;
    
    @NotBlank(message = "Consent version is required")
    @Schema(description = "Version of the consent document (REQUIRED)", example = "1.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private String consentVersion;
}


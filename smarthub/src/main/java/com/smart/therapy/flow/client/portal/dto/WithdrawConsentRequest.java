package com.smart.therapy.flow.client.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to withdraw consent")
public class WithdrawConsentRequest {
    @NotBlank(message = "Consent type is required")
    @Schema(description = "Type of consent to withdraw (REQUIRED)", example = "treatment", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"treatment", "privacy", "billing", "communication"})
    private String consentType;
}


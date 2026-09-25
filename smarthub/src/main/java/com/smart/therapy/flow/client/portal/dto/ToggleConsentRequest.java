package com.smart.therapy.flow.client.portal.dto;

import com.smart.therapy.flow.client.enums.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to toggle consent (grant or deny)")
public class ToggleConsentRequest {
    
    @NotNull(message = "Consent type is required")
    @Schema(
            description = "Type of consent to toggle (REQUIRED)", 
            example = "AI_PROCESSING",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {
                "TREATMENT", "TELEHEALTH", "HIPAA_PRIVACY", "HIPAA_AUTHORIZATION",
                "AI_PROCESSING", "ELECTRONIC_RECORDS", "INSURANCE_SHARING", "PAYMENT_AUTHORIZATION",
                "RESEARCH", "TRAINING", "PHOTOGRAPHY", "AUDIO_RECORDING", "VIDEO_RECORDING",
                "EMAIL_COMMUNICATION", "SMS_COMMUNICATION", "PARENTAL_CONSENT",
                "EMERGENCY_CONTACT", "DATA_SHARING", "MARKETING", "OTHER"
            }
    )
    private ConsentType consentType;
    
    @NotNull(message = "Granted status is required")
    @Schema(
            description = "Whether to grant (true) or deny/withdraw (false) consent (REQUIRED)", 
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean granted;
    
    @Schema(
            description = "Version of the consent document (optional, defaults to '1.0' if not provided)", 
            example = "1.0"
    )
    private String consentVersion;
}

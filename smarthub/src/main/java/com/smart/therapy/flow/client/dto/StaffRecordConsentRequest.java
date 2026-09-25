package com.smart.therapy.flow.client.dto;

import com.smart.therapy.flow.client.enums.ConsentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Staff-recorded consent request")
public class StaffRecordConsentRequest {

    @NotNull
    @Schema(description = "Consent type", requiredMode = Schema.RequiredMode.REQUIRED)
    private ConsentType consentType;

    @NotNull
    @Schema(description = "Grant true, withdraw false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean granted;

    @Schema(description = "Consent form version", example = "1.0")
    private String consentVersion;

    @Schema(description = "Source channel", example = "signed_consent_form")
    private String source;

    @Schema(description = "Optional notes")
    private String notes;

    @Schema(description = "Audit/compliance reason")
    private String auditReason;
}

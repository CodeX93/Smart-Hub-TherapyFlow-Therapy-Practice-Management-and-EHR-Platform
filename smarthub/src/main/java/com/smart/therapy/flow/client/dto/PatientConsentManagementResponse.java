package com.smart.therapy.flow.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Patient Consent Management List (Admin View)
 * Shows summary of all clients with their consent statuses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Patient Consent Management response with client information and consent statuses")
public class PatientConsentManagementResponse {

    @Schema(description = "Client ID", example = "1")
    private Long clientId;

    @Schema(description = "Client full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Client email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Whether client has portal access", example = "true")
    private Boolean portalAccess;

    @Schema(description = "AI Processing consent status", example = "GRANTED", allowableValues = {"GRANTED", "DENIED", "NOT_SET"})
    private ConsentStatus aiProcessing;

    @Schema(description = "Data Sharing consent status", example = "GRANTED", allowableValues = {"GRANTED", "DENIED", "NOT_SET"})
    private ConsentStatus dataSharing;

    @Schema(description = "Research Participation consent status", example = "DENIED", allowableValues = {"GRANTED", "DENIED", "NOT_SET"})
    private ConsentStatus research;

    @Schema(description = "Marketing Communications consent status", example = "GRANTED", allowableValues = {"GRANTED", "DENIED", "NOT_SET"})
    private ConsentStatus marketing;

    @Schema(description = "List of all consents for this client")
    private List<ConsentDetail> allConsents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Detailed consent information")
    public static class ConsentDetail {
        @Schema(description = "Consent ID", example = "1")
        private Long id;

        @Schema(description = "Consent type", example = "AI Processing Consent")
        private String consentType;

        @Schema(description = "Consent status", example = "GRANTED")
        private ConsentStatus status;

        @Schema(description = "Consent version", example = "1.0")
        private String version;

        @Schema(description = "Granted timestamp")
        private java.time.Instant grantedAt;

        @Schema(description = "Withdrawn timestamp")
        private java.time.Instant withdrawnAt;
    }

    public enum ConsentStatus {
        GRANTED,
        DENIED,
        NOT_SET
    }
}

package com.smart.therapy.flow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Practice configuration response")
public class PracticeConfigurationResponse {

    @Schema(description = "Configuration ID", example = "1")
    private Long id;

    @Schema(description = "Practice name", example = "SmartHub Healthcare Services")
    private String practiceName;

    @Schema(description = "Practice address", example = "123 Healthcare Ave, Suite 100")
    private String practiceAddress;

    @Schema(description = "Practice phone number", example = "(555) 123-4567")
    private String practicePhone;

    @Schema(description = "Practice email address", example = "contact@smarthub.com")
    private String practiceEmail;

    @Schema(description = "Practice website URL", example = "https://resiliencecrm.com")
    private String practiceWebsite;

    @Schema(description = "Tax ID", example = "12-3456789")
    private String taxId;

    @Schema(description = "License number", example = "PSY-12345-CA")
    private String licenseNumber;

    @Schema(description = "License state", example = "California")
    private String licenseState;

    @Schema(description = "NPI number", example = "1234567890")
    private String npiNumber;

    @Schema(description = "Practice description", example = "Professional Mental Health Services")
    private String description;

    @Schema(description = "Practice subtitle", example = "Licensed Clinical Practice")
    private String subtitle;

    @Schema(description = "Practice timezone (IANA timezone ID)", example = "America/New_York")
    private String timezone;

    @Schema(description = "Created at", example = "2025-01-01T00:00:00Z")
    private Instant createdAt;

    @Schema(description = "Updated at", example = "2025-01-01T00:00:00Z")
    private Instant updatedAt;
}

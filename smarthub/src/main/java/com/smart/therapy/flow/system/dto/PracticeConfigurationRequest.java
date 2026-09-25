package com.smart.therapy.flow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to update practice configuration")
public class PracticeConfigurationRequest {

    @NotBlank(message = "Practice name is required")
    @Size(max = 255, message = "Practice name cannot exceed 255 characters")
    @Schema(description = "Practice name", example = "SmartHub Healthcare Services", requiredMode = Schema.RequiredMode.REQUIRED)
    private String practiceName;

    @Size(max = 1000, message = "Practice address cannot exceed 1000 characters")
    @Schema(description = "Practice address", example = "123 Healthcare Ave, Suite 100\nMental Health City, CA 90210")
    private String practiceAddress;

    @Pattern(regexp = "^[\\+]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[0-9]{1,9}$", 
             message = "Invalid phone number format")
    @Size(max = 50, message = "Phone number cannot exceed 50 characters")
    @Schema(description = "Practice phone number", example = "(555) 123-4567")
    private String practicePhone;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    @Schema(description = "Practice email address", example = "contact@smarthub.com")
    private String practiceEmail;

    @Pattern(regexp = "^(https?://)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([/\\w \\.-]*)*/?$", 
             message = "Invalid website URL format")
    @Size(max = 255, message = "Website URL cannot exceed 255 characters")
    @Schema(description = "Practice website URL", example = "https://resiliencecrm.com")
    private String practiceWebsite;

    @Pattern(regexp = "^[0-9]{2}-[0-9]{7}$|^[0-9]{9}$", 
             message = "Tax ID must be in format XX-XXXXXXX or 9 digits")
    @Size(max = 50, message = "Tax ID cannot exceed 50 characters")
    @Schema(description = "Tax ID (SSN/EIN format)", example = "12-3456789")
    private String taxId;

    @Size(max = 100, message = "License number cannot exceed 100 characters")
    @Schema(description = "License number", example = "PSY-12345-CA")
    private String licenseNumber;

    @Size(max = 50, message = "License state cannot exceed 50 characters")
    @Schema(description = "License state", example = "California")
    private String licenseState;

    @Pattern(regexp = "^[0-9]{10}$", message = "NPI number must be exactly 10 digits")
    @Size(max = 50, message = "NPI number cannot exceed 50 characters")
    @Schema(description = "NPI number (10 digits)", example = "1234567890")
    private String npiNumber;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Practice description", example = "Professional Mental Health Services")
    private String description;

    @Size(max = 255, message = "Subtitle cannot exceed 255 characters")
    @Schema(description = "Practice subtitle", example = "Licensed Clinical Practice")
    private String subtitle;

    @Pattern(regexp = "^[A-Za-z_]+/[A-Za-z_]+$", 
             message = "Timezone must be a valid IANA timezone ID (e.g., America/New_York)")
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    @Schema(description = "Practice timezone (IANA timezone ID)", example = "America/New_York")
    private String timezone;
}

package com.smart.therapy.flow.client.dto;

import com.smart.therapy.flow.client.enums.ContactType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a client contact")
public class ClientContactRequest {

    @NotNull(message = "Contact type is required")
    @Schema(description = "Type of contact (EMAIL, PHONE, WORK_PHONE, MOBILE_PHONE, EMERGENCY_CONTACT, etc.)", 
            example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
    private ContactType contactType;

    @NotBlank(message = "Contact value is required")
    @Size(max = 500, message = "Contact value must not exceed 500 characters")
    @Schema(description = "Contact value (email address or phone number)", 
            example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactValue;

    @Schema(description = "Whether this is the primary contact of this type", example = "true")
    private Boolean isPrimary;

    @Schema(description = "Whether this contact has been verified", example = "false")
    private Boolean isVerified;

    @Size(max = 255, message = "Contact person name must not exceed 255 characters")
    @Schema(description = "Contact person name (for emergency contacts)", example = "Jane Doe")
    private String contactPersonName;

    @Size(max = 100, message = "Relationship must not exceed 100 characters")
    @Schema(description = "Relationship to client (for emergency contacts)", example = "Spouse")
    private String relationship;
}


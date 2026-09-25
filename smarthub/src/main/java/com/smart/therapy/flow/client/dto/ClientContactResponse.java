package com.smart.therapy.flow.client.dto;

import com.smart.therapy.flow.client.enums.ContactType;
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
@Schema(description = "Client contact information")
public class ClientContactResponse {

    @Schema(description = "Contact ID", example = "1")
    private Long id;

    @Schema(description = "Client ID", example = "1")
    private Long clientId;

    @Schema(description = "Type of contact", example = "EMAIL")
    private ContactType contactType;

    @Schema(description = "Contact value (email or phone)", example = "john.doe@example.com")
    private String contactValue;

    @Schema(description = "Whether this is the primary contact of this type", example = "true")
    private Boolean isPrimary;

    @Schema(description = "Whether this contact has been verified", example = "false")
    private Boolean isVerified;

    @Schema(description = "Contact person name (for emergency contacts)", example = "John Doe")
    private String contactPersonName;

    @Schema(description = "Relationship to client", example = "Self")
    private String relationship;

    @Schema(description = "When this contact was created")
    private Instant createdAt;

    @Schema(description = "When this contact was last updated")
    private Instant updatedAt;
}


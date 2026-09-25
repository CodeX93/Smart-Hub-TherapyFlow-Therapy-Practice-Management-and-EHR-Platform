package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to create/upload a document. File is uploaded via multipart/form-data.")
public class CreateDocumentRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "ID of the client (REQUIRED)", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clientId;

    @Schema(description = "Document type (optional)", example = "consent", allowableValues = {"consent", "intake", "release", "assessment", "report", "other"})
    private String documentType;

    @Schema(description = "Document category (optional)", example = "Legal")
    private String category;

    @Schema(description = "Document description (optional)", example = "Signed consent form")
    private String description;

    @Schema(description = "Whether document needs review (optional, default: false)", example = "false", defaultValue = "false")
    private Boolean needsReview = false;

    @Schema(description = "Whether to share document with client (optional, default: false)", example = "false", defaultValue = "false")
    private Boolean shareWithClient = false;

    // File will be handled via MultipartFile in controller
}


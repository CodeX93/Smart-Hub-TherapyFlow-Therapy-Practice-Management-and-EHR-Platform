package com.smart.therapy.flow.document.dto;

import com.smart.therapy.flow.document.enums.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to review a document")
public class ReviewDocumentRequest {
    
    @Schema(
            description = "Review status (REQUIRED). Overdue is system-derived and cannot be set directly.",
            example = "approved",
            allowableValues = {"PENDING", "THERAPIST_REVIEW", "SUPERVISOR_REVIEW", "APPROVED", "REJECTED"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private ReviewStatus reviewStatus;

    @Schema(
            description = "Compatibility action intent (optional): reviewed, rejected, pending_review",
            example = "reviewed",
            allowableValues = {"reviewed", "rejected", "pending_review"}
    )
    private String action;
    
    @Schema(description = "Review notes (optional)", example = "Document approved after verification")
    private String reviewNotes; // Optional notes about the review
}


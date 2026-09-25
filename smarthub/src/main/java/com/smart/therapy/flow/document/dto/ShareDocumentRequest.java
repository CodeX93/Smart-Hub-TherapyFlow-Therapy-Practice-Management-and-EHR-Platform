package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to update document sharing settings")
public class ShareDocumentRequest {

    @NotNull(message = "Share flag is required")
    @Schema(description = "Whether to share document with client in portal (REQUIRED)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean shareWithClient;
}


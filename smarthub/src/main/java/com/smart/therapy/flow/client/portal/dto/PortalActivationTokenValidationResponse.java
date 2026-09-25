package com.smart.therapy.flow.client.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Client portal activation token validation result")
public class PortalActivationTokenValidationResponse {

    @Schema(description = "Whether the activation token exists and has not expired")
    boolean valid;

    @Schema(description = "Human-readable message when invalid")
    String message;
}

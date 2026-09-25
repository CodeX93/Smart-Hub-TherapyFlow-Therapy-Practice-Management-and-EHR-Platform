package com.smart.therapy.flow.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ZoomCredentialsRequest {

    @NotBlank(message = "Zoom account ID is required")
    private String accountId;

    @NotBlank(message = "Zoom client ID is required")
    private String clientId;

    @NotBlank(message = "Zoom client secret is required")
    private String clientSecret;
}



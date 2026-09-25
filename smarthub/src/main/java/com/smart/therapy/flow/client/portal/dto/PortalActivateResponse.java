package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalActivateResponse {
    private String message;
    private PortalClientResponse client;
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn; // Token expiration in seconds
}


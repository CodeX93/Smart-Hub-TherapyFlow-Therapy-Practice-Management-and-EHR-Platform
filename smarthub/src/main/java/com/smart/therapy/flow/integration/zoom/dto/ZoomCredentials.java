package com.smart.therapy.flow.integration.zoom.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ZoomCredentials {
    private String accountId;
    private String clientId;
    private String clientSecret;
    private String accessToken;
    private Instant tokenExpiry;
}


package com.smart.therapy.flow.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ZoomCredentialsResponse {

    private boolean configured;
    private Instant lastUpdatedAt;
    private String accountId;
    private String clientId;
    private String clientSecret;
}



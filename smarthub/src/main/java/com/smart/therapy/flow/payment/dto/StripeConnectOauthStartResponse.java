package com.smart.therapy.flow.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class StripeConnectOauthStartResponse {
    private String authorizeUrl;
    private Instant stateExpiresAt;
}

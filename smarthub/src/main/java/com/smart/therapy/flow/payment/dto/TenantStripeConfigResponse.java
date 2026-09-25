package com.smart.therapy.flow.payment.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class TenantStripeConfigResponse {
    Long organisationId;
    String publishableKey;
    Boolean secretKeyConfigured;
    String webhookEndpointUrl;
    Boolean webhookSecretConfigured;
    Instant lastUpdatedAt;
}


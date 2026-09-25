package com.smart.therapy.flow.superadmin.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SuperAdminIntegrationResponse {
    String key;
    Boolean enabled;
    String clientId;
    String publishableKey;
    String secret;
    String connectClientSecret;
    String connectWebhookSecret;
    String platformWebhookSecret;
    String webhookUrl;
    Instant lastConfiguredAt;
}

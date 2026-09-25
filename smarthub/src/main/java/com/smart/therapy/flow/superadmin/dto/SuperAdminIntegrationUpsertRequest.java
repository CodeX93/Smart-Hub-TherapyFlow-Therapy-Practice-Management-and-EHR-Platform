package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SuperAdminIntegrationUpsertRequest {
    @NotNull(message = "enabled is required")
    private Boolean enabled;
    private String clientId;
    private String publishableKey;
    private String secret;
    /** Stripe Connect OAuth client secret (distinct from platform sk_ secret). */
    private String connectClientSecret;
    /** Stripe Connect webhook signing secret (whsec_). */
    private String connectWebhookSecret;
    /** Platform subscription webhook signing secret for /stripe/webhook/platform (whsec_). */
    private String platformWebhookSecret;
    private String webhookUrl;
}

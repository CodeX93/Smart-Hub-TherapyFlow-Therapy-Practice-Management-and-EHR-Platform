package com.smart.therapy.flow.payment.dto;

import lombok.Data;

@Data
public class TenantStripeConfigUpsertRequest {
    private String publishableKey;
    private String secretKey;
    private String webhookEndpointUrl;
    private String webhookSecret;
}


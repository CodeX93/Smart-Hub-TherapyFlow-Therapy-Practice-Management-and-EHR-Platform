package com.smart.therapy.flow.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscriptionCheckoutResponse {
    private String checkoutUrl;
    private String sessionId;
}

package com.smart.therapy.flow.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripeCheckoutResponse {
    private String sessionId;
    private String checkoutUrl;
}


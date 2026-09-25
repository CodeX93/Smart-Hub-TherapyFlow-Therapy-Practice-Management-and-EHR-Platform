package com.smart.therapy.flow.billing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantSubscriptionInvoicePayResponse {
    private Long invoiceId;
    private String providerInvoiceId;
    private String providerStatus;
    private String paymentUrl;
}

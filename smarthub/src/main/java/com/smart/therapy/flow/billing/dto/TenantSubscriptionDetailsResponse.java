package com.smart.therapy.flow.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscriptionDetailsResponse {

    private Long organisationId;
    private Long subscriptionId;
    private String planCode;
    private String planName;
    private String planStatus;
    private String subscriptionStatus;
    private String billingCycle;
    private BigDecimal priceAtTime;
    private Instant startAt;
    private Instant endAt;
    private Instant trialEndAt;
    private boolean trialing;
    private String usagePeriod;
    private List<FeatureEntitlement> features;
    private boolean auditExportEnabled;
    private Integer auditExportLimit;
    private Long auditExportUsage;
    private String billingMode;
    private boolean providerBillingConfigured;
    private boolean accessRestricted;
    private boolean pastDue;
    private boolean cancelled;
    private Instant currentPeriodEnd;
    private String nextAction;
    private Long pendingInvoiceId;
    private boolean canSelfServeRenew;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureEntitlement {
        private String featureCode;
        private String featureName;
        private String description;
        private boolean enabled;
        private Integer usageLimit;
        private Long currentUsage;
        private boolean coreFeature;
        private String valueType;
    }
}

package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Strict subscription response with limits and usage")
@Data
public class SuperAdminSubscriptionDetailsResponse {
    private Long organisationId;
    private Long subscriptionId;
    private String plan;
    private String status;
    private String billingCycle;
    private BigDecimal priceAtTime;
    private Instant startAt;
    private Instant endAt;
    private Instant trialEndsAt;
    private String providerCustomerId;
    private String providerSubscriptionId;
    private UserLimits userLimits;
    private UserUsage userUsage;

    @Data
    public static class UserLimits {
        private Integer therapistLimit;
        private Integer supervisorLimit;
        private Integer clientLimit;
    }

    @Data
    public static class UserUsage {
        private long therapistUsers;
        private long supervisorUsers;
        private long clientUsers;
        private long totalUsers;
    }
}

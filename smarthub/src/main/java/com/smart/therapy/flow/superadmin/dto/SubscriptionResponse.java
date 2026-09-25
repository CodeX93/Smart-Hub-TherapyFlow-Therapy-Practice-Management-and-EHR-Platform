package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Schema(description = "Subscription details with effective features")
@Data
public class SubscriptionResponse {
private Long organisationId;
private Long subscriptionId;
private String planName;
private String status;
private String billingCycle;
private BigDecimal priceAtTime;
private Instant startAt;
private Instant endAt;
private Instant trialEndAt;
private String providerCustomerId;
private String providerSubscriptionId;
private List<FeatureStateResponse> features;
}

package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Request for creating/updating subscription")
@Data
public class UpsertSubscriptionRequest {
private String planName;
private String billingCycle;
private Integer trialDays;
private String status;
private String providerCustomerId;
private String providerSubscriptionId;
}

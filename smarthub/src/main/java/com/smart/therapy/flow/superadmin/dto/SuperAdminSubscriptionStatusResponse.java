package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SuperAdminSubscriptionStatusResponse {

    private Long organisationId;
    private Long subscriptionId;
    private String plan;
    private String status;
    private String organisationStatus;
    private Instant startAt;
    private Instant endAt;
    private Instant trialEndsAt;

    private boolean current;
    private boolean trialExpired;
    private boolean pastDue;
    private boolean cancelled;
    private boolean accessRestricted;

    private boolean providerCustomerConfigured;
    private boolean providerSubscriptionConfigured;
    private boolean providerBillingConfigured;
    private String billingMode;
}


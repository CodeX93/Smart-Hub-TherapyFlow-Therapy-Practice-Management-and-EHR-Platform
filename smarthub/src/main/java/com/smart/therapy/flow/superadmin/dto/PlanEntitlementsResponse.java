package com.smart.therapy.flow.superadmin.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class PlanEntitlementsResponse {
    String plan;
    List<PlanEntitlementItemResponse> features;
    Instant updatedAt;
}

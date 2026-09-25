package com.smart.therapy.flow.superadmin.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlanEntitlementItemResponse {
    String key;
    Boolean enabled;
    Integer limit;
}

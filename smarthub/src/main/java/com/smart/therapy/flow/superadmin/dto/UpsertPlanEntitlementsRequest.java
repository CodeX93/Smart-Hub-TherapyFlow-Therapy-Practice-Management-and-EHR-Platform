package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Plan entitlement update request")
@Data
public class UpsertPlanEntitlementsRequest {
private List<PlanEntitlementItem> items;
private List<PlanEntitlementItem> features;

public List<PlanEntitlementItem> resolveItems() {
    if (features != null) {
        return features;
    }
    return items;
}
}

package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Plan entitlement response")
@Data
public class PlanEntitlementResponse {
private String planName;
private String featureCode;
private Boolean enabled;
private Integer usageLimit;
private Boolean trialAvailable;
private Instant effectiveFrom;
}

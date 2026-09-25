package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Single rollout rule")
@Data
public class RolloutRuleRequest {
@Schema(example = "GLOBAL", description = "Scope is ignored for global rollout endpoints and forced to GLOBAL")
private String scope;

@Schema(example = "null")
private Long targetId;

@Schema(example = "null")
private String targetKey;

@Schema(example = "CLIENT_PORTAL")
private String featureKey;

@Schema(example = "true")
private Boolean enabled;

@Schema(example = "null", description = "Usage cap; null means unlimited/no override")
private Integer usageLimit;

@Schema(example = "2026-04-03T00:00:00Z")
private Instant startAt;

@Schema(example = "2026-06-01T00:00:00Z")
private Instant endAt;
}

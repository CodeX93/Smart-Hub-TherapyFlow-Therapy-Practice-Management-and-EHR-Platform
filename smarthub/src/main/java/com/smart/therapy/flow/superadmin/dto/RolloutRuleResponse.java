package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Rollout rule response")
@Data
public class RolloutRuleResponse {
private Long id;
private Long organisationId;
private String scope;
private Long targetId;
private String targetKey;
private String featureKey;
private boolean enabled;
private Integer usageLimit;
private Instant startAt;
private Instant endAt;
private Instant updatedAt;
}

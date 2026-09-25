package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "Batch rollout upsert request")
@Data
public class UpsertRolloutsRequest {
@Schema(description = "Rules to create/update",
        example = "[{\"featureKey\":\"CLIENT_PORTAL\",\"enabled\":true,\"usageLimit\":null,\"startAt\":\"2026-04-03T00:00:00Z\",\"endAt\":null}]")
private List<RolloutRuleRequest> rules;
}

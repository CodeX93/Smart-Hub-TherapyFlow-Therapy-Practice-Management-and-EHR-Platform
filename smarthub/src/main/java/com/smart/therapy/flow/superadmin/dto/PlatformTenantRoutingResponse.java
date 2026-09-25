package com.smart.therapy.flow.superadmin.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class PlatformTenantRoutingResponse {
    Boolean emailAutoRouting;
    Boolean pathBasedRouting;
    String pathPrefix;
    String orgIdentifier;
    Instant updatedAt;
}


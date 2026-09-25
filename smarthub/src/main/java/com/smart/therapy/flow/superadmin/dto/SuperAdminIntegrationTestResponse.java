package com.smart.therapy.flow.superadmin.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SuperAdminIntegrationTestResponse {
    boolean success;
    long latencyMs;
    String error;
}


package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.util.List;

@Data
public class SuperAdminUsageResponse {
    private Long organisationId;
    private String period;
    private String targetKey;
    private List<SuperAdminUsageMetricResponse> metrics;
}

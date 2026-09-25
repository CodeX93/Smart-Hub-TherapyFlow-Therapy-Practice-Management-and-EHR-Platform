package com.smart.therapy.flow.superadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminUsageMetricResponse {
    private String featureKey;
    private long used;
    private Integer limit;
}

package com.smart.therapy.flow.superadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminUsageTargetResponse {
    private String targetKey;
    private Long userId;
    private String userName;
    private String userEmail;
    private List<SuperAdminUsageMetricResponse> metrics;
}

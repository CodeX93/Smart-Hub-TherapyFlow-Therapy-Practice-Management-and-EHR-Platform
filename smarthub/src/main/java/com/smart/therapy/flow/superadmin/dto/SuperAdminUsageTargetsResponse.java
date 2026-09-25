package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.util.List;

@Data
public class SuperAdminUsageTargetsResponse {
    private Long organisationId;
    private String period;
    private List<SuperAdminUsageTargetResponse> targets;
}

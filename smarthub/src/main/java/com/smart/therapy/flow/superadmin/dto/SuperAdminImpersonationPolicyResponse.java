package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SuperAdminImpersonationPolicyResponse {
    private Long id;
    private Boolean enabled;
    private Boolean requireReason;
    private Integer minReasonLength;
    private Integer maxDurationMinutes;
    private Boolean allowCrossOrganisation;
    private List<String> allowedRoles;
    private List<String> deniedRoles;
    private List<Long> allowedOrgIds;
    private List<Long> deniedOrgIds;
    private Long updatedByAuthId;
    private Instant createdAt;
    private Instant updatedAt;
}

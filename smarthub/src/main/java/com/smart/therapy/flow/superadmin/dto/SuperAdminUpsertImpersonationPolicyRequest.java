package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class SuperAdminUpsertImpersonationPolicyRequest {
    private Boolean enabled;
    private Boolean requireReason;
    @Positive
    private Integer minReasonLength;
    @Positive
    private Integer maxDurationMinutes;
    private Boolean allowCrossOrganisation;
    private List<String> allowedRoles;
    private List<String> deniedRoles;
    private List<Long> allowedOrgIds;
    private List<Long> deniedOrgIds;
}

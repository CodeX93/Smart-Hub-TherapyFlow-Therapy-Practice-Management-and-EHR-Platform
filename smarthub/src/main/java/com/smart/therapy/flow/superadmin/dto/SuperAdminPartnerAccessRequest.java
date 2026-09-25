package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SuperAdminPartnerAccessRequest {
    @NotEmpty
    private List<Long> organisationIds;
    private Map<String, Object> featureFlags;
    private Boolean active;
}

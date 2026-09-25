package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class SuperAdminPartnerAccessResponse {
    private Long id;
    private String partnerId;
    private Long organisationId;
    private Map<String, Object> featureFlags;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

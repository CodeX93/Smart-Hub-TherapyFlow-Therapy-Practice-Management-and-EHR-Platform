package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tenant HIPAA audit log entry visible to platform super-admin/auditor")
public class SuperAdminHipaaAuditLogResponse {
    private Long organisationId;
    private String organisationName;
    private String tenantSchema;

    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String rawAction;
    private String logLevel;
    private String result;
    private String resourceType;
    private String resourceId;
    private Long clientId;
    private String clientName;
    private String ipAddress;
    private String riskLevel;
    private Boolean hipaaRelevant;
    private String details;
    private Long userActivityCount;
    private Instant timestamp;
}

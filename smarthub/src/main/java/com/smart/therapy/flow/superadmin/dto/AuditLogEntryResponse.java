package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Platform audit log entry (read-only)")
@Data
public class AuditLogEntryResponse {
private Long id;
private Long authId;
private String action;
private String logLevel;
private String resourceType;
private String resourceId;
private Long organisationId;
private String organisationName;
private String details;
private String actionSummary;
private Object before;
private Object after;
private Instant createdAt;
}

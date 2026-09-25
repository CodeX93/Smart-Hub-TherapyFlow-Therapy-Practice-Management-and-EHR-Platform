package com.smart.therapy.flow.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for audit log response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
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
    /** Decrypted MRN (clients.clientId). Never patient name. */
    private String clientMrn;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private String riskLevel;
    private Boolean hipaaRelevant;
    private String details; // JSON string
    private Long userActivityCount;
    private String dataFields; // JSON string
    private String accessReason;
    private Instant timestamp;
}

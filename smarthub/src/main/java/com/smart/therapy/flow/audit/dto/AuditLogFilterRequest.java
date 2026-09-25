package com.smart.therapy.flow.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for filtering audit logs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFilterRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String riskLevel; // all, low, medium, high, critical
    private Boolean hipaaOnly;
    private String action; // all or specific action
    private String username; // partial match
    private Long clientId;
    private String resourceType; // client, session, document, etc.
    private String logLevel; // all, trace, debug, info, warn, error
}

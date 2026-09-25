package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_timestamp", columnList = "user_id, timestamp"),
    @Index(name = "idx_audit_entity_type", columnList = "entity_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 100)
    private String username; // Store username for records even if user deleted

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String result = "success";

    @Column(name = "resource_type", length = 50)
    private String resourceType; // 'client', 'session', 'document', etc.

    @Column(name = "resource_id", length = 50)
    private String resourceId; // ID of the resource accessed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client; // PHI access tracking

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // IPv4/IPv6

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "session_id", length = 255)
    private String sessionId; // Browser/app session ID

    @Column(columnDefinition = "TEXT")
    private String details; // JSON with additional context

    @Column(name = "risk_level", length = 20, nullable = false)
    @Builder.Default
    private String riskLevel = "low"; // low, medium, high, critical

    @Column(nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();

    // Additional HIPAA-specific fields
    @Column(name = "hipaa_relevant", nullable = false)
    @Builder.Default
    private Boolean hipaaRelevant = false; // PHI access flag

    @Column(name = "data_fields", columnDefinition = "TEXT")
    private String dataFields; // JSON array of specific PHI fields accessed

    @Column(name = "access_reason", columnDefinition = "TEXT")
    private String accessReason; // Reason for accessing PHI
    
    // NOTE: Database also has entity_type and entity_id columns from V20250101
    // These are legacy columns, kept for backward compatibility
    // Use resourceType and resourceId in new code
    @Column(name = "entity_type", length = 100)
    private String entityType; // Legacy column for backward compatibility
    
    @Column(name = "entity_id")
    private Long entityId; // Legacy column for backward compatibility
    
    // Golden Rule: Before/After state snapshots for compliance (HIPAA/GDPR)
    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState; // JSON snapshot of entity state before operation (null for creates)
    
    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState; // JSON snapshot of entity state after operation
    
    @Column(name = "changed_fields", columnDefinition = "TEXT")
    private String changedFields; // JSON array of field names that were modified

    @Column(name = "impersonator_auth_id")
    private Long impersonatorAuthId; // Platform super-admin auth id during impersonation

    @PrePersist
    @PreUpdate
    private void applyDefaults() {
        if ((username == null || username.trim().isEmpty()) && user != null) {
            // Prefer loginIdentifier (stable staff username) over email for actor consistency.
            if (user.getAuthIdentity() != null
                    && user.getAuthIdentity().getLoginIdentifier() != null
                    && !user.getAuthIdentity().getLoginIdentifier().trim().isEmpty()) {
                username = user.getAuthIdentity().getLoginIdentifier().trim();
            } else if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                username = user.getEmail().trim();
            } else if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
                username = user.getFullName().trim();
            }
        }
        if (result == null || result.trim().isEmpty()) {
            result = "success";
        }
        if (riskLevel == null || riskLevel.trim().isEmpty()) {
            riskLevel = "low";
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (hipaaRelevant == null) {
            hipaaRelevant = false;
        }
    }
}

package com.smart.therapy.flow.audit.support;

import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;

import java.time.Instant;

/**
 * Scalar draft for audit persistence. Domain services should build this (or an
 * {@link AuditLog}) and pass it to {@link com.smart.therapy.flow.audit.service.AuditLogService}
 * instead of saving to {@code AuditLogRepository} themselves.
 * <p>
 * Actor/client are referenced by id only; the write transaction validates and binds those references.
 */
public final class AuditEventDraft {

    private Long actorId;
    private Long clientId;
    private String username;
    private String action;
    private String result = "success";
    private String resourceType;
    private String resourceId;
    private String ipAddress;
    private String userAgent;
    private boolean hipaaRelevant;
    private String riskLevel;
    private String details;
    private String beforeState;
    private String afterState;
    private String changedFields;
    private String accessReason;
    private Long impersonatorAuthId;

    private AuditEventDraft() {
    }

    public static AuditEventDraft of(String action, String resourceType) {
        AuditEventDraft draft = new AuditEventDraft();
        draft.action = action;
        draft.resourceType = resourceType;
        return draft;
    }

    public AuditEventDraft actorId(Long actorId) {
        this.actorId = actorId;
        return this;
    }

    public AuditEventDraft clientId(Long clientId) {
        this.clientId = clientId;
        return this;
    }

    public AuditEventDraft username(String username) {
        this.username = username;
        return this;
    }

    public AuditEventDraft resourceId(Long resourceId) {
        this.resourceId = resourceId != null ? String.valueOf(resourceId) : null;
        return this;
    }

    public AuditEventDraft resourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public AuditEventDraft ipAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    public AuditEventDraft userAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public AuditEventDraft hipaaRelevant(boolean hipaaRelevant) {
        this.hipaaRelevant = hipaaRelevant;
        return this;
    }

    public AuditEventDraft riskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
        return this;
    }

    public AuditEventDraft result(String result) {
        this.result = result;
        return this;
    }

    public AuditEventDraft details(String details) {
        this.details = details;
        return this;
    }

    public AuditEventDraft beforeState(String beforeState) {
        this.beforeState = beforeState;
        return this;
    }

    public AuditEventDraft afterState(String afterState) {
        this.afterState = afterState;
        return this;
    }

    public AuditEventDraft changedFields(String changedFields) {
        this.changedFields = changedFields;
        return this;
    }

    public AuditEventDraft accessReason(String accessReason) {
        this.accessReason = accessReason;
        return this;
    }

    public AuditEventDraft impersonatorAuthId(Long impersonatorAuthId) {
        this.impersonatorAuthId = impersonatorAuthId;
        return this;
    }

    public Long getActorId() {
        return actorId;
    }

    public AuditLog toAuditLog() {
        String resolvedRisk = riskLevel != null ? riskLevel : (hipaaRelevant ? "medium" : "low");
        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action)
                .result(result != null ? result : "success")
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .hipaaRelevant(hipaaRelevant)
                .riskLevel(resolvedRisk)
                .details(details)
                .beforeState(beforeState)
                .afterState(afterState)
                .changedFields(changedFields)
                .accessReason(accessReason)
                .impersonatorAuthId(impersonatorAuthId)
                .timestamp(Instant.now())
                .build();
        if (actorId != null) {
            User userRef = new User();
            userRef.setId(actorId);
            auditLog.setUser(userRef);
        }
        if (clientId != null) {
            Client clientRef = new Client();
            clientRef.setId(clientId);
            auditLog.setClient(clientRef);
        }
        return auditLog;
    }
}

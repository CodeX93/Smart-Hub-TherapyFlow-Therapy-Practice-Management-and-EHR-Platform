package com.smart.therapy.flow.common.entity;

import jakarta.persistence.*;
import java.time.Instant;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * BaseEntity - common superclass for all JPA entities.
 * Automatically manages audit fields (created/updated by + timestamps).
 * Schema-per-tenant: tenant-scoped entities live in tenant_XXXX schema (no organisation_id).
 * Global entities that need organisation (e.g. AuthIdentity, LoginAttempt, OrgSubscription) declare it explicitly.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "createdat", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updatedat", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(nullable = false, name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(nullable = false, name = "updated_by")
    private Long updatedBy;

    @Version
    private Long version;

    // ========== Soft Delete for HIPAA/GDPR Compliance ==========
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Pre-persist callback to ensure createdAt and updatedAt are always set.
     * This is a fallback in case JPA auditing doesn't trigger (e.g., when using
     * builders).
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        // Fallback for auditing fields when not set by AuditorAware (e.g., system
        // operations)
        if (this.createdBy == null) {
            this.createdBy = 0L; // System user
        }
        if (this.updatedBy == null) {
            this.updatedBy = 0L; // System user
        }
    }

    /**
     * Pre-update callback to ensure updatedAt is always set.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        // Fallback for updatedBy when not set by AuditorAware
        if (this.updatedBy == null) {
            this.updatedBy = 0L; // System user
        }
    }

    /**
     * Soft delete this entity - preserves data for HIPAA/GDPR compliance
     */
    public void softDelete() {
        if (!Boolean.TRUE.equals(this.isDeleted)) {
            this.isDeleted = true;
            this.deletedAt = Instant.now();
        }
    }

    /**
     * Restore a soft-deleted entity (admin function)
     */
    public void restore() {
        if (Boolean.TRUE.equals(this.isDeleted)) {
            this.isDeleted = false;
            this.deletedAt = null;
        }
    }
}
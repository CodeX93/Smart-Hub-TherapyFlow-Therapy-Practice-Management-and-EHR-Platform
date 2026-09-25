package com.smart.therapy.flow.organisation.entity;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Blocks an auth identity from accessing a specific organisation.
 * Stored in public schema.
 */
@Entity
@Table(name = "user_organisation_access_blocks", schema = "public", indexes = {
        @Index(name = "idx_user_org_blocks_auth", columnList = "auth_id"),
        @Index(name = "idx_user_org_blocks_org", columnList = "organisation_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_org_blocks_auth_org", columnNames = {"auth_id", "organisation_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOrganisationAccessBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_id", nullable = false)
    private AuthIdentity auth;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "blocked_by")
    private Long blockedBy;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (blockedAt == null) {
            blockedAt = now;
        }
    }
}

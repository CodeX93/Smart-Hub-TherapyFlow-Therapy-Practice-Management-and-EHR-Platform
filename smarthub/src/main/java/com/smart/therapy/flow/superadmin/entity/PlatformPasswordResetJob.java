package com.smart.therapy.flow.superadmin.entity;

import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "platform_password_reset_jobs", schema = "public", indexes = {
        @Index(name = "idx_platform_password_reset_jobs_status_effective", columnList = "status, effective_at"),
        @Index(name = "idx_platform_password_reset_jobs_org", columnList = "organisation_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class PlatformPasswordResetJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "requested_by_auth_id")
    private Long requestedByAuthId;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

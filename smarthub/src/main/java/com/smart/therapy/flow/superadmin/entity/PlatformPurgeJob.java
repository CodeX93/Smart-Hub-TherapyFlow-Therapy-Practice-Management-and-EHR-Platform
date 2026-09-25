package com.smart.therapy.flow.superadmin.entity;

import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_purge_jobs", schema = "public", indexes = {
        @Index(name = "idx_platform_purge_jobs_status_scheduled", columnList = "status, scheduled_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformPurgeJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(name = "retention_days", nullable = false)
    private Integer retentionDays;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "warning_sent_at")
    private Instant warningSentAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_by_auth_id")
    private Long createdByAuthId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

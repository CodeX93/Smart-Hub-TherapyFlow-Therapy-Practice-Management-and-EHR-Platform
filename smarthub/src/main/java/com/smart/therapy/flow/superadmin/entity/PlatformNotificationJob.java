package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_notification_jobs", schema = "public", indexes = {
        @Index(name = "idx_platform_notification_jobs_status", columnList = "status"),
        @Index(name = "idx_platform_notification_jobs_scheduled_at", columnList = "scheduled_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformNotificationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_type", nullable = false, length = 20)
    private String jobType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "target_json", columnDefinition = "TEXT")
    private String targetJson;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_by_auth_id")
    private Long createdByAuthId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

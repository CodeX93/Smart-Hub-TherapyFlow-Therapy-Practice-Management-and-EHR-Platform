package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "platform_notification_read_receipts",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_platform_notification_read_receipts_auth_job", columnNames = {"auth_id", "notification_job_id"})
        },
        indexes = {
                @Index(name = "idx_platform_notification_read_receipts_auth", columnList = "auth_id"),
                @Index(name = "idx_platform_notification_read_receipts_job", columnList = "notification_job_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformNotificationReadReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_job_id", nullable = false)
    private PlatformNotificationJob notificationJob;

    @Column(name = "auth_id", nullable = false)
    private Long authId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

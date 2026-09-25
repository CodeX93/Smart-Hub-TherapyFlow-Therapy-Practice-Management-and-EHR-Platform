package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.subscription.enums.BillingExportJobStatus;
import com.smart.therapy.flow.subscription.enums.BillingExportType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "billing_export_jobs", schema = "public", indexes = {
        @Index(name = "idx_billing_export_jobs_status", columnList = "status, created_at"),
        @Index(name = "idx_billing_export_jobs_token", columnList = "download_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class BillingExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 60)
    private BillingExportType exportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private BillingExportJobStatus status = BillingExportJobStatus.QUEUED;

    @Column(name = "requested_by_auth_id")
    private Long requestedByAuthId;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "download_token", length = 120)
    private String downloadToken;

    @Column(name = "token_consumed", nullable = false)
    @Builder.Default
    private Boolean tokenConsumed = false;

    @Column(name = "token_consumed_at")
    private Instant tokenConsumedAt;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

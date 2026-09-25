package com.smart.therapy.flow.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_phi_blind_index_backfill_jobs", schema = "public", indexes = {
        @Index(name = "idx_phi_blind_backfill_status_updated", columnList = "status, updated_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_phi_blind_backfill_schema_kind",
                columnNames = {"schema_name", "job_kind"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformPhiBlindIndexBackfillJob {

    public static final String KIND_CLIENT_DIGESTS = "CLIENT_DIGESTS";
    public static final String KIND_CONTACT_DIGESTS = "CONTACT_DIGESTS";
    public static final String KIND_NAME_PREFIX_DIGESTS = "NAME_PREFIX_DIGESTS";
    public static final String KIND_NAME_TOKEN_DIGESTS = "NAME_TOKEN_DIGESTS";
    public static final String KIND_ENCRYPT_MRN = "ENCRYPT_MRN";
    public static final String KIND_REENCRYPT_NAME_V2 = "REENCRYPT_NAME_V2";
    public static final String KIND_REENCRYPT_CONTACT_V2 = "REENCRYPT_CONTACT_V2";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "job_kind", nullable = false, length = 40)
    private String jobKind;

    @Column(name = "last_processed_id", nullable = false)
    @Builder.Default
    private Long lastProcessedId = 0L;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "processed_count", nullable = false)
    @Builder.Default
    private Long processedCount = 0L;

    @Column(name = "error_count", nullable = false)
    @Builder.Default
    private Long errorCount = 0L;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "hmac_key_version", length = 64)
    private String hmacKeyVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

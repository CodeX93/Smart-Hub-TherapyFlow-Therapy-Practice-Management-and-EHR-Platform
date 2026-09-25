package com.smart.therapy.flow.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_phi_encryption_backfill_jobs", schema = "public", indexes = {
        @Index(name = "idx_phi_backfill_status_updated", columnList = "status, updated_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_phi_backfill_schema_table_column",
                columnNames = {"schema_name", "table_name", "column_name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformPhiEncryptionBackfillJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @Column(name = "table_name", nullable = false, length = 100)
    private String tableName;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "pk_column", nullable = false, length = 50)
    @Builder.Default
    private String pkColumn = "id";

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

package com.smart.therapy.flow.session.entity;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * AudioFile entity for temporary storage of audio files used in transcription.
 * Implements 30-day retention policy for quality assurance and re-processing.
 * 
 * Best Practice: Temporary storage with automatic deletion for HIPAA compliance.
 */
@Entity
@Table(name = "audio_files", indexes = {
    @Index(name = "idx_audio_file_session_note", columnList = "session_note_id"),
    @Index(name = "idx_audio_file_client", columnList = "client_id"),
    @Index(name = "idx_audio_file_expires_at", columnList = "expires_at"),
    @Index(name = "idx_audio_file_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AudioFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_note_id", nullable = false)
    private SessionNote sessionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath; // S3 key or local path

    @Column(name = "file_size", nullable = false)
    private Long fileSize; // Size in bytes

    @Column(name = "content_type", length = 100)
    private String contentType; // e.g., audio/webm, audio/mpeg

    @Column(name = "duration_seconds")
    private Integer durationSeconds; // Audio duration if available

    // Retention policy
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt; // Auto-delete after this date (30 days default)

    @Column(name = "retention_days", nullable = false)
    @Builder.Default
    private Integer retentionDays = 30; // Configurable retention period

    // Transcription metadata
    @Column(name = "transcription_status", length = 50, nullable = false)
    @Builder.Default
    private String transcriptionStatus = "PENDING"; // PENDING, COMPLETED, FAILED, RETRY

    @Column(name = "transcription_quality_score")
    private Double transcriptionQualityScore; // 0.0-1.0 confidence score

    @Column(name = "transcription_attempts", nullable = false)
    @Builder.Default
    private Integer transcriptionAttempts = 0;

    @Column(name = "last_transcription_at")
    private Instant lastTranscriptionAt;

    // Access tracking for audit
    @Column(name = "access_count", nullable = false)
    @Builder.Default
    private Integer accessCount = 0;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    @Column(name = "last_accessed_by")
    private Long lastAccessedBy; // User ID

    // Status tracking
    @Column(name = "status", length = 50, nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, DELETED

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "encryption_key_id", length = 255)
    private String encryptionKeyId; // KMS key ID for encrypted storage

    /**
     * Check if audio file has expired and should be deleted
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Check if audio can be re-processed (within retention period)
     */
    public boolean canReprocess() {
        return "ACTIVE".equals(status) && !isExpired();
    }

    /**
     * Mark as accessed for audit trail
     */
    public void recordAccess(Long userId) {
        this.accessCount++;
        this.lastAccessedAt = Instant.now();
        this.lastAccessedBy = userId;
    }

    /**
     * Mark as expired (scheduled for deletion)
     */
    public void markAsExpired() {
        this.status = "EXPIRED";
        this.deletedAt = Instant.now();
    }

    /**
     * Mark transcription as completed
     */
    public void markTranscriptionCompleted(Double qualityScore) {
        this.transcriptionStatus = "COMPLETED";
        this.transcriptionQualityScore = qualityScore;
        this.lastTranscriptionAt = Instant.now();
        this.transcriptionAttempts++;
    }

    /**
     * Mark transcription as failed
     */
    public void markTranscriptionFailed() {
        this.transcriptionStatus = "FAILED";
        this.transcriptionAttempts++;
    }
}

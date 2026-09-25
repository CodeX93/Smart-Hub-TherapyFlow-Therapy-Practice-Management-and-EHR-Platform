package com.smart.therapy.flow.session.entity;

import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.session.enums.SessionTranscriptChunkStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "session_transcript_chunks", indexes = {
        @Index(name = "idx_session_transcript_chunk_transcript", columnList = "transcript_id"),
        @Index(name = "idx_session_transcript_chunk_index", columnList = "chunk_index"),
        @Index(name = "idx_session_transcript_chunk_status", columnList = "chunk_status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_transcript_chunk_index", columnNames = {"transcript_id", "chunk_index"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SessionTranscriptChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcript_id", nullable = false)
    private SessionTranscript transcript;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_status", nullable = false, length = 20)
    private SessionTranscriptChunkStatus chunkStatus;

    @Column(name = "chunk_text", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String chunkText;

    @Column(name = "chunk_duration_seconds")
    private Double chunkDurationSeconds;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @PrePersist
    protected void onCreateChunk() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}

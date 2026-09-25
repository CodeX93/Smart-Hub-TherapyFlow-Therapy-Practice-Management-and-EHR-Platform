package com.smart.therapy.flow.session.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatus;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "session_transcripts", indexes = {
        @Index(name = "idx_session_transcript_session", columnList = "session_id"),
        @Index(name = "idx_session_transcript_upload", columnList = "upload_id"),
        @Index(name = "idx_session_transcript_client", columnList = "client_id"),
        @Index(name = "idx_session_transcript_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SessionTranscript extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "upload_id", nullable = false, length = 120, unique = true)
    private String uploadId;

    @Convert(converter = SessionTranscriptStatusConverter.class)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SessionTranscriptStatus status = SessionTranscriptStatus.RECORDING;

    @Column(name = "language", length = 20)
    private String language;

    @Column(name = "translated_to_english", nullable = false)
    @Builder.Default
    private Boolean translatedToEnglish = false;

    @Column(name = "expected_chunks")
    private Integer expectedChunks;

    @Column(name = "received_chunks", nullable = false)
    @Builder.Default
    private Integer receivedChunks = 0;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "final_transcript", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String finalTranscript;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String rawContent;

    // AI-generated speaker-diarized version of the transcript.
    // Populated on demand via POST /sessions/{id}/transcript/diarize.
    // Encrypted at rest. Null until diarization is triggered.
    @Column(name = "diarized_transcript", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String diarizedTranscript;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @OneToMany(mappedBy = "transcript", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SessionTranscriptChunk> chunks = new ArrayList<>();
}

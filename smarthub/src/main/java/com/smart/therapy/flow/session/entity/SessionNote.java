package com.smart.therapy.flow.session.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "session_notes", indexes = {
        @Index(name = "idx_session_note_session", columnList = "session_id"),
        @Index(name = "idx_session_note_client", columnList = "client_id"),
        @Index(name = "idx_session_note_therapist", columnList = "therapist_id"),
        @Index(name = "idx_session_note_finalized", columnList = "is_finalized")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SessionNote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    @Column(nullable = false)
    private Instant date;

    // Clinical content fields
    @Column(name = "session_focus", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String sessionFocus;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String symptoms;

    @Column(name = "short_term_goals", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String shortTermGoals;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String intervention;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String progress;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String remarks;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String recommendations;

    // Rating & outcome fields (with validation constraints)
    @Column(name = "client_rating", columnDefinition = "INTEGER CHECK (client_rating >= 0 AND client_rating <= 10)")
    @Min(value = 0, message = "Client rating must be at least 0")
    @Max(value = 10, message = "Client rating must be at most 10")
    private Integer clientRating; // 0-10

    @Column(name = "therapist_rating", columnDefinition = "INTEGER CHECK (therapist_rating >= 0 AND therapist_rating <= 10)")
    @Min(value = 0, message = "Therapist rating must be at least 0")
    @Max(value = 10, message = "Therapist rating must be at most 10")
    private Integer therapistRating; // 0-10

    @Column(name = "progress_toward_goals", columnDefinition = "INTEGER CHECK (progress_toward_goals >= 0 AND progress_toward_goals <= 100)")
    @Min(value = 0, message = "Progress must be at least 0%")
    @Max(value = 100, message = "Progress must be at most 100%")
    private Integer progressTowardGoals; // 0-100%

    @Column(name = "mood_before", columnDefinition = "INTEGER CHECK (mood_before >= 1 AND mood_before <= 10)")
    @Min(value = 1, message = "Mood rating must be at least 1")
    @Max(value = 10, message = "Mood rating must be at most 10")
    private Integer moodBefore; // 1-10

    @Column(name = "mood_after", columnDefinition = "INTEGER CHECK (mood_after >= 1 AND mood_after <= 10)")
    @Min(value = 1, message = "Mood rating must be at least 1")
    @Max(value = 10, message = "Mood rating must be at most 10")
    private Integer moodAfter; // 1-10

    // Risk Assessment fields (0-10 scale)
    @Column(name = "risk_suicidal_ideation", columnDefinition = "INTEGER CHECK (risk_suicidal_ideation >= 0 AND risk_suicidal_ideation <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskSuicidalIdeation; // 0-10

    @Column(name = "risk_self_harm", columnDefinition = "INTEGER CHECK (risk_self_harm >= 0 AND risk_self_harm <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskSelfHarm; // 0-10

    @Column(name = "risk_homicidal_ideation", columnDefinition = "INTEGER CHECK (risk_homicidal_ideation >= 0 AND risk_homicidal_ideation <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskHomicidalIdeation; // 0-10

    @Column(name = "risk_psychosis", columnDefinition = "INTEGER CHECK (risk_psychosis >= 0 AND risk_psychosis <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskPsychosis; // 0-10

    @Column(name = "risk_substance_use", columnDefinition = "INTEGER CHECK (risk_substance_use >= 0 AND risk_substance_use <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskSubstanceUse; // 0-10

    @Column(name = "risk_impulsivity", columnDefinition = "INTEGER CHECK (risk_impulsivity >= 0 AND risk_impulsivity <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskImpulsivity; // 0-10

    @Column(name = "risk_aggression", columnDefinition = "INTEGER CHECK (risk_aggression >= 0 AND risk_aggression <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskAggression; // 0-10

    @Column(name = "risk_trauma_symptoms", columnDefinition = "INTEGER CHECK (risk_trauma_symptoms >= 0 AND risk_trauma_symptoms <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskTraumaSymptoms; // 0-10

    @Column(name = "risk_non_adherence", columnDefinition = "INTEGER CHECK (risk_non_adherence >= 0 AND risk_non_adherence <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskNonAdherence; // 0-10

    @Column(name = "risk_support_system", columnDefinition = "INTEGER CHECK (risk_support_system >= 0 AND risk_support_system <= 10)")
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    private Integer riskSupportSystem; // 0-10

    // AI & content management
    @Column(name = "generated_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String generatedContent;

    @Column(name = "draft_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String draftContent;

    @Column(name = "final_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String finalContent;

    @Column(name = "is_draft", nullable = false)
    @Builder.Default
    private Boolean isDraft = true;

    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private Boolean isFinalized = false;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "ai_enabled", nullable = false)
    @Builder.Default
    private Boolean aiEnabled = false;

    @Column(name = "custom_ai_prompt", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String customAiPrompt;

    @Column(name = "ai_processing_status", length = 50)
    @Builder.Default
    private String aiProcessingStatus = "idle"; // idle, processing, completed, error

    // Voice transcription
    @Column(name = "voice_transcription", columnDefinition = "TEXT")
    private String voiceTranscription; // Raw transcript from audio recording (audit trail)
}

package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Request to create a session note")
public class CreateSessionNoteRequest {
    @NotNull
    @Schema(description = "ID of the session (REQUIRED)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sessionId;
    
    @NotNull
    @Schema(description = "ID of the client (REQUIRED)", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clientId;
    
    @NotNull
    @Schema(description = "ID of the therapist (REQUIRED)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long therapistId;
    
    @NotNull
    @Schema(description = "Date of the session (REQUIRED, ISO 8601 format)", example = "2025-12-25T14:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date-time")
    private Instant date;
    
    // Clinical content
    @Schema(description = "Session focus (optional)", example = "Anxiety management techniques")
    private String sessionFocus;

    @Schema(description = "Symptoms observed (optional)", example = "Mild anxiety, improved mood")
    private String symptoms;

    @Schema(description = "Short-term goals (optional)", example = "Practice breathing exercises daily")
    private String shortTermGoals;

    @Schema(description = "Interventions used (optional)", example = "CBT techniques, mindfulness")
    private String intervention;

    @Schema(description = "Progress notes (optional)", example = "Client showed improvement in managing anxiety")
    private String progress;

    @Schema(description = "Additional remarks (optional)", example = "Client engaged well in session")
    private String remarks;

    @Schema(description = "Recommendations (optional)", example = "Continue weekly sessions")
    private String recommendations;
    
    // Ratings
    @Schema(description = "Client's self-rating (optional, 1-10)", example = "7", minimum = "1", maximum = "10")
    private Integer clientRating;

    @Schema(description = "Therapist's rating (optional, 1-10)", example = "8", minimum = "1", maximum = "10")
    private Integer therapistRating;

    @Schema(description = "Progress toward goals (optional, 1-10)", example = "6", minimum = "1", maximum = "10")
    private Integer progressTowardGoals;

    @Schema(description = "Mood before session (optional, 1-10)", example = "4", minimum = "1", maximum = "10")
    private Integer moodBefore;

    @Schema(description = "Mood after session (optional, 1-10)", example = "7", minimum = "1", maximum = "10")
    private Integer moodAfter;
    
    // Risk assessments
    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk of suicidal ideation (optional, 0-10)", example = "2", minimum = "0", maximum = "10")
    private Integer riskSuicidalIdeation;

    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk of self-harm (optional, 0-10)", example = "2", minimum = "0", maximum = "10")
    private Integer riskSelfHarm;

    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk of homicidal ideation (optional, 0-10)", example = "2", minimum = "0", maximum = "10")
    private Integer riskHomicidalIdeation;

    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk of psychosis (optional, 0-10)", example = "2", minimum = "0", maximum = "10")
    private Integer riskPsychosis;

    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk of substance use (optional, 0-10)", example = "2", minimum = "0", maximum = "10")
    private Integer riskSubstanceUse;

    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk of impulsivity (optional, 0-10)", example = "2", minimum = "0", maximum = "10")
    private Integer riskImpulsivity;

    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk of aggression (optional, 0-10)", example = "2", minimum = "0", maximum = "10")
    private Integer riskAggression;

    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk of trauma symptoms (optional, 0-10)", example = "3", minimum = "0", maximum = "10")
    private Integer riskTraumaSymptoms;

    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk of non-adherence (optional, 0-10)", example = "2", minimum = "0", maximum = "10")
    private Integer riskNonAdherence;

    @Min(value = 0, message = "Risk level must be at least 0")
    @Max(value = 10, message = "Risk level must be at most 10")
    @Schema(description = "Risk related to support system (optional, 0-10)", example = "2", minimum = "0", maximum = "10")
    private Integer riskSupportSystem;

    // Content management
    @Schema(description = "AI-generated or therapist-edited final note HTML (optional)", example = "<p>Session focus...</p>")
    private String generatedContent;

    @Schema(description = "Draft note HTML (optional). Used when generatedContent is not set.", example = "<p>Draft notes...</p>")
    private String draftContent;

    @Schema(description = "Whether this is a draft (optional, default: true)", example = "true", defaultValue = "true")
    private Boolean isDraft;

    @Schema(description = "Whether this note is finalized (optional, default: false)", example = "false", defaultValue = "false")
    private Boolean isFinalized;
    
    // AI
    @Schema(description = "Whether AI assistance is enabled (optional, default: false)", example = "false", defaultValue = "false")
    private Boolean aiEnabled;

    @Schema(description = "Custom AI prompt (optional)", example = "Generate a summary focusing on progress")
    private String customAiPrompt;
}


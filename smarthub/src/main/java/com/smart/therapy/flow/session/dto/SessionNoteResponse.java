package com.smart.therapy.flow.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionNoteResponse {
    private Long id;
    private Long sessionId;
    private Long clientId;
    private Long therapistId;
    private String therapistName;
    private Instant date;
    
    // Clinical content
    private String sessionFocus;
    private String symptoms;
    private String shortTermGoals;
    private String intervention;
    private String progress;
    private String remarks;
    private String recommendations;
    
    // Ratings
    private Integer clientRating;
    private Integer therapistRating;
    private Integer progressTowardGoals;
    private Integer moodBefore;
    private Integer moodAfter;
    
    // Risk assessments
    private Integer riskSuicidalIdeation;
    private Integer riskSelfHarm;
    private Integer riskHomicidalIdeation;
    private Integer riskPsychosis;
    private Integer riskSubstanceUse;
    private Integer riskImpulsivity;
    private Integer riskAggression;
    private Integer riskTraumaSymptoms;
    private Integer riskNonAdherence;
    private Integer riskSupportSystem;
    
    // Content management
    private String generatedContent;
    private String draftContent;
    private String finalContent;
    private Boolean isDraft;
    private Boolean isFinalized;
    private Instant finalizedAt;
    
    // AI
    private Boolean aiEnabled;
    private String customAiPrompt;
    private String aiProcessingStatus;
    private String voiceTranscription;
    
    private Instant createdAt;
    private Instant updatedAt;
}


package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.ai.dto.SessionNoteTemplateRequest;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.NotificationPayloadFactory;
import com.smart.therapy.flow.session.dto.*;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionNote;
import com.smart.therapy.flow.session.entity.SessionNoteAmendment;
import com.smart.therapy.flow.session.repository.SessionNoteAmendmentRepository;
import com.smart.therapy.flow.session.repository.SessionNoteRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionNoteService {

    private static final String RESOURCE_TYPE_SESSION_NOTE = "session_note";

    private final SessionNoteRepository sessionNoteRepository;
    private final SessionNoteAmendmentRepository sessionNoteAmendmentRepository;
    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final CaseloadScopeService caseloadScopeService;
    private final SystemOptionResolverService systemOptionResolverService;
    private final TimezoneService timezoneService;
    private final com.smart.therapy.flow.system.repository.PracticeConfigurationRepository practiceConfigurationRepository;
    private final com.smart.therapy.flow.user.repository.UserProfileRepository userProfileRepository;
    private final com.smart.therapy.flow.client.util.ClientServiceEligibilityMessages clientServiceEligibilityMessages;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private com.smart.therapy.flow.ai.service.AiService aiService;

    @Autowired(required = false)
    private com.smart.therapy.flow.ai.service.OpenAiClient openAiClient;

    @Autowired(required = false)
    private AudioStorageService audioStorageService;

    @Autowired(required = false)
    private com.smart.therapy.flow.session.repository.AudioFileRepository audioFileRepository;

    @Autowired(required = false)
    private com.smart.therapy.flow.notification.service.NotificationService notificationService;

    @Transactional
    public List<SessionNoteResponse> getSessionNotesBySession(Long sessionId, AuthPrincipal requester) {
        Objects.requireNonNull(sessionId, "Session ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify session access
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Check service visibility
        if (!hasAccessToSession(session, requester)) {
            throw new ForbiddenException("Access denied to this session");
        }

        List<SessionNote> notes = sessionNoteRepository.findBySessionId(sessionId);
        User actor = currentUserService.requireCurrentUser(requester);
        recordAuditEvent(actor.getId(), requester.getLoginIdentifier(), null, session.getClient().getId(),
                "session_notes_viewed", null, "session-note-api",
                "Viewed session note list; resultCount=" + notes.size(), sessionId);
        return notes.stream()
                .map(this::toSessionNoteResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<SessionNoteResponse> getSessionNotesByClient(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Get all notes for client
        List<SessionNote> allNotes = sessionNoteRepository.findBySession_Client_Id(clientId);

        // PBAC: Filter by service visibility based on permissions
        boolean includeHiddenServices = permissionChecker.hasPermission(requester,"CLIENT_VIEW_ALL");
        if (!includeHiddenServices) {
            List<Session> visibleSessions = sessionRepository.findByClientId(clientId).stream()
                    .filter(s -> hasAccessToSession(s, requester))
                    .collect(Collectors.toList());
            List<Long> visibleSessionIds = visibleSessions.stream()
                    .map(Session::getId)
                    .collect(Collectors.toList());
            
            allNotes = allNotes.stream()
                    .filter(note -> visibleSessionIds.contains(note.getSession().getId()))
                    .collect(Collectors.toList());
        }

        User actor = currentUserService.requireCurrentUser(requester);
        recordAuditEvent(actor.getId(), requester.getLoginIdentifier(), null, clientId,
                "session_notes_viewed", null, "session-note-api",
                "Viewed client session note list; resultCount=" + allNotes.size(), null);
        return allNotes.stream()
                .map(this::toSessionNoteResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SessionNoteResponse getSessionNote(Long id, AuthPrincipal requester) {
        Objects.requireNonNull(id, "Session note ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionNote note = sessionNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        // Check service visibility
        if (!hasAccessToSession(note.getSession(), requester)) {
            throw new ResourceNotFoundException("Session note not found");
        }

        User actor = currentUserService.requireCurrentUser(requester);
        recordAuditEvent(actor.getId(), requester.getLoginIdentifier(), note.getId(), note.getClient().getId(),
                "session_note_viewed", null, "session-note-api",
                "Viewed session note", note.getSession().getId());
        return toSessionNoteResponse(note);
    }

    @Transactional
    public SessionNoteResponse createSessionNote(CreateSessionNoteRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        // Verify session exists and user has access
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (!hasAccessToSession(session, requester)) {
            throw new ForbiddenException("Cannot create notes for this session");
        }

        if (!session.getClient().canReceiveServices()) {
            throw new BadRequestException(clientServiceEligibilityMessages.notesBlocked(session.getClient()));
        }

        // Verify client matches session
        if (!session.getClient().getId().equals(request.getClientId())) {
            throw new BadRequestException("Client ID does not match session");
        }

        // Verify therapist
        User therapist = userRepository.findById(request.getTherapistId())
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found"));

        boolean isFinalized = request.getIsFinalized() != null ? request.getIsFinalized() : false;
        boolean isDraft = request.getIsDraft() != null ? request.getIsDraft() : !isFinalized;

        // Build session note
        SessionNote note = SessionNote.builder()
                .session(session)
                .client(session.getClient())
                .therapist(therapist)
                .date(request.getDate() != null ? request.getDate() : Instant.now())
                .sessionFocus(request.getSessionFocus())
                .symptoms(request.getSymptoms())
                .shortTermGoals(request.getShortTermGoals())
                .intervention(request.getIntervention())
                .progress(request.getProgress())
                .remarks(request.getRemarks())
                .recommendations(request.getRecommendations())
                .clientRating(request.getClientRating())
                .therapistRating(request.getTherapistRating())
                .progressTowardGoals(request.getProgressTowardGoals())
                .moodBefore(request.getMoodBefore())
                .moodAfter(request.getMoodAfter())
                .riskSuicidalIdeation(request.getRiskSuicidalIdeation())
                .riskSelfHarm(request.getRiskSelfHarm())
                .riskHomicidalIdeation(request.getRiskHomicidalIdeation())
                .riskPsychosis(request.getRiskPsychosis())
                .riskSubstanceUse(request.getRiskSubstanceUse())
                .riskImpulsivity(request.getRiskImpulsivity())
                .riskAggression(request.getRiskAggression())
                .riskTraumaSymptoms(request.getRiskTraumaSymptoms())
                .riskNonAdherence(request.getRiskNonAdherence())
                .riskSupportSystem(request.getRiskSupportSystem())
                .generatedContent(request.getGeneratedContent())
                .draftContent(request.getDraftContent())
                .aiEnabled(request.getAiEnabled() != null ? request.getAiEnabled() : false)
                .customAiPrompt(request.getCustomAiPrompt())
                .isDraft(isDraft)
                .isFinalized(isFinalized)
                .finalizedAt(isFinalized ? Instant.now() : null)
                .aiProcessingStatus("idle")
                .build();

        SessionNote saved = sessionNoteRepository.save(note);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), saved.getId(),
                request.getClientId(), "note_created", ipAddress, "session-note-api",
                "Session note created", request.getSessionId());

        // Process AI if enabled
        if (Boolean.TRUE.equals(request.getAiEnabled()) && aiService != null) {
            processAiGenerationAsync(saved, requester, ipAddress);
        }

        if (notificationService != null) {
            try {
                notificationService.processEvent(NotificationEventCatalog.SESSION_NOTE_CREATED,
                        buildSessionNoteEventData(saved));
            } catch (Exception e) {
                log.error("Failed to trigger session_note_created notification for note {}", saved.getId(), e);
            }
        }

        return toSessionNoteResponse(saved);
    }

    @Transactional
    public SessionNoteResponse updateSessionNote(Long id, UpdateSessionNoteRequest request, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Session note ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionNote note = sessionNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        // Check access
        if (!hasAccessToSession(note.getSession(), requester)) {
            throw new ResourceNotFoundException("Session note not found");
        }

        // ClientHub reopen: dedicated /unfinalize. Some clients still PUT isFinalized=false —
        // route that through the same reopen path (permissions + DB allow flag).
        if (Boolean.TRUE.equals(note.getIsFinalized())
                && Boolean.FALSE.equals(request.getIsFinalized())) {
            return unfinalizeSessionNote(id, requester, ipAddress);
        }

        assertNotFinalized(note, "edit");

        if (note.getSession() != null && note.getSession().getClient() != null
                && !note.getSession().getClient().canReceiveServices()) {
            throw new BadRequestException(clientServiceEligibilityMessages.notesBlocked(note.getSession().getClient()));
        }

        // Update fields
        if (request.getDate() != null) note.setDate(request.getDate());
        if (request.getSessionFocus() != null) note.setSessionFocus(request.getSessionFocus());
        if (request.getSymptoms() != null) note.setSymptoms(request.getSymptoms());
        if (request.getShortTermGoals() != null) note.setShortTermGoals(request.getShortTermGoals());
        if (request.getIntervention() != null) note.setIntervention(request.getIntervention());
        if (request.getProgress() != null) note.setProgress(request.getProgress());
        if (request.getRemarks() != null) note.setRemarks(request.getRemarks());
        if (request.getRecommendations() != null) note.setRecommendations(request.getRecommendations());
        if (request.getClientRating() != null) note.setClientRating(request.getClientRating());
        if (request.getTherapistRating() != null) note.setTherapistRating(request.getTherapistRating());
        if (request.getProgressTowardGoals() != null) note.setProgressTowardGoals(request.getProgressTowardGoals());
        if (request.getMoodBefore() != null) note.setMoodBefore(request.getMoodBefore());
        if (request.getMoodAfter() != null) note.setMoodAfter(request.getMoodAfter());
        if (request.getRiskSuicidalIdeation() != null) note.setRiskSuicidalIdeation(request.getRiskSuicidalIdeation());
        if (request.getRiskSelfHarm() != null) note.setRiskSelfHarm(request.getRiskSelfHarm());
        if (request.getRiskHomicidalIdeation() != null) note.setRiskHomicidalIdeation(request.getRiskHomicidalIdeation());
        if (request.getRiskPsychosis() != null) note.setRiskPsychosis(request.getRiskPsychosis());
        if (request.getRiskSubstanceUse() != null) note.setRiskSubstanceUse(request.getRiskSubstanceUse());
        if (request.getRiskImpulsivity() != null) note.setRiskImpulsivity(request.getRiskImpulsivity());
        if (request.getRiskAggression() != null) note.setRiskAggression(request.getRiskAggression());
        if (request.getRiskTraumaSymptoms() != null) note.setRiskTraumaSymptoms(request.getRiskTraumaSymptoms());
        if (request.getRiskNonAdherence() != null) note.setRiskNonAdherence(request.getRiskNonAdherence());
        if (request.getRiskSupportSystem() != null) note.setRiskSupportSystem(request.getRiskSupportSystem());
        if (request.getGeneratedContent() != null) note.setGeneratedContent(request.getGeneratedContent());
        if (request.getDraftContent() != null) note.setDraftContent(request.getDraftContent());
        if (request.getIsDraft() != null) note.setIsDraft(request.getIsDraft());
        if (request.getIsFinalized() != null) {
            note.setIsFinalized(request.getIsFinalized());
            if (Boolean.TRUE.equals(request.getIsFinalized())) {
                note.setIsDraft(false);
                if (note.getFinalizedAt() == null) {
                    note.setFinalizedAt(Instant.now());
                }
            } else {
                note.setFinalizedAt(null);
            }
        }
        if (request.getAiEnabled() != null) note.setAiEnabled(request.getAiEnabled());
        if (request.getCustomAiPrompt() != null) note.setCustomAiPrompt(request.getCustomAiPrompt());
        if (request.getAiProcessingStatus() != null) note.setAiProcessingStatus(request.getAiProcessingStatus());

        SessionNote saved = sessionNoteRepository.save(note);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), id,
                note.getClient().getId(), "note_updated", ipAddress, "session-note-api",
                "Session note updated", note.getSession().getId());

        if (notificationService != null) {
            try {
                notificationService.processEvent(NotificationEventCatalog.SESSION_NOTE_UPDATED,
                        buildSessionNoteEventData(saved));
            } catch (Exception e) {
                log.error("Failed to trigger session_note_updated notification for note {}", saved.getId(), e);
            }
        }

        return toSessionNoteResponse(saved);
    }

    @Transactional
    public void deleteSessionNote(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Session note ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionNote note = sessionNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        // Check access
        if (!hasAccessToSession(note.getSession(), requester)) {
            throw new ResourceNotFoundException("Session note not found");
        }
        assertNotFinalized(note, "delete");

        Long clientId = note.getClient().getId();
        Long sessionId = note.getSession().getId();

        sessionNoteRepository.delete(note);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), id,
                clientId, "note_deleted", ipAddress, "session-note-api",
                "Session note deleted", sessionId);
    }

    @Transactional
    public SessionNoteResponse finalizeSessionNote(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Session note ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionNote note = sessionNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        // Check if already finalized
        if (Boolean.TRUE.equals(note.getIsFinalized())) {
            throw new BadRequestException("Session note is already finalized");
        }

        // PBAC: Permission + Data Scope check
        if (!hasAccessToTherapist(note.getTherapist().getId(), requester)) {
            throw new ForbiddenException("You do not have permission to finalize this session note");
        }

        // Finalize
        note.setIsFinalized(true);
        note.setIsDraft(false);
        note.setFinalizedAt(Instant.now());
        String contentToFinalize = note.getGeneratedContent() != null 
                ? note.getGeneratedContent() 
                : (note.getDraftContent() != null ? note.getDraftContent() : "");
        note.setFinalContent(contentToFinalize);

        SessionNote saved = sessionNoteRepository.save(note);

        // Audit log
        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), id,
                note.getClient().getId(), "finalize_session_note", ipAddress, "session-note-api",
                "Session note finalized", note.getSession().getId());

        return toSessionNoteResponse(saved);
    }

    /**
     * Reopen (unfinalize) a session note — ClientHub parity.
     * Same permission model as finalize: assigned therapist, supervisor of that therapist, or admin
     * (via caseload scope). Requires {@code app.allow_clinical_unfinalize} for the DB immutability trigger.
     */
    @Transactional
    public SessionNoteResponse unfinalizeSessionNote(Long id, AuthPrincipal requester, String ipAddress) {
        Objects.requireNonNull(id, "Session note ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionNote note = sessionNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        if (!Boolean.TRUE.equals(note.getIsFinalized())) {
            throw new BadRequestException("Session note is not finalized");
        }

        // PBAC: same access as finalize — assigned therapist / supervisor caseload / admin
        if (!hasAccessToTherapist(note.getTherapist().getId(), requester)) {
            throw new ForbiddenException("You do not have permission to reopen this session note");
        }

        Instant previouslyFinalizedAt = note.getFinalizedAt();
        String draftContent = firstNonBlank(
                note.getFinalContent(), note.getGeneratedContent(), note.getDraftContent());

        // Permit finalized -> draft for this transaction only (V71 trigger).
        entityManager.createNativeQuery(
                        "SELECT set_config('app.allow_clinical_unfinalize', 'true', true)")
                .getSingleResult();

        note.setIsFinalized(false);
        note.setIsDraft(true);
        note.setFinalContent(null);
        note.setFinalizedAt(null);
        note.setDraftContent(draftContent);
        if (!StringUtils.hasText(note.getGeneratedContent())) {
            note.setGeneratedContent(draftContent);
        }

        SessionNote saved = sessionNoteRepository.save(note);
        entityManager.flush();

        recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), id,
                note.getClient().getId(), "session_note_reopened", ipAddress, "session-note-api",
                "Session note reopened; previouslyFinalizedAt=" + previouslyFinalizedAt,
                note.getSession().getId());

        return toSessionNoteResponse(saved);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    @Transactional
    public SessionNoteAmendmentResponse createAmendment(
            Long sessionNoteId,
            CreateSessionNoteAmendmentRequest request,
            AuthPrincipal requester,
            String ipAddress) {
        Objects.requireNonNull(sessionNoteId, "Session note ID is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionNote note = sessionNoteRepository.findById(sessionNoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        if (!Boolean.TRUE.equals(note.getIsFinalized())) {
            throw new BadRequestException("Amendments are only allowed for finalized session notes");
        }

        validateAmendmentAccess(note, requester);

        User actor = currentUserService.requireCurrentUser(requester);
        Instant signedAt = Instant.now();
        SessionNoteAmendment amendment = SessionNoteAmendment.builder()
                .sessionNote(note)
                .amendmentText(request.getAmendmentText().trim())
                .reason(request.getReason().trim())
                .createdByUser(actor)
                .signedAt(signedAt)
                .build();

        SessionNoteAmendment saved = sessionNoteAmendmentRepository.save(amendment);

        recordAuditEvent(actor.getId(), requester.getLoginIdentifier(), sessionNoteId,
                note.getClient().getId(), "session_note_amendment_created", ipAddress, "session-note-api",
                "Signed session note amendment created; amendmentId=" + saved.getId(),
                note.getSession().getId());

        return toAmendmentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SessionNoteAmendmentResponse> getAmendments(Long sessionNoteId, AuthPrincipal requester) {
        Objects.requireNonNull(sessionNoteId, "Session note ID is required");
        Objects.requireNonNull(requester, "Requester is required");

        SessionNote note = sessionNoteRepository.findById(sessionNoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        if (!hasAccessToSession(note.getSession(), requester)) {
            throw new ForbiddenException("Access denied to this session note");
        }

        User actor = currentUserService.requireCurrentUser(requester);
        recordAuditEvent(actor.getId(), requester.getLoginIdentifier(), sessionNoteId,
                note.getClient().getId(), "session_note_amendments_viewed", null, "session-note-api",
                "Viewed session note amendment list", note.getSession().getId());

        return sessionNoteAmendmentRepository.findBySessionNoteId(sessionNoteId).stream()
                .map(this::toAmendmentResponse)
                .collect(Collectors.toList());
    }

    private void validateAmendmentAccess(SessionNote note, AuthPrincipal requester) {
        if (!hasAccessToTherapist(note.getTherapist().getId(), requester)) {
            throw new ForbiddenException("You do not have permission to amend this session note");
        }
    }

    private SessionNoteAmendmentResponse toAmendmentResponse(SessionNoteAmendment amendment) {
        return SessionNoteAmendmentResponse.builder()
                .id(amendment.getId())
                .sessionNoteId(amendment.getSessionNote().getId())
                .amendmentText(amendment.getAmendmentText())
                .reason(amendment.getReason())
                .createdByUserId(amendment.getCreatedByUser().getId())
                .createdByUserName(amendment.getCreatedByUser().getFullName())
                .signedAt(amendment.getSignedAt())
                .createdAt(amendment.getCreatedAt())
                .build();
    }

    @Transactional
    public TranscribeAudioResponse transcribeAudio(AuthPrincipal requester, Long sessionNoteId, byte[] audioData, String fileName, String ipAddress) {
        SessionNote note = null;
        if (sessionNoteId != null) {
            note = sessionNoteRepository.findById(sessionNoteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));
            boolean isAssignedTherapist = note.getTherapist().getId()
                    .equals(currentUserService.requireCurrentUser(requester).getId());
            boolean isAdmin = permissionChecker.hasRole(requester, "ADMIN")
                    || permissionChecker.hasRole(requester, "SUPER_ADMIN");
            if (!isAssignedTherapist && !isAdmin) {
                throw new ForbiddenException("You do not have permission to transcribe audio for this session note");
            }
            assertNotFinalized(note, "edit");
        }

        try {
            // Step 0: Store audio file temporarily (30-day retention)
            com.smart.therapy.flow.session.entity.AudioFile audioFileEntity = null;
            if (note != null && audioStorageService != null) {
                try {
                    String storagePath = audioStorageService.storeAudioFile(audioData, fileName, 
                            note.getClient().getId(), note.getId());
                    
                    Instant expiresAt = audioStorageService.calculateExpirationDate(30);
                    
                    audioFileEntity = com.smart.therapy.flow.session.entity.AudioFile.builder()
                            .sessionNote(note)
                            .client(note.getClient())
                            .originalFilename(fileName)
                            .storagePath(storagePath)
                            .fileSize((long) audioData.length)
                            .contentType(getContentType(fileName))
                            .expiresAt(expiresAt)
                            .retentionDays(30)
                            .transcriptionStatus("PENDING")
                            .status("ACTIVE")
                            .build();
                    
                    audioFileEntity = audioFileRepository.save(audioFileEntity);
                    log.info("Audio file stored temporarily: id={}, path={}, expiresAt={}", 
                            audioFileEntity.getId(), storagePath, expiresAt);
                } catch (Exception e) {
                    log.warn("Failed to store audio file (continuing with transcription): {}", e.getMessage());
                    // Continue without storing - transcription is more critical
                }
            }

            // Step 1: Transcribe audio using OpenAI Whisper
            String rawTranscription;
            Double transcriptionQualityScore = null;
            if (openAiClient != null) {
                log.info("Transcribing audio file: fileName={}, size={} bytes", fileName, audioData.length);
                rawTranscription = openAiClient.transcribeAudio(audioData, fileName);
                log.info("Transcription completed: length={} characters", rawTranscription.length());
                
                // Calculate quality score (simple heuristic: length vs expected)
                // In production, use OpenAI's confidence scores if available
                transcriptionQualityScore = calculateTranscriptionQuality(rawTranscription, audioData.length);
            } else {
                log.warn("OpenAI client not available, skipping transcription");
                throw new IllegalStateException("Audio transcription service is not configured");
            }

            // Step 2: Organize transcription into session note fields using AI
            Map<String, String> mappedFields = new java.util.HashMap<>();
            if (aiService != null && StringUtils.hasText(rawTranscription)) {
                String clientName = note != null && note.getClient() != null 
                        ? note.getClient().getFullName() 
                        : null;
                
                log.info("Organizing transcription into session note fields");
                mappedFields = aiService.organizeTranscriptionIntoFields(rawTranscription, clientName);
                log.info("Field organization completed: {} fields extracted", mappedFields.size());
            }

            // Step 3: If session note exists, update it with transcription and mapped fields
            if (note != null) {
                note.setVoiceTranscription(rawTranscription);
                
                // Auto-populate fields from mapped data (only if field is currently empty)
                if (mappedFields.containsKey("sessionFocus") && !StringUtils.hasText(note.getSessionFocus())) {
                    note.setSessionFocus(mappedFields.get("sessionFocus"));
                }
                if (mappedFields.containsKey("symptoms") && !StringUtils.hasText(note.getSymptoms())) {
                    note.setSymptoms(mappedFields.get("symptoms"));
                }
                if (mappedFields.containsKey("shortTermGoals") && !StringUtils.hasText(note.getShortTermGoals())) {
                    note.setShortTermGoals(mappedFields.get("shortTermGoals"));
                }
                if (mappedFields.containsKey("intervention") && !StringUtils.hasText(note.getIntervention())) {
                    note.setIntervention(mappedFields.get("intervention"));
                }
                if (mappedFields.containsKey("progress") && !StringUtils.hasText(note.getProgress())) {
                    note.setProgress(mappedFields.get("progress"));
                }
                if (mappedFields.containsKey("remarks") && !StringUtils.hasText(note.getRemarks())) {
                    note.setRemarks(mappedFields.get("remarks"));
                }
                if (mappedFields.containsKey("recommendations") && !StringUtils.hasText(note.getRecommendations())) {
                    note.setRecommendations(mappedFields.get("recommendations"));
                }
                
                // Handle numeric fields
                try {
                    if (mappedFields.containsKey("clientRating") && note.getClientRating() == null) {
                        note.setClientRating(Integer.parseInt(mappedFields.get("clientRating")));
                    }
                    if (mappedFields.containsKey("therapistRating") && note.getTherapistRating() == null) {
                        note.setTherapistRating(Integer.parseInt(mappedFields.get("therapistRating")));
                    }
                    if (mappedFields.containsKey("moodBefore") && note.getMoodBefore() == null) {
                        note.setMoodBefore(Integer.parseInt(mappedFields.get("moodBefore")));
                    }
                    if (mappedFields.containsKey("moodAfter") && note.getMoodAfter() == null) {
                        note.setMoodAfter(Integer.parseInt(mappedFields.get("moodAfter")));
                    }
                    
                    // Risk assessments
                    if (mappedFields.containsKey("riskSuicidalIdeation") && note.getRiskSuicidalIdeation() == null) {
                        note.setRiskSuicidalIdeation(Integer.parseInt(mappedFields.get("riskSuicidalIdeation")));
                    }
                    if (mappedFields.containsKey("riskSelfHarm") && note.getRiskSelfHarm() == null) {
                        note.setRiskSelfHarm(Integer.parseInt(mappedFields.get("riskSelfHarm")));
                    }
                    if (mappedFields.containsKey("riskHomicidalIdeation") && note.getRiskHomicidalIdeation() == null) {
                        note.setRiskHomicidalIdeation(Integer.parseInt(mappedFields.get("riskHomicidalIdeation")));
                    }
                    if (mappedFields.containsKey("riskPsychosis") && note.getRiskPsychosis() == null) {
                        note.setRiskPsychosis(Integer.parseInt(mappedFields.get("riskPsychosis")));
                    }
                    if (mappedFields.containsKey("riskSubstanceUse") && note.getRiskSubstanceUse() == null) {
                        note.setRiskSubstanceUse(Integer.parseInt(mappedFields.get("riskSubstanceUse")));
                    }
                    if (mappedFields.containsKey("riskImpulsivity") && note.getRiskImpulsivity() == null) {
                        note.setRiskImpulsivity(Integer.parseInt(mappedFields.get("riskImpulsivity")));
                    }
                    if (mappedFields.containsKey("riskAggression") && note.getRiskAggression() == null) {
                        note.setRiskAggression(Integer.parseInt(mappedFields.get("riskAggression")));
                    }
                    if (mappedFields.containsKey("riskTraumaSymptoms") && note.getRiskTraumaSymptoms() == null) {
                        note.setRiskTraumaSymptoms(Integer.parseInt(mappedFields.get("riskTraumaSymptoms")));
                    }
                    if (mappedFields.containsKey("riskNonAdherence") && note.getRiskNonAdherence() == null) {
                        note.setRiskNonAdherence(Integer.parseInt(mappedFields.get("riskNonAdherence")));
                    }
                    if (mappedFields.containsKey("riskSupportSystem") && note.getRiskSupportSystem() == null) {
                        note.setRiskSupportSystem(Integer.parseInt(mappedFields.get("riskSupportSystem")));
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse numeric field from transcription", e);
                }
                
                sessionNoteRepository.save(note);
                
                // Update audio file entity with transcription results
                if (audioFileEntity != null) {
                    audioFileEntity.markTranscriptionCompleted(transcriptionQualityScore);
                    audioFileEntity.recordAccess(currentUserService.requireCurrentUser(requester).getId());
                    audioFileRepository.save(audioFileEntity);
                }
                
                // Audit log
                recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), note.getId(),
                        note.getClient().getId(), "audio_transcribed", ipAddress, "session-note-api",
                        "Audio transcribed and organized into session note fields", note.getSession().getId());
            } else if (audioFileEntity != null) {
                // Update audio file even if note doesn't exist yet
                audioFileEntity.markTranscriptionCompleted(transcriptionQualityScore);
                audioFileEntity.recordAccess(currentUserService.requireCurrentUser(requester).getId());
                audioFileRepository.save(audioFileEntity);
            }
        
        return TranscribeAudioResponse.builder()
                .success(true)
                    .rawTranscription(rawTranscription)
                    .mappedFields(mappedFields)
                .build();

        } catch (Exception e) {
            log.error("Failed to transcribe audio or organize fields", e);
            throw new com.smart.therapy.flow.common.exception.BadRequestException(
                    "Failed to transcribe audio: " + e.getMessage());
        }
    }

    @Transactional
    public String generatePdfHtml(
            Long id,
            AuthPrincipal requesterParam
    ) {
        Objects.requireNonNull(id, "Session note ID is required");
        Objects.requireNonNull(requesterParam, "Requester is required");

        SessionNote note = sessionNoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        // Check access
        if (!hasAccessToSession(note.getSession(), requesterParam)) {
            throw new ResourceNotFoundException("Session note not found");
        }

        User actor = currentUserService.requireCurrentUser(requesterParam);
        recordAuditEvent(actor.getId(), requesterParam.getLoginIdentifier(), note.getId(), note.getClient().getId(),
                "session_note_exported", null, "session-note-api",
                "Exported session note; format=printable_html", note.getSession().getId());
        // Return printable HTML and let the browser handle PDF conversion.
        return generateBasicPdfHtml(note);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * PBAC: Check if user has access to session based on permissions + data scope.
     */
    private boolean hasAccessToSession(
            Session session,
            AuthPrincipal requester
    ) {
        return hasAccessToTherapist(session.getTherapist().getId(), requester);
    }

    private boolean hasAccessToTherapist(Long therapistId, AuthPrincipal requester) {
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        if (resolved.scope() == CaseloadScope.NONE) {
            return false;
        }
        if (resolved.scope() == CaseloadScope.ALL) {
            return true;
        }
        return resolved.includesTherapist(therapistId);
    }

    private SessionNoteResponse toSessionNoteResponse(SessionNote note) {
        return SessionNoteResponse.builder()
                .id(note.getId())
                .sessionId(note.getSession().getId())
                .clientId(note.getClient().getId())
                .therapistId(note.getTherapist().getId())
                .therapistName(note.getTherapist().getFullName())
                .date(note.getDate())
                .sessionFocus(note.getSessionFocus())
                .symptoms(note.getSymptoms())
                .shortTermGoals(note.getShortTermGoals())
                .intervention(note.getIntervention())
                .progress(note.getProgress())
                .remarks(note.getRemarks())
                .recommendations(note.getRecommendations())
                .clientRating(note.getClientRating())
                .therapistRating(note.getTherapistRating())
                .progressTowardGoals(note.getProgressTowardGoals())
                .moodBefore(note.getMoodBefore())
                .moodAfter(note.getMoodAfter())
                .riskSuicidalIdeation(note.getRiskSuicidalIdeation())
                .riskSelfHarm(note.getRiskSelfHarm())
                .riskHomicidalIdeation(note.getRiskHomicidalIdeation())
                .riskPsychosis(note.getRiskPsychosis())
                .riskSubstanceUse(note.getRiskSubstanceUse())
                .riskImpulsivity(note.getRiskImpulsivity())
                .riskAggression(note.getRiskAggression())
                .riskTraumaSymptoms(note.getRiskTraumaSymptoms())
                .riskNonAdherence(note.getRiskNonAdherence())
                .riskSupportSystem(note.getRiskSupportSystem())
                .generatedContent(note.getGeneratedContent())
                .draftContent(note.getDraftContent())
                .finalContent(note.getFinalContent())
                .isDraft(note.getIsDraft())
                .isFinalized(note.getIsFinalized())
                .finalizedAt(note.getFinalizedAt())
                .aiEnabled(note.getAiEnabled())
                .customAiPrompt(note.getCustomAiPrompt())
                .aiProcessingStatus(note.getAiProcessingStatus())
                .voiceTranscription(note.getVoiceTranscription())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }

    @Async
    private void processAiGenerationAsync(SessionNote note, AuthPrincipal requester, String ipAddress) {
        Long noteId = note.getId();
        try {
            SessionNote noteToProcess = sessionNoteRepository.findById(noteId).orElse(note);
            noteToProcess.setAiProcessingStatus("processing");
            sessionNoteRepository.save(noteToProcess);

            if (aiService == null) {
                throw new IllegalStateException("AI service is not configured");
            }

            String generatedContent = aiService.generateSessionNoteTemplate(buildAiTemplateRequest(noteToProcess));
            if (!StringUtils.hasText(generatedContent)) {
                throw new BadRequestException("AI returned empty generated content");
            }

            noteToProcess.setGeneratedContent(generatedContent);
            if (!StringUtils.hasText(noteToProcess.getDraftContent())) {
                noteToProcess.setDraftContent(generatedContent);
            }
            noteToProcess.setAiProcessingStatus("completed");
            sessionNoteRepository.save(noteToProcess);

            recordAuditEvent(
                    currentUserService.requireCurrentUser(requester).getId(),
                    requester.getLoginIdentifier(),
                    noteToProcess.getId(),
                    noteToProcess.getClient().getId(),
                    "session_note_ai_generated",
                    ipAddress,
                    "session-note-api",
                    "Session note AI content generated",
                    noteToProcess.getSession().getId()
            );

        } catch (Exception e) {
            log.error("AI processing failed for session note {}", noteId, e);
            SessionNote failedNote = sessionNoteRepository.findById(noteId).orElse(note);
            failedNote.setAiProcessingStatus("error");
            sessionNoteRepository.save(failedNote);
        }
    }

    private SessionNoteTemplateRequest buildAiTemplateRequest(SessionNote note) {
        SessionNoteTemplateRequest request = new SessionNoteTemplateRequest();

        SessionNoteTemplateRequest.ClientInfo clientInfo = new SessionNoteTemplateRequest.ClientInfo();
        clientInfo.setFullName(note.getClient().getFullName());
        if (note.getClient().getDateOfBirth() != null) {
            clientInfo.setDateOfBirth(note.getClient().getDateOfBirth()
                    .atStartOfDay(resolvePracticeZoneId())
                    .toInstant());
        }
        clientInfo.setGender(note.getClient().getGender());
        clientInfo.setStage(note.getClient().getStage());
        request.setClient(clientInfo);

        SessionNoteTemplateRequest.SessionInfo sessionInfo = new SessionNoteTemplateRequest.SessionInfo();
        String sessionType = StringUtils.hasText(note.getSession().getClinicalSessionType())
                ? note.getSession().getClinicalSessionType()
                : (note.getSession().getSessionType() != null ? systemOptionResolverService.resolveOptionLabel(SystemOptionCategories.SESSION_MODE, note.getSession().getSessionType()) : null);
        sessionInfo.setSessionType(sessionType);
        sessionInfo.setSessionDate(note.getSession().getSessionDate());
        sessionInfo.setDuration(note.getSession().getDuration());
        request.setSession(sessionInfo);

        Map<String, String> formData = new java.util.LinkedHashMap<>();
        putIfHasText(formData, "clientId", note.getClient().getClientId());
        putIfHasText(formData, "sessionFocus", note.getSessionFocus());
        putIfHasText(formData, "symptoms", note.getSymptoms());
        putIfHasText(formData, "shortTermGoals", note.getShortTermGoals());
        putIfHasText(formData, "intervention", note.getIntervention());
        putIfHasText(formData, "progress", note.getProgress());
        putIfHasText(formData, "remarks", note.getRemarks());
        putIfHasText(formData, "recommendations", note.getRecommendations());
        request.setFormData(formData);
        request.setCustomInstructions(note.getCustomAiPrompt());

        return request;
    }

    private void putIfHasText(Map<String, String> data, String key, String value) {
        if (StringUtils.hasText(value)) {
            data.put(key, value);
        }
    }

    /**
     * Calculate transcription quality score (simple heuristic)
     * In production, use OpenAI's confidence scores if available
     */
    private Double calculateTranscriptionQuality(String transcription, int audioSizeBytes) {
        if (transcription == null || transcription.trim().isEmpty()) {
            return 0.0;
        }
        
        // Simple heuristic: longer transcriptions for larger files = better quality
        // Expected: ~100 characters per 10KB of audio (rough estimate)
        int expectedLength = audioSizeBytes / 100;
        int actualLength = transcription.length();
        
        double ratio = Math.min(1.0, (double) actualLength / expectedLength);
        // Normalize to 0.5-1.0 range (assuming minimum quality)
        return 0.5 + (ratio * 0.5);
    }

    /**
     * Get content type from filename
     */
    private String getContentType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "audio/webm";
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "webm" -> "audio/webm";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";
            case "ogg" -> "audio/ogg";
            default -> "audio/webm";
        };
    }

    /**
     * Re-process audio transcription from stored audio file
     * Only available within retention period (30 days)
     */
    @Transactional
    public TranscribeAudioResponse reprocessAudioTranscription(Long sessionNoteId, AuthPrincipal requester, String ipAddress) {
        SessionNote note = sessionNoteRepository.findById(sessionNoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        // Verify permissions
        if (!hasAccessToTherapist(note.getTherapist().getId(), requester)) {
            throw new ForbiddenException("You do not have permission to re-process audio for this session note");
        }
        assertNotFinalized(note, "edit");

        // Find stored audio file
        com.smart.therapy.flow.session.entity.AudioFile audioFile = audioFileRepository
                .findBySessionNoteId(sessionNoteId)
                .orElseThrow(() -> new ResourceNotFoundException("No stored audio file found for this session note"));

        // Check if audio can be re-processed
        if (!audioFile.canReprocess()) {
            throw new BadRequestException("Audio file has expired and cannot be re-processed. Retention period: 30 days");
        }

        // Check if audio file exists in storage
        if (!audioStorageService.audioFileExists(audioFile.getStoragePath())) {
            throw new ResourceNotFoundException("Audio file not found in storage");
        }

        try {
            // Retrieve audio file
            java.io.InputStream audioStream = audioStorageService.retrieveAudioFile(audioFile.getStoragePath());
            byte[] audioData = audioStream.readAllBytes();
            audioStream.close();

            // Re-transcribe
            String rawTranscription;
            Double transcriptionQualityScore = null;
            if (openAiClient != null) {
                log.info("Re-transcribing audio file: id={}, path={}", audioFile.getId(), audioFile.getStoragePath());
                rawTranscription = openAiClient.transcribeAudio(audioData, audioFile.getOriginalFilename());
                transcriptionQualityScore = calculateTranscriptionQuality(rawTranscription, audioData.length);
                log.info("Re-transcription completed: length={} characters, quality={}", 
                        rawTranscription.length(), transcriptionQualityScore);
            } else {
                throw new IllegalStateException("Audio transcription service is not configured");
            }

            // Organize transcription into fields
            Map<String, String> mappedFields = new java.util.HashMap<>();
            if (aiService != null && StringUtils.hasText(rawTranscription)) {
                String clientName = note.getClient() != null ? note.getClient().getFullName() : null;
                mappedFields = aiService.organizeTranscriptionIntoFields(rawTranscription, clientName);
            }

            // Update session note
            note.setVoiceTranscription(rawTranscription);
            // Auto-populate fields (same logic as initial transcription)
            populateFieldsFromMappedData(note, mappedFields);
            sessionNoteRepository.save(note);

            // Update audio file metadata
            audioFile.markTranscriptionCompleted(transcriptionQualityScore);
            audioFile.recordAccess(currentUserService.requireCurrentUser(requester).getId());
            audioFileRepository.save(audioFile);

            // Audit log
            recordAuditEvent(currentUserService.requireCurrentUser(requester).getId(), requester.getLoginIdentifier(), note.getId(),
                    note.getClient().getId(), "audio_reprocessed", ipAddress, "session-note-api",
                    "Audio re-transcribed from stored file", note.getSession().getId());

            return TranscribeAudioResponse.builder()
                    .success(true)
                    .rawTranscription(rawTranscription)
                    .mappedFields(mappedFields)
                    .qualityScore(transcriptionQualityScore)
                    .message("Audio re-transcribed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to re-process audio: sessionNoteId={}", sessionNoteId, e);
            audioFile.markTranscriptionFailed();
            audioFileRepository.save(audioFile);
            throw new RuntimeException("Failed to re-process audio: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to populate session note fields from mapped data
     */
    private void populateFieldsFromMappedData(SessionNote note, Map<String, String> mappedFields) {
        if (mappedFields.containsKey("sessionFocus") && !StringUtils.hasText(note.getSessionFocus())) {
            note.setSessionFocus(mappedFields.get("sessionFocus"));
        }
        if (mappedFields.containsKey("symptoms") && !StringUtils.hasText(note.getSymptoms())) {
            note.setSymptoms(mappedFields.get("symptoms"));
        }
        if (mappedFields.containsKey("shortTermGoals") && !StringUtils.hasText(note.getShortTermGoals())) {
            note.setShortTermGoals(mappedFields.get("shortTermGoals"));
        }
        if (mappedFields.containsKey("intervention") && !StringUtils.hasText(note.getIntervention())) {
            note.setIntervention(mappedFields.get("intervention"));
        }
        if (mappedFields.containsKey("progress") && !StringUtils.hasText(note.getProgress())) {
            note.setProgress(mappedFields.get("progress"));
        }
        if (mappedFields.containsKey("remarks") && !StringUtils.hasText(note.getRemarks())) {
            note.setRemarks(mappedFields.get("remarks"));
        }
        if (mappedFields.containsKey("recommendations") && !StringUtils.hasText(note.getRecommendations())) {
            note.setRecommendations(mappedFields.get("recommendations"));
        }
    }

private String generateBasicPdfHtml(SessionNote note) {
        var client = note.getClient();
        var session = note.getSession();
        var therapist = note.getTherapist();
        ZoneId practiceZone = resolvePracticeZoneId();

        String clientName = client.getFullName();

        DateTimeFormatter sessionDateFmt = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.US);
        DateTimeFormatter dateOnlyFmt = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);
        String sessionDate = note.getDate() != null
                ? sessionDateFmt.format(note.getDate().atZone(practiceZone))
                : "N/A";
        boolean finalized = Boolean.TRUE.equals(note.getIsFinalized());
        String finalizedDate = note.getFinalizedAt() != null
                ? dateOnlyFmt.format(note.getFinalizedAt().atZone(practiceZone))
                : "";
        String todayDate = LocalDate.now(practiceZone).format(dateOnlyFmt);

        String serviceType = session.getClinicalSessionType() != null ? session.getClinicalSessionType() : "Psychotherapy";
        String roomName = session.getRoom() != null && StringUtils.hasText(session.getRoom().getRoomName())
                ? session.getRoom().getRoomName()
                : "N/A";
        String therapistName = therapist.getFullName();
        String therapistLicense = userProfileRepository.findByUserId(therapist.getId())
                .map(profile -> {
                    String type = StringUtils.hasText(profile.getLicenseType()) ? profile.getLicenseType().trim() : "";
                    String number = StringUtils.hasText(profile.getLicenseNumber()) ? "#" + profile.getLicenseNumber().trim() : "";
                    return (type + " " + number).trim();
                })
                .orElse("");

        // Letterhead comes from the practice's own configuration, matching the
        // ClientHub report; the old builder hardcoded "Therapy Practice".
        var practiceConfig = practiceConfigurationRepository.findFirstByOrderByIdAsc().orElse(null);
        String orgName = practiceConfig != null && StringUtils.hasText(practiceConfig.getPracticeName())
                ? practiceConfig.getPracticeName() : "Therapy Practice";
        String orgAddress = practiceConfig != null && StringUtils.hasText(practiceConfig.getPracticeAddress())
                ? practiceConfig.getPracticeAddress() : "";
        String orgPhone = practiceConfig != null && StringUtils.hasText(practiceConfig.getPracticePhone())
                ? practiceConfig.getPracticePhone() : "";
        String orgEmail = practiceConfig != null && StringUtils.hasText(practiceConfig.getPracticeEmail())
                ? practiceConfig.getPracticeEmail() : "";
        String orgWebsite = practiceConfig != null && StringUtils.hasText(practiceConfig.getPracticeWebsite())
                ? practiceConfig.getPracticeWebsite() : "";

        StringBuilder practiceLines = new StringBuilder();
        if (!orgAddress.isEmpty()) practiceLines.append("<p>").append(escapeHtml(orgAddress)).append("</p>");
        if (!orgPhone.isEmpty()) practiceLines.append("<p>Phone: ").append(escapeHtml(orgPhone)).append("</p>");
        if (!orgEmail.isEmpty()) practiceLines.append("<p>Email: ").append(escapeHtml(orgEmail)).append("</p>");
        if (!orgWebsite.isEmpty()) practiceLines.append("<p>Website: ").append(escapeHtml(orgWebsite)).append("</p>");

        StringBuilder riskTable = new StringBuilder();
        String[] riskFactors = {"Suicidal Ideation", "Self-Harm", "Homicidal Ideation", "Psychosis", "Substance Use", "Impulsivity", "Aggression/Violence", "Trauma Symptoms", "Non-Adherence", "Support System"};
        int[] riskValues = {
            note.getRiskSuicidalIdeation() != null ? note.getRiskSuicidalIdeation() : 0,
            note.getRiskSelfHarm() != null ? note.getRiskSelfHarm() : 0,
            note.getRiskHomicidalIdeation() != null ? note.getRiskHomicidalIdeation() : 0,
            note.getRiskPsychosis() != null ? note.getRiskPsychosis() : 0,
            note.getRiskSubstanceUse() != null ? note.getRiskSubstanceUse() : 0,
            note.getRiskImpulsivity() != null ? note.getRiskImpulsivity() : 0,
            note.getRiskAggression() != null ? note.getRiskAggression() : 0,
            note.getRiskTraumaSymptoms() != null ? note.getRiskTraumaSymptoms() : 0,
            note.getRiskNonAdherence() != null ? note.getRiskNonAdherence() : 0,
            note.getRiskSupportSystem() != null ? note.getRiskSupportSystem() : 0,
        };
        int totalRisk = 0;
        for (int i = 0; i < riskValues.length; i++) {
            totalRisk += riskValues[i];
            String level = switch (riskValues[i]) {
                case 1 -> "Minimal";
                case 2 -> "Moderate";
                case 3 -> "High";
                case 4 -> "Severe";
                default -> "None";
            };
            riskTable.append("<tr><td>").append(i + 1).append(". ").append(riskFactors[i]).append("</td>");
            riskTable.append("<td><strong>").append(riskValues[i]).append("/4</strong></td>");
            riskTable.append("<td>").append(level).append("</td></tr>");
        }
        double riskPercent = totalRisk * 100.0 / 40;
        String overallRisk;
        String overallRiskColor;
        if (riskPercent > 75) { overallRisk = "Critical"; overallRiskColor = "#dc2626"; }
        else if (riskPercent > 50) { overallRisk = "High"; overallRiskColor = "#ea580c"; }
        else if (riskPercent > 25) { overallRisk = "Moderate"; overallRiskColor = "#ca8a04"; }
        else { overallRisk = "Low"; overallRiskColor = "#059669"; }

        // The signed note is the document. Structured fields print only when a
        // note predates final-note generation and has nothing else to show.
        String rawNoteContent = resolveFinalNoteContent(note);
        String noteBody = buildFinalNoteBodyHtml(rawNoteContent);

        // AI-generated notes already open with their own CLIENT INFORMATION
        // header; anything else gets the same identity block from the record,
        // so the reader never loses the client fields (ClientHub parity).
        String age = client.getDateOfBirth() != null
                ? String.valueOf(java.time.Period.between(client.getDateOfBirth(), LocalDate.now(practiceZone)).getYears())
                : "Not specified";
        String gender = StringUtils.hasText(client.getGender()) ? client.getGender() : "Not specified";
        String stage = StringUtils.hasText(client.getStage()) ? client.getStage() : "Not specified";
        String duration = session.getDuration() != null ? session.getDuration() + " minutes" : "Not specified";
        String clientInfoBlock = noteCarriesIdentityHeader(rawNoteContent) ? "" :
                "<div class=\"content client-info\">"
                        + "<p><strong>Name:</strong> " + escapeHtml(clientName) + "</p>"
                        + "<p><strong>Client ID:</strong> " + escapeHtml(client.getClientId() != null ? client.getClientId() : "N/A") + "</p>"
                        + "<p><strong>Age:</strong> " + escapeHtml(age) + "</p>"
                        + "<p><strong>Gender:</strong> " + escapeHtml(gender) + "</p>"
                        + "<p><strong>Treatment Stage:</strong> " + escapeHtml(stage) + "</p>"
                        + "<p><strong>Date:</strong> " + escapeHtml(sessionDate) + "</p>"
                        + "<p><strong>Type:</strong> " + escapeHtml(serviceType) + "</p>"
                        + "<p><strong>Duration:</strong> " + escapeHtml(duration) + "</p>"
                        + "</div>";
        // ClientHub's report shows Session Focus, Symptoms, Short-Term Goals and
        // the rest because its AI writes them into the note text. A note written
        // from another template (SOAP, DAP) or typed by hand does not, so any
        // field the text is missing is supplied from the record, in ClientHub's
        // order, ahead of the narrative.
        String missingFieldSections = StringUtils.hasText(noteBody)
                ? buildMissingClinicalSections(note, rawNoteContent)
                : "";
        String clinicalSection = StringUtils.hasText(noteBody)
                ? clientInfoBlock + "<div class=\"content\">" + missingFieldSections + noteBody + "</div>"
                : clientInfoBlock + "<div class=\"clinical-content\">" + buildClinicalFields(note) + "</div>";

        String statusBadge = finalized
                ? "<span class=\"status-badge status-finalized\">Finalized</span>"
                : "<span class=\"status-badge status-draft\">Draft</span>";
        String finalizedMeta = finalizedDate.isEmpty() ? "" :
                "<div class=\"meta-item\"><div class=\"meta-label\">Finalized</div><div class=\"meta-value\">"
                        + escapeHtml(finalizedDate) + "</div></div>";
        String signedLine = finalized
                ? "<div class=\"signature-date\">Digitally signed on " + escapeHtml(finalizedDate.isEmpty() ? todayDate : finalizedDate) + "</div>"
                : "";

        // Changes made after signing travel with the note: whoever receives the
        // PDF must see them, each with its reason, author and time.
        StringBuilder amendmentsHtml = new StringBuilder();
        List<SessionNoteAmendment> amendments = note.getId() == null
                ? List.of()
                : sessionNoteAmendmentRepository.findBySessionNoteId(note.getId());
        if (!amendments.isEmpty()) {
            amendmentsHtml.append("<div class=\"section amendments\"><div class=\"section-title\">Amendments (")
                    .append(amendments.size()).append(")</div>");
            for (SessionNoteAmendment amendment : amendments) {
                Instant amendedAt = amendment.getSignedAt() != null ? amendment.getSignedAt() : amendment.getCreatedAt();
                String author = amendment.getCreatedByUser() != null
                        && StringUtils.hasText(amendment.getCreatedByUser().getFullName())
                        ? amendment.getCreatedByUser().getFullName()
                        : "Clinician not recorded";
                amendmentsHtml.append("<div class=\"amendment\">")
                        .append("<div class=\"amendment-reason\">")
                        .append(escapeHtml(StringUtils.hasText(amendment.getReason()) ? amendment.getReason().trim() : "Amendment"))
                        .append("</div><div class=\"amendment-text\">")
                        .append(escapeHtml(amendment.getAmendmentText() == null ? "" : amendment.getAmendmentText().trim())
                                .replaceAll("\r?\n", "<br>"))
                        .append("</div><div class=\"amendment-meta\">")
                        .append(escapeHtml(author))
                        .append(amendedAt != null ? " · " + escapeHtml(sessionDateFmt.format(amendedAt.atZone(practiceZone))) : "")
                        .append("</div></div>");
            }
            amendmentsHtml.append("</div>");
        }

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Session Note - %s</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body { font-family: 'Inter', ui-sans-serif, system-ui, -apple-system, 'Segoe UI', Arial, sans-serif; padding: 20px 30px; line-height: 1.5; color: #333; font-size: 13px; }
                        .page { max-width: 800px; margin: 0 auto; overflow-wrap: anywhere; word-break: break-word; }
                        .header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 3px solid #3C4D58; padding-bottom: 12px; margin-bottom: 15px; gap: 16px; }
                        .header h1 { font-size: 24px; color: #3C4D58; }
                        .client-name { margin: 5px 0; color: #6b7280; font-size: 15px; }
                        .header-right { text-align: right; color: #4b5563; font-size: 13px; min-width: 0; }
                        .header-right p { margin: 4px 0; }
                        .practice-name { font-weight: 600; color: #3C4D58; font-size: 16px; margin-bottom: 2px; }
                        .status-badge { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 999px; vertical-align: middle; margin-left: 8px; }
                        .status-finalized { background: #E0EAED; color: #3C4D58; }
                        .status-draft { background: #fef3c7; color: #92400e; }
                        .confidentiality-banner { background: #fef9c3; border-left: 4px solid #f59e0b; color: #92400e; padding: 10px 14px; text-align: center; font-size: 11px; font-weight: 600; letter-spacing: .02em; margin-bottom: 15px; }
                        .meta-info { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 24px; background: #f3f4f6; border-radius: 8px; padding: 14px 18px; margin-bottom: 18px; }
                        .meta-label { font-size: 10px; font-weight: 600; letter-spacing: .05em; text-transform: uppercase; color: #6b7280; }
                        .meta-value { font-size: 13px; color: #111827; }
                        .section { margin: 18px 0; min-width: 0; }
                        .section-title { font-size: 16px; font-weight: 700; color: #3C4D58; border-bottom: 2px solid #E0EAED; padding-bottom: 4px; margin-bottom: 10px; }
                        .content { font-size: 13px; color: #1f2937; overflow-wrap: anywhere; word-break: break-word; }
                        .content p { margin: 0 0 8px; }
                        .client-info { border-bottom: 1px solid #e5e7eb; padding-bottom: 8px; margin-bottom: 10px; }
                        .client-info p { margin: 0 0 3px; }
                        .content .section-label { display: block; margin-top: 16px; padding-top: 10px; border-top: 2px solid #e5e7eb; font-weight: 600; color: #3C4D58; }
                        .content > .section-label:first-child { margin-top: 4px; padding-top: 0; border-top: none; }
                        .clinical-content { min-width: 0; }
                        .clinical-field { margin-bottom: 12px; min-width: 0; }
                        .field-label { font-weight: bold; color: #3C4D58; font-size: 12px; margin-bottom: 3px; }
                        .field-content { background: #f9fafb; padding: 8px 10px; border-left: 3px solid #3C4D58; overflow-wrap: anywhere; word-break: break-word; white-space: pre-wrap; }
                        .risk-overall { background: #f3f4f6; padding: 12px 15px; border-radius: 6px; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center; }
                        table { width: 100%%; border-collapse: collapse; font-size: 13px; table-layout: fixed; }
                        th, td { border-bottom: 1px solid #e5e7eb; padding: 7px 10px; text-align: left; overflow-wrap: anywhere; vertical-align: top; }
                        th { background: #f9fafb; font-weight: 600; color: #374151; font-size: 11px; text-transform: uppercase; letter-spacing: .04em; }
                        .signature { margin-top: 28px; border-top: 1px solid #e5e7eb; padding-top: 14px; }
                        .signature-name { font-weight: 700; font-size: 14px; color: #111827; }
                        .signature-license { color: #6b7280; font-size: 12px; }
                        .signature-date { color: #9ca3af; font-size: 11px; font-style: italic; margin-top: 3px; }
                        .amendments { margin-top: 22px; }
                        .amendment { border: 1px solid #e5e7eb; border-left: 3px solid #3C4D58; border-radius: 6px; background: #f9fafb; padding: 10px 12px; margin-top: 10px; page-break-inside: avoid; }
                        .amendment-reason { font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: .04em; color: #3C4D58; }
                        .amendment-text { margin-top: 4px; color: #111827; }
                        .amendment-meta { margin-top: 6px; font-size: 11px; color: #6b7280; }
                        .footer { display: flex; justify-content: space-between; border-top: 1px solid #e5e7eb; margin-top: 22px; padding-top: 8px; font-size: 11px; color: #6b7280; }
                    </style>
                </head>
                <body>
                    <div class="page">
                        <div class="header">
                            <div class="header-left">
                                <h1>Session Note %s</h1>
                                <p class="client-name">%s</p>
                            </div>
                            <div class="header-right">
                                <div class="practice-name">%s</div>
                                %s
                            </div>
                        </div>

                        <div class="confidentiality-banner">
                            PERSONAL AND CONFIDENTIAL – PROTECTED HEALTH INFORMATION. UNAUTHORIZED USE OR DISCLOSURE IS PROHIBITED UNDER HIPAA.
                        </div>

                        <div class="meta-info">
                            <div class="meta-item"><div class="meta-label">Session Date</div><div class="meta-value">%s</div></div>
                            <div class="meta-item"><div class="meta-label">Service Type</div><div class="meta-value">%s</div></div>
                            <div class="meta-item"><div class="meta-label">Therapist</div><div class="meta-value">%s</div></div>
                            <div class="meta-item"><div class="meta-label">Room</div><div class="meta-value">%s</div></div>
                            %s
                        </div>

                        <div class="section">
                            <div class="section-title">Clinical Documentation</div>
                            %s
                        </div>

                        <div class="section">
                            <div class="section-title">Risk Assessment</div>
                            <div class="risk-overall">
                                <span style="font-weight: 600; color: #374151;">Overall Risk Level:</span>
                                <span style="font-weight: 700; font-size: 15px; color: %s;">%s (%d/40)</span>
                            </div>
                            <table>
                                <thead>
                                    <tr><th>Risk Factor</th><th style="width: 90px;">Score</th><th style="width: 110px;">Level</th></tr>
                                </thead>
                                <tbody>
                                    %s
                                </tbody>
                            </table>
                        </div>

                        <div class="signature">
                            <div class="signature-name">%s</div>
                            <div class="signature-license">%s</div>
                            %s
                        </div>

                        %s

                        <div class="footer">
                            <span>%s</span>
                            <span>%s</span>
                        </div>
                    </div>
                </body>
                </html>
                """,
            escapeHtml(clientName),
            statusBadge,
            escapeHtml(clientName),
            escapeHtml(orgName),
            practiceLines.toString(),
            escapeHtml(sessionDate),
            escapeHtml(serviceType),
            escapeHtml(therapistName),
            escapeHtml(roomName),
            finalizedMeta,
            clinicalSection,
            overallRiskColor,
            overallRisk,
            totalRisk,
            riskTable.toString(),
            escapeHtml(therapistName),
            escapeHtml(therapistLicense),
            signedLine,
            amendmentsHtml.toString(),
            escapeHtml(clientName),
            escapeHtml(todayDate)
        );
    }

    /** The note the clinician signed: final first, then generated, then draft. */
    private String resolveFinalNoteContent(SessionNote note) {
        if (StringUtils.hasText(note.getFinalContent())) return note.getFinalContent();
        if (StringUtils.hasText(note.getGeneratedContent())) return note.getGeneratedContent();
        if (StringUtils.hasText(note.getDraftContent())) return note.getDraftContent();
        return "";
    }

    /**
     * The final note is rich text from an editor or plain text from the AI, and
     * it goes into a printed document, so it is never inserted unsanitized.
     */
    private String buildFinalNoteBodyHtml(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return "";
        }
        String trimmed = rawContent.trim();
        boolean hasMarkup = trimmed.matches("(?is).*</?[a-z][^>]*>.*");
        String html = hasMarkup
                ? org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.relaxed())
                : escapeHtml(trimmed).replaceAll("\r?\n", "<br>");
        // Style the labels the way the ClientHub report does, whether the text
        // arrived as editor paragraphs or as AI plain text joined with <br>.
        String[] sectionLabels = {"Session Focus:", "Symptoms:", "Short-Term Goals:", "Intervention:",
                "Progress Remarks:", "Progress:", "Recommendations:", "Additional Notes:",
                "Subjective:", "Objective:", "Assessment:", "Plan:"};
        for (String label : sectionLabels) {
            html = html.replace("<p>" + label, "<p class=\"section-label\">" + label);
            html = html.replaceAll("(?m)(^|<br>)" + java.util.regex.Pattern.quote(label),
                    "$1<span class=\"section-label\">" + label + "</span>");
        }
        html = html.replaceAll("(?:<br>\\s*)+(<span class=\"section-label\">)", "$1")
                .replaceAll("(<span class=\"section-label\">[^<]*</span>)<br>", "$1");
        String[] identityLabels = {"CLIENT INFORMATION", "SESSION INFORMATION", "Name:", "Client ID:",
                "Age:", "Gender:", "Treatment Stage:", "Date:", "Type:", "Duration:"};
        for (String label : identityLabels) {
            html = html.replaceAll("(?m)(^|<br>|<p>)" + java.util.regex.Pattern.quote(label),
                    "$1<strong>" + label + "</strong>");
        }
        return html;
    }

    /**
     * Clinical fields present on the record but absent from the note text, as
     * report sections. A field is considered present in the text when any of
     * the labels the AI or ClientHub use for it opens a line, either as
     * "Label:" or as a heading on its own ("Session Focus").
     */
    private String buildMissingClinicalSections(SessionNote note, String rawNoteContent) {
        // Block ends and line breaks become newlines so a heading starts a line.
        String textLower = rawNoteContent == null ? ""
                : rawNoteContent
                        .replaceAll("(?i)<br\\s*/?>|</(p|div|h[1-6]|li)>", "\n")
                        .replaceAll("<[^>]*>", " ")
                        .replace("&nbsp;", " ")
                        .toLowerCase(Locale.US);
        Object[][] fields = {
                {"Session Focus:", note.getSessionFocus(), new String[]{"session focus"}},
                {"Symptoms:", note.getSymptoms(), new String[]{"symptoms"}},
                {"Short-Term Goals:", note.getShortTermGoals(), new String[]{"short-term goals", "short term goals"}},
                {"Intervention:", note.getIntervention(), new String[]{"intervention", "interventions"}},
                {"Progress:", note.getProgress(), new String[]{"progress", "progress remarks"}},
                {"Progress Remarks:", note.getRemarks(), new String[]{"progress remarks", "remarks", "additional notes"}},
                {"Recommendations:", note.getRecommendations(), new String[]{"recommendations"}},
        };
        StringBuilder sb = new StringBuilder();
        for (Object[] field : fields) {
            String label = (String) field[0];
            String value = (String) field[1];
            String[] aliases = (String[]) field[2];
            if (!hasContent(value)) {
                continue;
            }
            boolean inText = false;
            for (String alias : aliases) {
                // "Progress" must not match a "Progress Remarks" heading, so the
                // label has to end the line or be followed by a colon.
                if (java.util.regex.Pattern.compile("(?m)^[\\s*#]*" + java.util.regex.Pattern.quote(alias) + "[\\s*]*(:|$)")
                        .matcher(textLower).find()) {
                    inText = true;
                    break;
                }
            }
            if (inText) {
                continue;
            }
            sb.append("<p class=\"section-label\">").append(escapeHtml(label)).append("</p>")
                    .append("<p>").append(escapeHtml(value.trim()).replaceAll("\r?\n", "<br>")).append("</p>");
        }
        return sb.toString();
    }

    /** Whether the note text already opens with the AI-enriched identity header. */
    private boolean noteCarriesIdentityHeader(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return false;
        }
        String textOnly = rawContent.replaceAll("<[^>]*>", " ");
        int probe = Math.min(textOnly.length(), 300);
        return textOnly.substring(0, probe).toUpperCase(Locale.US).contains("CLIENT INFORMATION");
    }

    private String buildClinicalFields(SessionNote note) {
        StringBuilder sb = new StringBuilder();

        appendClinicalField(sb, "Session Focus:", note.getSessionFocus());
        appendClinicalField(sb, "Symptoms:", note.getSymptoms());
        appendClinicalField(sb, "Short-Term Goals:", note.getShortTermGoals());
        appendClinicalField(sb, "Intervention:", note.getIntervention());
        appendClinicalField(sb, "Progress:", note.getProgress());
        appendClinicalField(sb, "Progress Remarks:", note.getRemarks());
        appendClinicalField(sb, "Recommendations:", note.getRecommendations());

        return sb.toString();
    }

    private void appendClinicalField(StringBuilder sb, String label, String content) {
        if (!hasContent(content)) {
            return;
        }
        sb.append("<div class='clinical-field'><div class='field-label'>")
                .append(escapeHtml(label))
                .append("</div><div class='field-content'>")
                .append(formatFieldContent(content))
                .append("</div></div>");
    }

    private String formatFieldContent(String content) {
        return escapeHtml(content.trim());
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean hasContent(String content) {
        return content != null && !content.trim().isEmpty();
    }

    private void assertNotFinalized(SessionNote note, String operation) {
        if (Boolean.TRUE.equals(note.getIsFinalized())) {
            throw new BadRequestException("Cannot " + operation + " a finalized session note");
        }
    }

    private Map<String, Object> buildSessionNoteEventData(SessionNote note) {
        Map<String, Object> eventData = new java.util.HashMap<>();
        if (note == null) {
            return eventData;
        }
        Session session = note.getSession();
        eventData.put("id", note.getId());
        eventData.put("sessionNoteId", note.getId());
        eventData.put("sessionId", session != null ? session.getId() : null);
        eventData.put("clientId", note.getClient() != null ? note.getClient().getId() : null);
        NotificationPayloadFactory.putClientIdentity(eventData, note.getClient());
        eventData.put("therapistId", note.getTherapist() != null ? note.getTherapist().getId() : null);
        eventData.put("therapistName", note.getTherapist() != null ? note.getTherapist().getFullName() : "Unassigned");
        eventData.put("sessionDate", note.getDate() != null ? note.getDate() : (session != null ? session.getSessionDate() : null));
        return eventData;
    }

    /**
     * Record audit via {@link AuditLogService#write}.
     * Private {@code @Transactional(REQUIRES_NEW)} does not apply (self-invocation).
     */
    private void recordAuditEvent(Long userId, String username, Long resourceId, Long clientId,
                                  String action, String ipAddress, String userAgent, String details, Long sessionId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .result("success")
                    .resourceType(RESOURCE_TYPE_SESSION_NOTE)
                    .resourceId(resourceId != null ? resourceId.toString() : null)
                    .username(username)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .hipaaRelevant(true)
                    .riskLevel("high")
                    .timestamp(Instant.now())
                    .details(details + (sessionId != null ? ", sessionId: " + sessionId : ""))
                    .build();

            if (userId != null) {
                userRepository.findById(userId).ifPresent(user -> {
                    try {
                        auditLog.setUser(user);
                    } catch (Exception e) {
                        log.error("Failed to set user on audit log", e);
                    }
                });
            }
            if (clientId != null) {
                clientRepository.findById(clientId).ifPresent(client -> {
                    try {
                        auditLog.setClient(client);
                    } catch (Exception e) {
                        log.error("Failed to set client on audit log", e);
                    }
                });
            }

            auditLogService.write(auditLog);
        } catch (Exception e) {
            log.error("Failed to record audit event for session note: {}", resourceId, e);
        }
    }

    /**
     * Admin/practice timezone from Administration settings.
     * Falls back to UTC when unset; server timezone is never used.
     */
    private ZoneId resolvePracticeZoneId() {
        return timezoneService.findPracticeTimezoneId()
                .map(id -> {
                    try {
                        return ZoneId.of(id);
                    } catch (Exception ex) {
                        log.debug("Invalid practice timezone {}, falling back to UTC: {}",
                                id, ex.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .orElse(ZoneId.of("UTC"));
    }
}

package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.ai.service.AiService;
import com.smart.therapy.flow.ai.service.ConsentPolicyService;
import com.smart.therapy.flow.ai.service.OpenAiClient;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.TranscriptionFailedException;
import com.smart.therapy.flow.common.exception.TranscriptionServiceUnavailableException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.session.dto.*;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionTranscript;
import com.smart.therapy.flow.session.entity.SessionTranscriptChunk;
import com.smart.therapy.flow.session.enums.SessionTranscriptChunkStatus;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatus;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptChunkRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptRepository;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionTranscriptService {

    private static final String RESOURCE_TYPE_SESSION_TRANSCRIPT = "session_transcript";
    private static final int MAX_CHUNK_INDEX = 500;

    private static final Pattern HALLUCINATION_PHRASES = Pattern.compile(
            "(?i)\\b(thank you for watching|thanks for watching|subscribe to my channel|hit the like button|"
                    + "captions by|music by|visit our website)\\b");
    private static final Pattern REPETITIVE_TOKEN = Pattern.compile("\\b(\\w+)(?:\\s+\\1){5,}\\b", Pattern.CASE_INSENSITIVE);

    static final String NO_SMART_FILL_FIELDS_MESSAGE =
            "No structured session note fields could be extracted from this transcript. "
                    + "The recording does not contain definable clinical content such as session focus, symptoms, "
                    + "interventions, progress, recommendations, mood ratings, or risk indicators. "
                    + "Please record an actual therapy session conversation and try again.";

    private final SessionRepository sessionRepository;
    private final SessionTranscriptRepository sessionTranscriptRepository;
    private final SessionTranscriptChunkRepository sessionTranscriptChunkRepository;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;
    private final ConsentPolicyService consentPolicyService;
    private final AiService aiService;
    private final OpenAiClient openAiClient;
    private final TranscriptChunkRateLimiter transcriptChunkRateLimiter;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    @Value("${app.transcripts.retention-days:30}")
    private int defaultRetentionDays;

    @Value("${app.transcripts.max-expected-chunks:5000}")
    private int maxExpectedChunks;

    @Value("${app.transcripts.max-chunk-chars:40000}")
    private int maxChunkChars;

    @Value("${app.transcripts.language-allowlist:auto,en,en-us,en-gb,es,fr,de,it,pt,nl,ru,hi,zh,ja,ko,tr,pl,ar,multi,ur}")
    private String languageAllowlist;

    @Transactional
    public TranscribeStartResponse startUpload(Long sessionId, TranscribeStartRequest request, AuthPrincipal requester,
                                               String ipAddress, String userAgent) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(requester, "Requester is required");

        User actor = currentUserService.requireCurrentUser(requester);
        Session session = sessionRepository.findByIdWithRelations(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        validateSessionAccess(session, requester, actor.getId());
        enforceAiConsent(session.getClient().getId(), actor.getId(), ipAddress, userAgent);

        String normalizedLanguage = normalizeLanguage(
                request != null && StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : "auto");
        boolean translateToEnglish = request != null && Boolean.TRUE.equals(request.getTranslateToEnglish());
        Instant now = Instant.now();

        Integer expectedChunks = request != null ? request.getExpectedChunks() : null;
        if (expectedChunks != null && (expectedChunks <= 0 || expectedChunks > maxExpectedChunks)) {
            throw new BadRequestException("expectedChunks out of range; must be between 1 and " + maxExpectedChunks);
        }

        int retentionDays = request != null && request.getRetentionDays() != null
                ? request.getRetentionDays()
                : defaultRetentionDays;

        SessionTranscript.SessionTranscriptBuilder transcriptBuilder = SessionTranscript.builder()
                .session(session)
                .client(session.getClient())
                .uploader(actor)
                .uploadId(generateUploadId())
                .status(SessionTranscriptStatus.RECORDING)
                .language(normalizedLanguage)
                .translatedToEnglish(translateToEnglish)
                .receivedChunks(0)
                .expiresAt(now.plusSeconds(retentionDays * 24L * 60L * 60L));

        if (expectedChunks != null) {
            transcriptBuilder.expectedChunks(expectedChunks);
        }

        SessionTranscript transcript = transcriptBuilder.build();

        SessionTranscript saved = sessionTranscriptRepository.save(transcript);
        recordAuditEvent(actor.getId(), actor.getEmail(), saved.getId(), session.getClient().getId(),
                "session_transcript_started", ipAddress, userAgent,
                "Started transcript uploadId=" + saved.getUploadId(), session.getId());

        return TranscribeStartResponse.builder()
                .uploadId(saved.getUploadId())
                .build();
    }

    @Transactional
    public TranscribeChunkResponse processChunk(Long sessionId,
                                                String uploadId,
                                                Integer chunkIndex,
                                                Double chunkDurationSeconds,
                                                byte[] audioData,
                                                String fileName,
                                                String languageOverride,
                                                AuthPrincipal requester,
                                                String ipAddress,
                                                String userAgent) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(requester, "Requester is required");

        if (!StringUtils.hasText(uploadId) || !uploadId.startsWith("srv-")) {
            throw new BadRequestException("Invalid uploadId");
        }
        if (chunkIndex == null || chunkIndex < 0 || chunkIndex > MAX_CHUNK_INDEX) {
            throw new BadRequestException("chunkIndex out of range; expected 0.." + MAX_CHUNK_INDEX);
        }
        if (audioData == null || audioData.length == 0) {
            throw new BadRequestException("Missing audio chunk");
        }

        User actor = currentUserService.requireCurrentUser(requester);
        transcriptChunkRateLimiter.checkAndIncrement(actor.getId(), sessionId);

        SessionTranscript transcript = sessionTranscriptRepository.findByUploadIdForUpdate(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript upload not found"));

        validateTranscriptBelongsToSession(transcript, sessionId);
        validateSessionAccess(transcript.getSession(), requester, actor.getId());
        validateTranscriptOwnership(transcript, requester, actor.getId());
        validateTranscriptAcceptingChunks(transcript);
        enforceAiConsent(transcript.getClient().getId(), actor.getId(), ipAddress, userAgent);

        String previousText = sessionTranscriptChunkRepository
                .findByTranscriptIdAndChunkIndex(transcript.getId(), chunkIndex - 1)
                .map(SessionTranscriptChunk::getChunkText)
                .orElse(null);

        String effectiveLanguage = StringUtils.hasText(languageOverride)
                ? normalizeLanguage(languageOverride)
                : transcript.getLanguage();

        String chunkText;
        try {
            chunkText = openAiClient.transcribeSessionChunk(
                    audioData,
                    resolveAudioFileName(fileName),
                    effectiveLanguage,
                    previousText,
                    Boolean.TRUE.equals(transcript.getTranslatedToEnglish()));
        } catch (TranscriptionServiceUnavailableException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            Throwable cause = ex;
            while (cause != null) {
                if (cause instanceof TranscriptionServiceUnavailableException unavailable) {
                    throw unavailable;
                }
                cause = cause.getCause();
            }
            throw new TranscriptionFailedException("Chunk transcription failed", ex);
        }

        chunkText = sanitizeWhisperHallucinations(chunkText);
        chunkText = collapseRepetitiveHallucinations(chunkText);
        if (StringUtils.hasText(chunkText) && chunkText.length() > maxChunkChars) {
            chunkText = chunkText.substring(0, maxChunkChars);
        }

        SessionTranscriptChunkStatus chunkStatus = StringUtils.hasText(chunkText)
                ? SessionTranscriptChunkStatus.RECEIVED
                : SessionTranscriptChunkStatus.SILENT;

        SessionTranscriptChunk chunk = sessionTranscriptChunkRepository
                .findByTranscriptIdAndChunkIndex(transcript.getId(), chunkIndex)
                .orElseGet(() -> SessionTranscriptChunk.builder()
                        .transcript(transcript)
                        .chunkIndex(chunkIndex)
                        .build());

        chunk.setChunkStatus(chunkStatus);
        chunk.setChunkText(StringUtils.hasText(chunkText) ? chunkText.trim() : null);
        chunk.setChunkDurationSeconds(chunkDurationSeconds);
        chunk.setFailureReason(null);
        chunk.setReceivedAt(Instant.now());
        sessionTranscriptChunkRepository.save(chunk);

        int chunksReceived = (int) sessionTranscriptChunkRepository.countByTranscriptId(transcript.getId());
        transcript.setReceivedChunks(chunksReceived);
        transcript.setFailureReason(null);
        sessionTranscriptRepository.save(transcript);

        recordAuditEvent(actor.getId(), actor.getEmail(), transcript.getId(), transcript.getClient().getId(),
                "session_transcript_chunk_transcribed", ipAddress, userAgent,
                "Chunk index=" + chunkIndex + ", chunkStatus=" + chunkStatus.getValue(), transcript.getSession().getId());

        return TranscribeChunkResponse.builder()
                .uploadId(transcript.getUploadId())
                .chunkIndex(chunkIndex)
                .chunkText(chunk.getChunkText() != null ? chunk.getChunkText() : "")
                .chunksReceived(chunksReceived)
                .build();
    }

    @Transactional
    public TranscribeFinalizeResponse finalizeUpload(Long sessionId, TranscribeFinalizeRequest request, AuthPrincipal requester,
                                                     String ipAddress, String userAgent) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(request, "Request is required");
        Objects.requireNonNull(requester, "Requester is required");

        User actor = currentUserService.requireCurrentUser(requester);
        SessionTranscript transcript = sessionTranscriptRepository.findByUploadIdForUpdate(request.getUploadId())
                .orElseThrow(() -> new ResourceNotFoundException("Transcript upload not found"));

        validateTranscriptBelongsToSession(transcript, sessionId);
        validateSessionAccess(transcript.getSession(), requester, actor.getId());
        validateTranscriptOwnership(transcript, requester, actor.getId());

        if (transcript.getStatus() == SessionTranscriptStatus.READY
                && transcript.getDurationSeconds() != null
                && transcript.getWordCount() != null) {
            SessionTranscript withSpeakers = tryAutoDiarizeAfterFinalize(
                    transcript, actor, ipAddress, userAgent);
            return toFinalizeResponse(withSpeakers);
        }

        if (transcript.getExpiresAt() != null && transcript.getExpiresAt().isBefore(Instant.now())) {
            transcript.setStatus(SessionTranscriptStatus.EXPIRED);
            sessionTranscriptRepository.save(transcript);
            throw new BadRequestException("Transcript upload has expired");
        }

        int expectedChunks = request.getExpectedChunks();
        if (expectedChunks <= 0 || expectedChunks > maxExpectedChunks) {
            throw new BadRequestException("expectedChunks out of range; must be between 1 and " + maxExpectedChunks);
        }

        transcript.setStatus(SessionTranscriptStatus.PROCESSING);
        transcript.setExpectedChunks(expectedChunks);
        sessionTranscriptRepository.save(transcript);

        List<SessionTranscriptChunk> chunks = sessionTranscriptChunkRepository
                .findByTranscriptIdOrderByChunkIndexAsc(transcript.getId());
        Map<Integer, SessionTranscriptChunk> byIndex = chunks.stream()
                .collect(Collectors.toMap(SessionTranscriptChunk::getChunkIndex, c -> c, (a, b) -> a));

        Map<Integer, Double> silentByIndex = new HashMap<>();
        if (request.getSilentChunks() != null) {
            for (SilentChunkDto silent : request.getSilentChunks()) {
                if (silent != null && silent.getIndex() != null) {
                    silentByIndex.put(silent.getIndex(), silent.getDurationSeconds());
                }
            }
        }

        int maxIndex = Math.min(expectedChunks - 1, MAX_CHUNK_INDEX);
        int accountedFor = 0;
        for (int i = 0; i <= maxIndex; i++) {
            if (byIndex.containsKey(i) || silentByIndex.containsKey(i)) {
                accountedFor++;
            }
        }

        if (accountedFor < expectedChunks) {
            Map<String, Object> details = Map.of(
                    "chunksReceived", accountedFor,
                    "chunksExpected", expectedChunks);
            throw new ConflictException(
                    "Cannot finalize: only " + accountedFor + " of " + expectedChunks + " chunks were accounted for",
                    details);
        }

        StringBuilder stitched = new StringBuilder();
        double cumulativeSeconds = 0;
        int totalDurationSeconds = 0;

        for (int i = 0; i <= maxIndex; i++) {
            String timestamp = formatTimestamp(cumulativeSeconds);
            SessionTranscriptChunk chunk = byIndex.get(i);
            Double silentDuration = silentByIndex.get(i);
            double segmentDuration = resolveSegmentDuration(chunk, silentDuration);

            if (chunk != null && StringUtils.hasText(chunk.getChunkText())) {
                stitched.append('[').append(timestamp).append("]\nTherapist: ")
                        .append(chunk.getChunkText().trim()).append("\n\n");
            } else if (chunk != null) {
                int gapSeconds = (int) Math.round(segmentDuration);
                stitched.append('[').append(timestamp).append("]\n[GAP IN RECORDING ~")
                        .append(gapSeconds).append("s — audio was unintelligible]\n\n");
            } else if (silentByIndex.containsKey(i)) {
                int silenceSeconds = (int) Math.round(segmentDuration);
                stitched.append('[').append(timestamp).append("]\n[silence ~")
                        .append(silenceSeconds)
                        .append("s — microphone was muted or no speech]\n\n");
            } else {
                stitched.append('[').append(timestamp).append("]\n[GAP IN RECORDING — chunk ")
                        .append(i).append(" missing]\n\n");
            }

            cumulativeSeconds += segmentDuration;
            totalDurationSeconds += (int) Math.round(segmentDuration);
        }

        String rawContent = stitched.toString().trim();
        String finalTranscript = sanitizeWhisperHallucinations(rawContent);
        finalTranscript = collapseRepetitiveHallucinations(finalTranscript);

        transcript.setRawContent(rawContent);
        transcript.setFinalTranscript(finalTranscript);
        transcript.setReceivedChunks(chunks.size());
        transcript.setDurationSeconds(totalDurationSeconds);
        transcript.setWordCount(countWords(finalTranscript));
        transcript.setStatus(SessionTranscriptStatus.READY);
        transcript.setFinalizedAt(Instant.now());
        transcript.setFailureReason(null);
        SessionTranscript saved = sessionTranscriptRepository.save(transcript);

        for (SessionTranscriptChunk chunk : chunks) {
            chunk.softDelete();
        }
        sessionTranscriptChunkRepository.saveAll(chunks);

        recordAuditEvent(actor.getId(), actor.getEmail(), saved.getId(), saved.getClient().getId(),
                "session_transcript_finalized", ipAddress, userAgent,
                "Finalized transcript uploadId=" + saved.getUploadId(), saved.getSession().getId());

        SessionTranscript withSpeakers = tryAutoDiarizeAfterFinalize(saved, actor, ipAddress, userAgent);
        return toFinalizeResponse(withSpeakers);
    }

    @Transactional
    public SessionTranscriptResponse getSessionTranscript(Long sessionId, AuthPrincipal requester) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(requester, "Requester is required");

        User actor = currentUserService.requireCurrentUser(requester);
        SessionTranscript transcript = findLatestTranscriptBySession(sessionId);
        validateSessionAccess(transcript.getSession(), requester, actor.getId());

        recordAuditEvent(actor.getId(), actor.getEmail(), transcript.getId(), transcript.getClient().getId(),
                "session_transcript_viewed", null, "session-transcript-api",
                "Viewed session transcript; status="
                        + (transcript.getStatus() != null ? transcript.getStatus().getValue() : "unknown"), sessionId);
        return toTranscriptResponse(transcript, transcript.getStatus() != SessionTranscriptStatus.READY);
    }

    @Transactional(readOnly = true)
    public SessionTranscript validateLiveUploadAccess(String uploadId, AuthPrincipal requester) {
        Objects.requireNonNull(uploadId, "Upload id is required");
        Objects.requireNonNull(requester, "Requester is required");

        User actor = currentUserService.requireCurrentUser(requester);
        SessionTranscript transcript = sessionTranscriptRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript upload not found"));
        validateSessionAccess(transcript.getSession(), requester, actor.getId());
        validateTranscriptOwnership(transcript, requester, actor.getId());
        if (transcript.getStatus() != SessionTranscriptStatus.RECORDING) {
            throw new BadRequestException("Upload is no longer accepting live stream audio (status: "
                    + transcript.getStatus().getValue() + ")");
        }
        return transcript;
    }

    @Transactional
    public String downloadSessionTranscriptText(Long sessionId, AuthPrincipal requester) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(requester, "Requester is required");
        User actor = currentUserService.requireCurrentUser(requester);
        SessionTranscript transcript = findLatestTranscriptBySession(sessionId);
        validateSessionAccess(transcript.getSession(), requester, actor.getId());
        recordAuditEvent(actor.getId(), actor.getEmail(), transcript.getId(), transcript.getClient().getId(),
                "session_transcript_downloaded", null, "session-transcript-api",
                "Downloaded session transcript; format=text", sessionId);
        return StringUtils.hasText(transcript.getFinalTranscript()) ? transcript.getFinalTranscript() : "";
    }

    /**
     * Triggers GPT-4o speaker diarization on a finalized session transcript.
     *
     * Design decisions:
     * - Idempotent: if diarizedTranscript already exists, returns it immediately without calling GPT again.
     * - HIPAA-safe: client name is NOT passed to the AI. Only the spoken text goes to OpenAI.
     * - Requires AI consent to have been granted by the client (enforceAiConsent).
     * - The original finalTranscript is NEVER modified — diarizedTranscript is a separate field.
     * - Records a dedicated audit event for compliance tracking.
     */
    @Transactional
    public SessionTranscriptResponse diarizeTranscript(Long sessionId, AuthPrincipal requester,
                                                       String ipAddress, String userAgent) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(requester, "Requester is required");

        User actor = currentUserService.requireCurrentUser(requester);
        SessionTranscript transcript = findLatestTranscriptBySession(sessionId);
        validateSessionAccess(transcript.getSession(), requester, actor.getId());

        if (transcript.getStatus() != SessionTranscriptStatus.READY) {
            throw new BadRequestException("Transcript must be finalized before diarization (status: "
                    + (transcript.getStatus() != null ? transcript.getStatus().getValue() : "unknown") + ")");
        }

        Long clientId = transcript.getClient() != null ? transcript.getClient().getId() : null;
        enforceAiConsent(clientId, actor.getId(), ipAddress, userAgent);

        // Idempotency: return existing diarized transcript if already available.
        // Clear cached OpenAI safety refusals so Identify Speakers can be retried.
        if (StringUtils.hasText(transcript.getDiarizedTranscript())) {
            if (AiService.isInvalidDiarizationResult(transcript.getDiarizedTranscript())) {
                log.warn("[Diarization] Clearing cached invalid/refusal diarization for sessionId={}", sessionId);
                transcript.setDiarizedTranscript(null);
                sessionTranscriptRepository.save(transcript);
            } else {
                log.info("[Diarization] Returning cached diarized transcript for sessionId={}", sessionId);
                recordAuditEvent(actor.getId(), actor.getEmail(), transcript.getId(), clientId,
                        "session_transcript_diarization_retrieved", ipAddress, userAgent,
                        "Retrieved cached diarized transcript for sessionId=" + sessionId, sessionId);
                return toTranscriptResponse(transcript, false);
            }
        }

        String finalTranscript = transcript.getFinalTranscript();
        if (!StringUtils.hasText(finalTranscript)) {
            throw new BadRequestException("No finalized transcript content available for diarization");
        }

        log.info("[Diarization] Starting AI speaker diarization for sessionId={}, transcriptId={}",
                sessionId, transcript.getId());

        // Call GPT-4o — client name is intentionally NOT passed (HIPAA compliance)
        String diarized = aiService.diarizeTranscript(finalTranscript);

        if (!StringUtils.hasText(diarized) || AiService.isInvalidDiarizationResult(diarized)) {
            throw new BadRequestException("AI diarization returned an empty result — please try again");
        }

        transcript.setDiarizedTranscript(diarized);
        SessionTranscript saved = sessionTranscriptRepository.save(transcript);

        log.info("[Diarization] Diarization complete for sessionId={}, transcriptId={}", sessionId, transcript.getId());

        recordAuditEvent(actor.getId(), actor.getEmail(), saved.getId(), clientId,
                "session_transcript_diarized", ipAddress, userAgent,
                "AI speaker diarization completed for sessionId=" + sessionId
                        + "; model=gpt-4o; clientNameSentToAI=false", sessionId);

        return toTranscriptResponse(saved, false);
    }

    @Transactional
    public TranscriptSmartFillResponse smartFillFromTranscript(Long sessionId, AuthPrincipal requester) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(requester, "Requester is required");

        User actor = currentUserService.requireCurrentUser(requester);
        Session session = sessionRepository.findByIdWithRelations(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        validateSessionAccess(session, requester, actor.getId());

        SessionTranscript transcript = findLatestTranscriptBySession(sessionId);
        if (transcript.getStatus() != SessionTranscriptStatus.READY) {
            throw new BadRequestException("Transcript is not finalized yet (status: "
                    + (transcript.getStatus() != null ? transcript.getStatus().getValue() : "unknown") + ")");
        }

        Long clientId = transcript.getClient() != null ? transcript.getClient().getId() : session.getClient().getId();
        enforceAiConsent(clientId, actor.getId(), null, "session-transcript-smart-fill");

        String transcriptText = transcript.getFinalTranscript();
        if (!StringUtils.hasText(transcriptText)) {
            throw new BadRequestException("No finalized transcript content found for this session");
        }

        String clientName = null;
        if (transcript.getClient() != null) {
            clientName = transcript.getClient().getFullName();
        } else if (session.getClient() != null) {
            clientName = session.getClient().getFullName();
        }

        Map<String, String> mappedFields = aiService.organizeTranscriptionIntoFieldsStrict(transcriptText, clientName);

        TranscriptSmartFillResponse.TranscriptSmartFillResponseBuilder responseBuilder =
                TranscriptSmartFillResponse.builder()
                        .sessionId(sessionId)
                        .uploadId(transcript.getUploadId())
                        .transcript(transcriptText)
                        .mappedFields(mappedFields);

        if (mappedFields.isEmpty()) {
            responseBuilder.message(NO_SMART_FILL_FIELDS_MESSAGE);
        }

        recordAuditEvent(actor.getId(), actor.getEmail(), transcript.getId(), clientId,
                "session_transcript_viewed", null, "session-transcript-smart-fill",
                "Used transcript for smart fill; mappedFieldCount=" + mappedFields.size(), sessionId);
        return responseBuilder.build();
    }

    @Transactional
    public void deleteSessionTranscript(Long sessionId, AuthPrincipal requester, String ipAddress, String userAgent) {
        Objects.requireNonNull(sessionId, "Session id is required");
        Objects.requireNonNull(requester, "Requester is required");

        User actor = currentUserService.requireCurrentUser(requester);
        SessionTranscript transcript = findLatestTranscriptBySession(sessionId);
        validateSessionAccess(transcript.getSession(), requester, actor.getId());
        validateTranscriptOwnership(transcript, requester, actor.getId());

        List<SessionTranscriptChunk> chunks = sessionTranscriptChunkRepository
                .findByTranscriptIdOrderByChunkIndexAsc(transcript.getId());
        for (SessionTranscriptChunk chunk : chunks) {
            chunk.softDelete();
        }
        sessionTranscriptChunkRepository.saveAll(chunks);

        transcript.softDelete();
        transcript.setStatus(SessionTranscriptStatus.DELETED);
        sessionTranscriptRepository.save(transcript);

        recordAuditEvent(actor.getId(), actor.getEmail(), transcript.getId(), transcript.getClient().getId(),
                "session_transcript_deleted", ipAddress, userAgent,
                "Deleted transcript uploadId=" + transcript.getUploadId(), transcript.getSession().getId());
    }

    @Transactional(readOnly = true)
    public List<SessionTranscriptStatusResponse> getTranscriptStatuses(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");

        User actor = currentUserService.requireCurrentUser(requester);
        List<SessionTranscript> all = sessionTranscriptRepository.findAllActiveOrderByUpdatedAtDesc();
        return all.stream()
                .filter(t -> canAccessSession(t.getSession(), requester, actor.getId()))
                .map(this::toStatusResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SessionTranscriptStatusResponse> getClientTranscriptStatuses(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client id is required");
        Objects.requireNonNull(requester, "Requester is required");

        User actor = currentUserService.requireCurrentUser(requester);
        List<SessionTranscript> all = sessionTranscriptRepository.findByClientIdOrderByUpdatedAtDesc(clientId);
        return all.stream()
                .filter(t -> canAccessSession(t.getSession(), requester, actor.getId()))
                .map(this::toStatusResponse)
                .toList();
    }

    private TranscribeFinalizeResponse toFinalizeResponse(SessionTranscript transcript) {
        return TranscribeFinalizeResponse.builder()
                .id(transcript.getId())
                .sessionId(transcript.getSession() != null ? transcript.getSession().getId() : null)
                .clientId(transcript.getClient() != null ? transcript.getClient().getId() : null)
                .content(transcript.getFinalTranscript())
                .diarizedTranscript(safeDiarizedTranscript(transcript.getDiarizedTranscript()))
                .status(transcript.getStatus() != null ? transcript.getStatus().getValue() : null)
                .language(transcript.getLanguage())
                .durationSeconds(transcript.getDurationSeconds())
                .wordCount(transcript.getWordCount())
                .uploadId(transcript.getUploadId())
                .build();
    }

    /**
     * Runs speaker diarization immediately after finalize so the transcript opens with
     * Therapist:/Client: labels by default. Soft-fails (consent/AI errors) so finalize still succeeds.
     */
    private SessionTranscript tryAutoDiarizeAfterFinalize(SessionTranscript transcript,
                                                          User actor,
                                                          String ipAddress,
                                                          String userAgent) {
        if (transcript == null) {
            return null;
        }
        if (StringUtils.hasText(transcript.getDiarizedTranscript())) {
            return transcript;
        }
        if (!StringUtils.hasText(transcript.getFinalTranscript())) {
            return transcript;
        }

        Long clientId = transcript.getClient() != null ? transcript.getClient().getId() : null;
        Long sessionId = transcript.getSession() != null ? transcript.getSession().getId() : null;

        try {
            if (clientId != null) {
                enforceAiConsent(clientId, actor.getId(), ipAddress, userAgent);
            }

            String diarized = aiService.diarizeTranscript(transcript.getFinalTranscript());
            if (!StringUtils.hasText(diarized) || AiService.isInvalidDiarizationResult(diarized)) {
                log.warn("[Diarization] Auto-diarize after finalize returned empty/invalid for sessionId={}", sessionId);
                return transcript;
            }

            transcript.setDiarizedTranscript(diarized);
            SessionTranscript saved = sessionTranscriptRepository.save(transcript);
            recordAuditEvent(actor.getId(), actor.getEmail(), saved.getId(), clientId,
                    "session_transcript_diarized", ipAddress, userAgent,
                    "AI speaker diarization auto-ran after finalize; sessionId=" + sessionId
                            + "; model=gpt-4o; clientNameSentToAI=false", sessionId);
            return saved;
        } catch (Exception ex) {
            log.warn("[Diarization] Auto-diarize after finalize skipped for sessionId={}: {}",
                    sessionId, ex.getMessage());
            return transcript;
        }
    }

    private SessionTranscript findLatestTranscriptBySession(Long sessionId) {
        return sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Session transcript not found"));
    }

    /**
     * Hide OpenAI safety refusals that were previously persisted as "diarized" text,
     * so the UI falls back to the original transcript + Identify Speakers.
     */
    private static String safeDiarizedTranscript(String diarized) {
        if (!StringUtils.hasText(diarized) || AiService.isInvalidDiarizationResult(diarized)) {
            return null;
        }
        return diarized;
    }

    private SessionTranscriptResponse toTranscriptResponse(SessionTranscript transcript, boolean includeChunks) {
        List<SessionTranscriptChunkResponse> chunkResponses = List.of();
        if (includeChunks) {
            chunkResponses = sessionTranscriptChunkRepository.findByTranscriptIdOrderByChunkIndexAsc(transcript.getId()).stream()
                    .map(c -> SessionTranscriptChunkResponse.builder()
                            .chunkIndex(c.getChunkIndex())
                            .chunkStatus(c.getChunkStatus() != null ? c.getChunkStatus().getValue() : null)
                            .chunkText(c.getChunkText())
                            .failureReason(c.getFailureReason())
                            .receivedAt(c.getReceivedAt())
                            .build())
                    .toList();
        }

        return SessionTranscriptResponse.builder()
                .transcriptId(transcript.getId())
                .sessionId(transcript.getSession() != null ? transcript.getSession().getId() : null)
                .clientId(transcript.getClient() != null ? transcript.getClient().getId() : null)
                .clientName(transcript.getClient() != null ? transcript.getClient().getFullName() : null)
                .uploadId(transcript.getUploadId())
                .status(transcript.getStatus() != null ? transcript.getStatus().getValue() : null)
                .language(transcript.getLanguage())
                .expectedChunks(transcript.getExpectedChunks())
                .receivedChunks(transcript.getReceivedChunks())
                .finalTranscript(transcript.getFinalTranscript())
                .diarizedTranscript(safeDiarizedTranscript(transcript.getDiarizedTranscript()))
                .content(transcript.getFinalTranscript())
                .durationSeconds(transcript.getDurationSeconds())
                .wordCount(transcript.getWordCount())
                .failureReason(transcript.getFailureReason())
                .startedAt(transcript.getCreatedAt())
                .finalizedAt(transcript.getFinalizedAt())
                .expiresAt(transcript.getExpiresAt())
                .updatedAt(transcript.getUpdatedAt())
                .chunks(chunkResponses)
                .build();
    }

    private SessionTranscriptStatusResponse toStatusResponse(SessionTranscript transcript) {
        return SessionTranscriptStatusResponse.builder()
                .transcriptId(transcript.getId())
                .sessionId(transcript.getSession() != null ? transcript.getSession().getId() : null)
                .clientId(transcript.getClient() != null ? transcript.getClient().getId() : null)
                .clientName(transcript.getClient() != null ? transcript.getClient().getFullName() : null)
                .uploadId(transcript.getUploadId())
                .status(transcript.getStatus() != null ? transcript.getStatus().getValue() : null)
                .expectedChunks(transcript.getExpectedChunks())
                .receivedChunks(transcript.getReceivedChunks())
                .startedAt(transcript.getCreatedAt())
                .finalizedAt(transcript.getFinalizedAt())
                .updatedAt(transcript.getUpdatedAt())
                .build();
    }

    private void validateTranscriptBelongsToSession(SessionTranscript transcript, Long sessionId) {
        Long transcriptSessionId = transcript.getSession() != null ? transcript.getSession().getId() : null;
        if (transcriptSessionId == null || !transcriptSessionId.equals(sessionId)) {
            throw new BadRequestException("uploadId does not belong to provided sessionId");
        }
    }

    private void validateTranscriptOwnership(SessionTranscript transcript, AuthPrincipal principal, Long requesterUserId) {
        if (transcript.getUploader() == null || transcript.getUploader().getId() == null) {
            throw new ForbiddenException("Transcript uploader metadata is missing");
        }

        boolean isAdmin = permissionChecker.hasRole(principal, "ADMIN")
                || permissionChecker.hasRole(principal, "SUPER_ADMIN");
        if (isAdmin) {
            return;
        }

        if (!transcript.getUploader().getId().equals(requesterUserId)) {
            throw new ForbiddenException("You can only modify transcript uploads created by you");
        }
    }

    private void validateTranscriptAcceptingChunks(SessionTranscript transcript) {
        if (transcript.getStatus() != SessionTranscriptStatus.RECORDING) {
            throw new ConflictException("Upload is no longer accepting chunks (status: "
                    + transcript.getStatus().getValue() + ")");
        }
        if (transcript.getExpiresAt() != null && transcript.getExpiresAt().isBefore(Instant.now())) {
            transcript.setStatus(SessionTranscriptStatus.EXPIRED);
            sessionTranscriptRepository.save(transcript);
            throw new BadRequestException("Transcript upload has expired");
        }
    }

    private double resolveSegmentDuration(SessionTranscriptChunk chunk, Double silentDuration) {
        if (chunk != null && chunk.getChunkDurationSeconds() != null) {
            return chunk.getChunkDurationSeconds();
        }
        if (silentDuration != null) {
            return silentDuration;
        }
        return 20.0;
    }

    private String formatTimestamp(double totalSeconds) {
        int total = Math.max(0, (int) Math.floor(totalSeconds));
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        int seconds = total % 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private int countWords(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private String resolveAudioFileName(String fileName) {
        if (StringUtils.hasText(fileName)) {
            return fileName;
        }
        return "chunk.webm";
    }

    private String normalizeLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            return "auto";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        Set<String> allow = Arrays.stream(languageAllowlist.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allow.contains(normalized)) {
            throw new BadRequestException("Unsupported language code: " + normalized);
        }
        return normalized;
    }

    private String sanitizeWhisperHallucinations(String transcript) {
        if (!StringUtils.hasText(transcript)) {
            return transcript;
        }
        String sanitized = HALLUCINATION_PHRASES.matcher(transcript).replaceAll("");
        sanitized = sanitized.replaceAll("(?m)^[\\s\\-:;,.]+$", "");
        sanitized = sanitized.replaceAll("\\n{3,}", "\n\n");
        return sanitized.trim();
    }

    private String collapseRepetitiveHallucinations(String transcript) {
        if (!StringUtils.hasText(transcript)) {
            return transcript;
        }
        return REPETITIVE_TOKEN.matcher(transcript).replaceAll("$1").trim();
    }

    private String generateUploadId() {
        return "srv-" + UUID.randomUUID().toString().replace("-", "");
    }

    private void enforceAiConsent(Long clientId, Long actorId, String ipAddress, String userAgent) {
        try {
            consentPolicyService.requireAiConsent(clientId);
        } catch (ForbiddenException ex) {
            recordAuditEvent(actorId, null, null, clientId, "ai_processing_blocked", ipAddress, userAgent,
                    ex.getMessage(), null);
            throw ex;
        }
    }

    private boolean canAccessSession(Session session, AuthPrincipal requester, Long requesterUserId) {
        try {
            validateSessionAccess(session, requester, requesterUserId);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void validateSessionAccess(Session session, AuthPrincipal requester, Long requesterUserId) {
        if (session == null || session.getTherapist() == null || session.getClient() == null) {
            throw new ResourceNotFoundException("Session not found");
        }

        boolean isAdmin = permissionChecker.hasRole(requester, "ADMIN")
                || permissionChecker.hasRole(requester, "SUPER_ADMIN");
        if (isAdmin || permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL")) {
            return;
        }

        Long sessionTherapistId = session.getTherapist().getId();
        if (sessionTherapistId != null && sessionTherapistId.equals(requesterUserId)) {
            return;
        }

        if (permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")) {
            List<Long> teamTherapists = supervisorAssignmentRepository.findBySupervisorId(requesterUserId).stream()
                    .map(SupervisorAssignment::getTherapist)
                    .filter(Objects::nonNull)
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (teamTherapists.contains(sessionTherapistId)) {
                return;
            }
        }

        if (permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN") && sessionTherapistId != null
                && sessionTherapistId.equals(requesterUserId)) {
            return;
        }

        throw new ForbiddenException("You do not have access to this session");
    }

    private void recordAuditEvent(Long userId, String username, Long resourceId, Long clientId,
                                  String action, String ipAddress, String userAgent, String details, Long sessionId) {
        try {
            String safeDetails = details != null ? details : "";
            String combinedDetails = sessionId != null ? safeDetails + ", sessionId: " + sessionId : safeDetails;
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .result("success")
                    .resourceType(RESOURCE_TYPE_SESSION_TRANSCRIPT)
                    .resourceId(resourceId != null ? resourceId.toString() : null)
                    .username(username)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .hipaaRelevant(true)
                    .riskLevel("high")
                    .timestamp(Instant.now())
                    .details(combinedDetails)
                    .build();

            if (userId != null) {
                userRepository.findById(userId).ifPresent(auditLog::setUser);
            }
            if (clientId != null) {
                clientRepository.findById(clientId).ifPresent(auditLog::setClient);
            }

            auditLogService.write(auditLog);
        } catch (Exception e) {
            log.error("Failed to record transcript audit event action={}", action, e);
        }
    }
}

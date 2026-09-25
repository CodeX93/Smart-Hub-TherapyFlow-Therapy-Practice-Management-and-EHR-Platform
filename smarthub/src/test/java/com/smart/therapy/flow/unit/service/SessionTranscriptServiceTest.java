package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.ai.service.AiService;
import com.smart.therapy.flow.ai.service.ConsentPolicyService;
import com.smart.therapy.flow.ai.service.OpenAiClient;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ErrorCode;
import com.smart.therapy.flow.common.exception.RateLimitExceededException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.TranscriptionServiceUnavailableException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.session.dto.SilentChunkDto;
import com.smart.therapy.flow.session.dto.TranscribeChunkResponse;
import com.smart.therapy.flow.session.dto.TranscribeFinalizeRequest;
import com.smart.therapy.flow.session.dto.SessionTranscriptResponse;
import com.smart.therapy.flow.session.dto.TranscribeFinalizeResponse;
import com.smart.therapy.flow.session.dto.TranscribeStartRequest;
import com.smart.therapy.flow.session.dto.TranscribeStartResponse;
import com.smart.therapy.flow.session.dto.TranscriptSmartFillResponse;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionTranscript;
import com.smart.therapy.flow.session.entity.SessionTranscriptChunk;
import com.smart.therapy.flow.session.enums.SessionTranscriptChunkStatus;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatus;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptChunkRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptRepository;
import com.smart.therapy.flow.session.service.SessionTranscriptService;
import com.smart.therapy.flow.session.service.TranscriptChunkRateLimiter;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SessionTranscriptService Unit Tests")
class SessionTranscriptServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private SessionTranscriptRepository sessionTranscriptRepository;
    @Mock private SessionTranscriptChunkRepository sessionTranscriptChunkRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private PermissionChecker permissionChecker;
    @Mock private SupervisorAssignmentRepository supervisorAssignmentRepository;
    @Mock private ConsentPolicyService consentPolicyService;
    @Mock private AiService aiService;
    @Mock private OpenAiClient openAiClient;
    @Mock private TranscriptChunkRateLimiter transcriptChunkRateLimiter;
    @Mock private AuditLogService auditLogService;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;

    @InjectMocks
    private SessionTranscriptService sessionTranscriptService;

    private User therapist;
    private AuthPrincipal therapistPrincipal;
    private Client client;
    private Session session;
    private SessionTranscript transcript;

    @BeforeEach
    void setUp() {
        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(10L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);

        client = TestDataFactory.createTestClient(therapist);
        client.setId(20L);

        session = TestDataFactory.createTestSession(client, therapist);
        session.setId(21L);

        transcript = TestDataFactory.createTestSessionTranscript(session, client, therapist);
        transcript.setId(100L);

        ReflectionTestUtils.setField(sessionTranscriptService, "defaultRetentionDays", 30);
        ReflectionTestUtils.setField(sessionTranscriptService, "maxExpectedChunks", 5000);
        ReflectionTestUtils.setField(sessionTranscriptService, "maxChunkChars", 40000);
        ReflectionTestUtils.setField(sessionTranscriptService, "languageAllowlist",
                "auto,en,en-us,en-gb,es,fr,de,it,pt,nl,ru,hi,zh,ja,ko,tr,pl,ar,multi,ur");

        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        doNothing().when(consentPolicyService).requireAiConsent(anyLong());
        doNothing().when(transcriptChunkRateLimiter).checkAndIncrement(anyLong(), anyLong());
        when(permissionChecker.hasRole(any(), eq("ADMIN"))).thenReturn(false);
        when(permissionChecker.hasRole(any(), eq("SUPER_ADMIN"))).thenReturn(false);
        when(permissionChecker.hasPermission(any(), anyString())).thenReturn(false);
        doNothing().when(auditLogService).write(any());
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(therapist));
        when(clientRepository.findById(anyLong())).thenReturn(Optional.of(client));
    }

    @Nested
    @DisplayName("startUpload")
    class StartUploadTests {

        @Test
        @DisplayName("Should create transcript upload and return uploadId")
        void shouldStartUploadSuccessfully() {
            TranscribeStartRequest request = new TranscribeStartRequest();
            request.setLanguage("en");

            when(sessionRepository.findByIdWithRelations(21L)).thenReturn(Optional.of(session));
            when(sessionTranscriptRepository.save(any(SessionTranscript.class))).thenAnswer(invocation -> {
                SessionTranscript saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            TranscribeStartResponse response = sessionTranscriptService.startUpload(
                    21L, request, therapistPrincipal, "127.0.0.1", "test-agent");

            assertThat(response.getUploadId()).startsWith("srv-");

            ArgumentCaptor<SessionTranscript> captor = ArgumentCaptor.forClass(SessionTranscript.class);
            verify(sessionTranscriptRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SessionTranscriptStatus.RECORDING);
            assertThat(captor.getValue().getLanguage()).isEqualTo("en");
            assertThat(captor.getValue().getUploader().getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should throw when session not found")
        void shouldThrowWhenSessionNotFound() {
            when(sessionRepository.findByIdWithRelations(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionTranscriptService.startUpload(
                    999L, new TranscribeStartRequest(), therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Session not found");
        }

        @Test
        @DisplayName("Should throw when language is unsupported")
        void shouldThrowWhenLanguageUnsupported() {
            when(sessionRepository.findByIdWithRelations(21L)).thenReturn(Optional.of(session));
            TranscribeStartRequest request = new TranscribeStartRequest();
            request.setLanguage("english");

            assertThatThrownBy(() -> sessionTranscriptService.startUpload(
                    21L, request, therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Unsupported language code");
        }

        @Test
        @DisplayName("Should throw when AI consent is missing")
        void shouldThrowWhenAiConsentBlocked() {
            when(sessionRepository.findByIdWithRelations(21L)).thenReturn(Optional.of(session));
            doThrow(new ForbiddenException("AI consent required"))
                    .when(consentPolicyService).requireAiConsent(client.getId());

            assertThatThrownBy(() -> sessionTranscriptService.startUpload(
                    21L, new TranscribeStartRequest(), therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("AI consent required");
        }
    }

    @Nested
    @DisplayName("processChunk")
    class ProcessChunkTests {

        @BeforeEach
        void stubTranscriptLookup() {
            when(sessionTranscriptRepository.findByUploadIdForUpdate(transcript.getUploadId()))
                    .thenReturn(Optional.of(transcript));
            when(sessionTranscriptChunkRepository.findByTranscriptIdAndChunkIndex(100L, -1))
                    .thenReturn(Optional.empty());
            when(sessionTranscriptChunkRepository.findByTranscriptIdAndChunkIndex(eq(100L), anyInt()))
                    .thenReturn(Optional.empty());
            when(sessionTranscriptChunkRepository.countByTranscriptId(100L)).thenReturn(1L);
            when(sessionTranscriptChunkRepository.save(any(SessionTranscriptChunk.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(sessionTranscriptRepository.save(any(SessionTranscript.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        @DisplayName("Should transcribe and persist chunk text")
        void shouldProcessChunkSuccessfully() {
            when(openAiClient.transcribeSessionChunk(any(), anyString(), eq("en"), isNull(), eq(false)))
                    .thenReturn("Client discussed anxiety coping strategies.");

            TranscribeChunkResponse response = sessionTranscriptService.processChunk(
                    21L,
                    transcript.getUploadId(),
                    0,
                    20.0,
                    new byte[] {1, 2, 3},
                    "chunk.webm",
                    "en",
                    therapistPrincipal,
                    "127.0.0.1",
                    "ua");

            assertThat(response.getUploadId()).isEqualTo(transcript.getUploadId());
            assertThat(response.getChunkIndex()).isZero();
            assertThat(response.getChunkText()).contains("anxiety coping strategies");
            assertThat(response.getChunksReceived()).isEqualTo(1);

            ArgumentCaptor<SessionTranscriptChunk> chunkCaptor = ArgumentCaptor.forClass(SessionTranscriptChunk.class);
            verify(sessionTranscriptChunkRepository).save(chunkCaptor.capture());
            assertThat(chunkCaptor.getValue().getChunkStatus()).isEqualTo(SessionTranscriptChunkStatus.RECEIVED);
        }

        @Test
        @DisplayName("Should mark chunk as silent when transcription is empty")
        void shouldMarkSilentChunkWhenTranscriptionEmpty() {
            when(openAiClient.transcribeSessionChunk(any(), anyString(), anyString(), isNull(), eq(false)))
                    .thenReturn("");

            TranscribeChunkResponse response = sessionTranscriptService.processChunk(
                    21L,
                    transcript.getUploadId(),
                    0,
                    20.0,
                    new byte[] {1, 2, 3},
                    "chunk.webm",
                    null,
                    therapistPrincipal,
                    "127.0.0.1",
                    "ua");

            assertThat(response.getChunkText()).isEmpty();

            ArgumentCaptor<SessionTranscriptChunk> chunkCaptor = ArgumentCaptor.forClass(SessionTranscriptChunk.class);
            verify(sessionTranscriptChunkRepository).save(chunkCaptor.capture());
            assertThat(chunkCaptor.getValue().getChunkStatus()).isEqualTo(SessionTranscriptChunkStatus.SILENT);
        }

        @Test
        @DisplayName("Should reject invalid uploadId prefix")
        void shouldRejectInvalidUploadId() {
            assertThatThrownBy(() -> sessionTranscriptService.processChunk(
                    21L, "bad-upload", 0, 20.0, new byte[] {1}, "chunk.webm", null,
                    therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid uploadId");
        }

        @Test
        @DisplayName("Should reject unsupported language override")
        void shouldRejectUnsupportedLanguageOverride() {
            assertThatThrownBy(() -> sessionTranscriptService.processChunk(
                    21L, transcript.getUploadId(), 0, 20.0, new byte[] {1}, "chunk.webm", "english",
                    therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Unsupported language code");
        }

        @Test
        @DisplayName("Should throw when upload not found")
        void shouldThrowWhenUploadNotFound() {
            when(sessionTranscriptRepository.findByUploadIdForUpdate("srv-missing"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionTranscriptService.processChunk(
                    21L, "srv-missing", 0, 20.0, new byte[] {1}, "chunk.webm", null,
                    therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Transcript upload not found");
        }

        @Test
        @DisplayName("Should throw when upload belongs to different session")
        void shouldThrowWhenUploadBelongsToDifferentSession() {
            assertThatThrownBy(() -> sessionTranscriptService.processChunk(
                    99L, transcript.getUploadId(), 0, 20.0, new byte[] {1}, "chunk.webm", null,
                    therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("does not belong to provided sessionId");
        }

        @Test
        @DisplayName("Should throw when upload is no longer recording")
        void shouldThrowWhenUploadNotRecording() {
            transcript.setStatus(SessionTranscriptStatus.READY);

            assertThatThrownBy(() -> sessionTranscriptService.processChunk(
                    21L, transcript.getUploadId(), 0, 20.0, new byte[] {1}, "chunk.webm", null,
                    therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("no longer accepting chunks");
        }

        @Test
        @DisplayName("Should throw when rate limit exceeded")
        void shouldThrowWhenRateLimitExceeded() {
            doThrow(new RateLimitExceededException("rate limited", 60))
                    .when(transcriptChunkRateLimiter).checkAndIncrement(10L, 21L);

            assertThatThrownBy(() -> sessionTranscriptService.processChunk(
                    21L, transcript.getUploadId(), 0, 20.0, new byte[] {1}, "chunk.webm", null,
                    therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        @DisplayName("Should propagate transcription service errors from OpenAI")
        void shouldPropagateTranscriptionServiceErrors() {
            when(openAiClient.transcribeSessionChunk(any(), anyString(), anyString(), isNull(), anyBoolean()))
                    .thenThrow(new TranscriptionServiceUnavailableException(
                            "Transcription is temporarily unavailable. Please wait a moment and try again.",
                            ErrorCode.EXTERNAL_OPENAI_ERROR,
                            true,
                            "circuit_open"));

            assertThatThrownBy(() -> sessionTranscriptService.processChunk(
                    21L, transcript.getUploadId(), 0, 20.0, new byte[] {1}, "chunk.webm", null,
                    therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(TranscriptionServiceUnavailableException.class)
                    .hasMessageContaining("temporarily unavailable");
        }

        @Test
        @DisplayName("Should forbid therapist without session access from uploading chunks")
        void shouldForbidTherapistWithoutSessionAccess() {
            User otherTherapist = TestDataFactory.createTestTherapist();
            otherTherapist.setId(99L);
            otherTherapist.getAuthIdentity().setId(999L);
            when(currentUserService.requireCurrentUser(any())).thenReturn(otherTherapist);

            assertThatThrownBy(() -> sessionTranscriptService.processChunk(
                    21L, transcript.getUploadId(), 0, 20.0, new byte[] {1}, "chunk.webm", null,
                    TestDataFactory.createAuthPrincipal(otherTherapist), "127.0.0.1", "ua"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("do not have access to this session");

            verify(openAiClient, never()).transcribeSessionChunk(any(), anyString(), anyString(), isNull(), anyBoolean());
        }

        @Test
        @DisplayName("Should forbid non-owner therapist even when session is accessible")
        void shouldForbidNonOwnerTherapist() {
            User otherTherapist = TestDataFactory.createTestTherapist();
            otherTherapist.setId(99L);
            otherTherapist.getAuthIdentity().setId(999L);
            session.setTherapist(otherTherapist);
            when(currentUserService.requireCurrentUser(any())).thenReturn(otherTherapist);

            assertThatThrownBy(() -> sessionTranscriptService.processChunk(
                    21L, transcript.getUploadId(), 0, 20.0, new byte[] {1}, "chunk.webm", null,
                    TestDataFactory.createAuthPrincipal(otherTherapist), "127.0.0.1", "ua"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("created by you");

            verify(openAiClient, never()).transcribeSessionChunk(any(), anyString(), anyString(), isNull(), anyBoolean());
        }
    }

    @Nested
    @DisplayName("finalizeUpload")
    class FinalizeUploadTests {

        @BeforeEach
        void stubFinalizeLookups() {
            when(sessionTranscriptRepository.findByUploadIdForUpdate(transcript.getUploadId()))
                    .thenReturn(Optional.of(transcript));
            when(sessionTranscriptRepository.save(any(SessionTranscript.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(sessionTranscriptChunkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(aiService.diarizeTranscript(anyString())).thenAnswer(invocation -> {
                String input = invocation.getArgument(0);
                return input == null ? null : input.replace("Therapist: Client reported", "Client: Client reported");
            });
        }

        @Test
        @DisplayName("Should stitch chunks, mark ready, and auto-diarize speakers")
        void shouldFinalizeUploadSuccessfully() {
            SessionTranscriptChunk chunk = TestDataFactory.createTestSessionTranscriptChunk(
                    transcript, 0, "Client reported improved sleep.");
            when(sessionTranscriptChunkRepository.findByTranscriptIdOrderByChunkIndexAsc(100L))
                    .thenReturn(List.of(chunk));

            TranscribeFinalizeRequest request = new TranscribeFinalizeRequest();
            request.setUploadId(transcript.getUploadId());
            request.setExpectedChunks(1);

            TranscribeFinalizeResponse response = sessionTranscriptService.finalizeUpload(
                    21L, request, therapistPrincipal, "127.0.0.1", "ua");

            assertThat(response.getStatus()).isEqualTo("ready");
            assertThat(response.getContent()).contains("Client reported improved sleep.");
            assertThat(response.getContent()).contains("Therapist:");
            assertThat(response.getDiarizedTranscript()).isNotBlank();
            assertThat(response.getWordCount()).isGreaterThan(0);
            verify(aiService).diarizeTranscript(anyString());

            ArgumentCaptor<SessionTranscript> captor = ArgumentCaptor.forClass(SessionTranscript.class);
            verify(sessionTranscriptRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            SessionTranscript saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
            assertThat(saved.getStatus()).isEqualTo(SessionTranscriptStatus.READY);
            assertThat(saved.getFinalTranscript()).isNotBlank();
            assertThat(saved.getDiarizedTranscript()).isNotBlank();
        }

        @Test
        @DisplayName("Should include silent chunk markers in stitched transcript")
        void shouldIncludeSilentChunkMarkers() {
            when(sessionTranscriptChunkRepository.findByTranscriptIdOrderByChunkIndexAsc(100L))
                    .thenReturn(List.of());

            TranscribeFinalizeRequest request = new TranscribeFinalizeRequest();
            request.setUploadId(transcript.getUploadId());
            request.setExpectedChunks(1);
            SilentChunkDto silent = new SilentChunkDto();
            silent.setIndex(0);
            silent.setDurationSeconds(15.0);
            request.setSilentChunks(List.of(silent));

            TranscribeFinalizeResponse response = sessionTranscriptService.finalizeUpload(
                    21L, request, therapistPrincipal, "127.0.0.1", "ua");

            assertThat(response.getContent()).contains("[silence");
            assertThat(response.getDurationSeconds()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should return existing transcript when already ready")
        void shouldReturnExistingWhenAlreadyReady() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript("Already finalized.");
            transcript.setDurationSeconds(20);
            transcript.setWordCount(2);

            TranscribeFinalizeRequest request = new TranscribeFinalizeRequest();
            request.setUploadId(transcript.getUploadId());
            request.setExpectedChunks(1);

            TranscribeFinalizeResponse response = sessionTranscriptService.finalizeUpload(
                    21L, request, therapistPrincipal, "127.0.0.1", "ua");

            assertThat(response.getContent()).isEqualTo("Already finalized.");
            verify(sessionTranscriptChunkRepository, never()).findByTranscriptIdOrderByChunkIndexAsc(anyLong());
        }

        @Test
        @DisplayName("Should throw conflict when chunks are missing")
        void shouldThrowConflictWhenChunksMissing() {
            when(sessionTranscriptChunkRepository.findByTranscriptIdOrderByChunkIndexAsc(100L))
                    .thenReturn(List.of());

            TranscribeFinalizeRequest request = new TranscribeFinalizeRequest();
            request.setUploadId(transcript.getUploadId());
            request.setExpectedChunks(2);

            assertThatThrownBy(() -> sessionTranscriptService.finalizeUpload(
                    21L, request, therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("only 0 of 2 chunks");
        }

        @Test
        @DisplayName("Should throw when upload expired")
        void shouldThrowWhenUploadExpired() {
            transcript.setExpiresAt(Instant.now().minusSeconds(60));

            TranscribeFinalizeRequest request = new TranscribeFinalizeRequest();
            request.setUploadId(transcript.getUploadId());
            request.setExpectedChunks(1);

            assertThatThrownBy(() -> sessionTranscriptService.finalizeUpload(
                    21L, request, therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");
        }
    }

    @Nested
    @DisplayName("getSessionTranscript")
    class GetSessionTranscriptTests {

        @Test
        @DisplayName("Should return latest transcript with chunks while recording")
        void shouldReturnTranscriptWithChunksWhileRecording() {
            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));
            SessionTranscriptChunk chunk = TestDataFactory.createTestSessionTranscriptChunk(
                    transcript, 0, "partial text");
            when(sessionTranscriptChunkRepository.findByTranscriptIdOrderByChunkIndexAsc(100L))
                    .thenReturn(List.of(chunk));

            var response = sessionTranscriptService.getSessionTranscript(21L, therapistPrincipal);

            assertThat(response.getUploadId()).isEqualTo(transcript.getUploadId());
            assertThat(response.getStatus()).isEqualTo("recording");
            assertThat(response.getChunks()).hasSize(1);

            ArgumentCaptor<com.smart.therapy.flow.auth.entity.AuditLog> auditCaptor =
                    ArgumentCaptor.forClass(com.smart.therapy.flow.auth.entity.AuditLog.class);
            verify(auditLogService).write(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction()).isEqualTo("session_transcript_viewed");
            assertThat(auditCaptor.getValue().getHipaaRelevant()).isTrue();
        }

        @Test
        @DisplayName("Should omit chunks when transcript is ready")
        void shouldOmitChunksWhenReady() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript("Final text.");
            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));

            var response = sessionTranscriptService.getSessionTranscript(21L, therapistPrincipal);

            assertThat(response.getFinalTranscript()).isEqualTo("Final text.");
            assertThat(response.getChunks()).isEmpty();
            verify(sessionTranscriptChunkRepository, never()).findByTranscriptIdOrderByChunkIndexAsc(anyLong());
        }

        @Test
        @DisplayName("Should include diarizedTranscript when speakers have been identified")
        void shouldIncludeDiarizedTranscriptWhenPresent() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript("[00:00:00]\nTherapist: How are you?\n\n[00:00:10]\nTherapist: Better today.");
            transcript.setDiarizedTranscript(
                    "[00:00:00]\nTherapist: How are you?\n\n[00:00:10]\nClient: Better today.");
            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));

            var response = sessionTranscriptService.getSessionTranscript(21L, therapistPrincipal);

            assertThat(response.getFinalTranscript()).contains("Therapist: Better today.");
            assertThat(response.getContent()).isEqualTo(response.getFinalTranscript());
            assertThat(response.getDiarizedTranscript()).contains("Client: Better today.");
            assertThat(response.getDiarizedTranscript()).isNotEqualTo(response.getFinalTranscript());
        }

        @Test
        @DisplayName("Should audit transcript text downloads")
        void shouldAuditTranscriptDownload() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript("Final text.");
            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));

            String content = sessionTranscriptService.downloadSessionTranscriptText(21L, therapistPrincipal);

            assertThat(content).isEqualTo("Final text.");
            ArgumentCaptor<com.smart.therapy.flow.auth.entity.AuditLog> auditCaptor =
                    ArgumentCaptor.forClass(com.smart.therapy.flow.auth.entity.AuditLog.class);
            verify(auditLogService).write(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction()).isEqualTo("session_transcript_downloaded");
        }
    }

    @Nested
    @DisplayName("smartFillFromTranscript")
    class SmartFillFromTranscriptTests {

        @Test
        @DisplayName("Should map finalized transcript into note fields")
        void shouldSmartFillFromFinalizedTranscript() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript("[00:00]\nTherapist: Client discussed anxiety.");

            when(sessionRepository.findByIdWithRelations(21L)).thenReturn(Optional.of(session));
            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));
            when(aiService.organizeTranscriptionIntoFieldsStrict(anyString(), anyString()))
                    .thenReturn(Map.of("sessionFocus", "Anxiety", "symptoms", "Racing thoughts"));

            TranscriptSmartFillResponse response = sessionTranscriptService.smartFillFromTranscript(
                    21L, therapistPrincipal);

            assertThat(response.getSessionId()).isEqualTo(21L);
            assertThat(response.getUploadId()).isEqualTo(transcript.getUploadId());
            assertThat(response.getTranscript()).contains("anxiety");
            assertThat(response.getMappedFields())
                    .containsEntry("sessionFocus", "Anxiety")
                    .containsEntry("symptoms", "Racing thoughts");
            verify(aiService).organizeTranscriptionIntoFieldsStrict(
                    eq("[00:00]\nTherapist: Client discussed anxiety."),
                    eq(client.getFullName()));
        }

        @Test
        @DisplayName("Should return empty mapped fields with message when nothing clinical is extractable")
        void shouldReturnEmptyFieldsWithMessageWhenNothingExtractable() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript("[00:00]\nTherapist: This is only a recording test.");

            when(sessionRepository.findByIdWithRelations(21L)).thenReturn(Optional.of(session));
            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));
            when(aiService.organizeTranscriptionIntoFieldsStrict(anyString(), anyString()))
                    .thenReturn(Map.of());

            TranscriptSmartFillResponse response = sessionTranscriptService.smartFillFromTranscript(
                    21L, therapistPrincipal);

            assertThat(response.getMappedFields()).isEmpty();
            assertThat(response.getMessage())
                    .contains("No structured session note fields could be extracted");
        }

        @Test
        @DisplayName("Should reject smart-fill when transcript is not finalized")
        void shouldRejectWhenTranscriptNotReady() {
            transcript.setStatus(SessionTranscriptStatus.RECORDING);
            transcript.setFinalTranscript("partial");

            when(sessionRepository.findByIdWithRelations(21L)).thenReturn(Optional.of(session));
            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));

            assertThatThrownBy(() -> sessionTranscriptService.smartFillFromTranscript(21L, therapistPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not finalized");
            verify(aiService, never()).organizeTranscriptionIntoFieldsStrict(anyString(), anyString());
        }

        @Test
        @DisplayName("Should reject smart-fill when finalized transcript text is empty")
        void shouldRejectWhenFinalTranscriptMissing() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript("   ");

            when(sessionRepository.findByIdWithRelations(21L)).thenReturn(Optional.of(session));
            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));

            assertThatThrownBy(() -> sessionTranscriptService.smartFillFromTranscript(21L, therapistPrincipal))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No finalized transcript content");
        }
    }

    @Nested
    @DisplayName("diarizeTranscript")
    class DiarizeTranscriptTests {

        private static final String RAW_FINAL =
                "[00:00:00]\nTherapist: How have you been this week?\n\n"
                        + "[00:00:12]\nTherapist: Better, but still anxious at work.";

        private static final String DIARIZED =
                "[00:00:00]\nTherapist: How have you been this week?\n\n"
                        + "[00:00:12]\nClient: Better, but still anxious at work.";

        @Test
        @DisplayName("Should call AI, save diarized transcript, and leave finalTranscript unchanged")
        void shouldDiarizeAndPreserveOriginal() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript(RAW_FINAL);
            transcript.setDiarizedTranscript(null);

            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));
            when(aiService.diarizeTranscript(RAW_FINAL)).thenReturn(DIARIZED);
            when(sessionTranscriptRepository.save(any(SessionTranscript.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            SessionTranscriptResponse response = sessionTranscriptService.diarizeTranscript(
                    21L, therapistPrincipal, "127.0.0.1", "ua");

            assertThat(response.getDiarizedTranscript()).isEqualTo(DIARIZED);
            assertThat(response.getFinalTranscript()).isEqualTo(RAW_FINAL);
            assertThat(response.getContent()).isEqualTo(RAW_FINAL);
            assertThat(response.getDiarizedTranscript()).contains("Client:");

            ArgumentCaptor<SessionTranscript> captor = ArgumentCaptor.forClass(SessionTranscript.class);
            verify(sessionTranscriptRepository).save(captor.capture());
            assertThat(captor.getValue().getDiarizedTranscript()).isEqualTo(DIARIZED);
            assertThat(captor.getValue().getFinalTranscript()).isEqualTo(RAW_FINAL);

            // HIPAA: only spoken transcript text is sent — never client name
            verify(aiService).diarizeTranscript(eq(RAW_FINAL));
            assertThat(RAW_FINAL).doesNotContain(client.getFullName());

            ArgumentCaptor<com.smart.therapy.flow.auth.entity.AuditLog> auditCaptor =
                    ArgumentCaptor.forClass(com.smart.therapy.flow.auth.entity.AuditLog.class);
            verify(auditLogService).write(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction()).isEqualTo("session_transcript_diarized");
            assertThat(auditCaptor.getValue().getDetails()).contains("clientNameSentToAI=false");
        }

        @Test
        @DisplayName("Should return cached diarized transcript without calling AI again")
        void shouldReturnCachedWithoutCallingAi() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript(RAW_FINAL);
            transcript.setDiarizedTranscript(DIARIZED);

            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));

            SessionTranscriptResponse response = sessionTranscriptService.diarizeTranscript(
                    21L, therapistPrincipal, "127.0.0.1", "ua");

            assertThat(response.getDiarizedTranscript()).isEqualTo(DIARIZED);
            verify(aiService, never()).diarizeTranscript(anyString());
            verify(sessionTranscriptRepository, never()).save(any());

            ArgumentCaptor<com.smart.therapy.flow.auth.entity.AuditLog> auditCaptor =
                    ArgumentCaptor.forClass(com.smart.therapy.flow.auth.entity.AuditLog.class);
            verify(auditLogService).write(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getAction())
                    .isEqualTo("session_transcript_diarization_retrieved");
        }

        @Test
        @DisplayName("Should clear cached AI refusal and re-run diarization")
        void shouldClearCachedRefusalAndRediarize() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript(RAW_FINAL);
            transcript.setDiarizedTranscript("I'm sorry, I can't assist with that request.");

            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));
            when(aiService.diarizeTranscript(RAW_FINAL)).thenReturn(DIARIZED);
            when(sessionTranscriptRepository.save(any(SessionTranscript.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            SessionTranscriptResponse response = sessionTranscriptService.diarizeTranscript(
                    21L, therapistPrincipal, "127.0.0.1", "ua");

            assertThat(response.getDiarizedTranscript()).isEqualTo(DIARIZED);
            verify(aiService).diarizeTranscript(eq(RAW_FINAL));
            verify(sessionTranscriptRepository, atLeastOnce()).save(any(SessionTranscript.class));
        }

        @Test
        @DisplayName("Should reject diarization when transcript is not finalized")
        void shouldRejectWhenNotReady() {
            transcript.setStatus(SessionTranscriptStatus.RECORDING);
            transcript.setFinalTranscript(RAW_FINAL);

            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));

            assertThatThrownBy(() -> sessionTranscriptService.diarizeTranscript(
                    21L, therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("must be finalized");
            verify(aiService, never()).diarizeTranscript(anyString());
        }

        @Test
        @DisplayName("Should reject diarization when final transcript is empty")
        void shouldRejectWhenFinalTranscriptEmpty() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript("   ");

            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));

            assertThatThrownBy(() -> sessionTranscriptService.diarizeTranscript(
                    21L, therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No finalized transcript content");
            verify(aiService, never()).diarizeTranscript(anyString());
        }

        @Test
        @DisplayName("Should reject when AI returns empty diarization")
        void shouldRejectWhenAiReturnsEmpty() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript(RAW_FINAL);

            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));
            when(aiService.diarizeTranscript(RAW_FINAL)).thenReturn("  ");

            assertThatThrownBy(() -> sessionTranscriptService.diarizeTranscript(
                    21L, therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("empty result");
            verify(sessionTranscriptRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should require AI consent before diarization")
        void shouldRequireAiConsent() {
            transcript.setStatus(SessionTranscriptStatus.READY);
            transcript.setFinalTranscript(RAW_FINAL);

            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));
            doThrow(new ForbiddenException("AI consent required"))
                    .when(consentPolicyService).requireAiConsent(client.getId());

            assertThatThrownBy(() -> sessionTranscriptService.diarizeTranscript(
                    21L, therapistPrincipal, "127.0.0.1", "ua"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("AI consent required");
            verify(aiService, never()).diarizeTranscript(anyString());
        }
    }

    @Nested
    @DisplayName("deleteSessionTranscript")
    class DeleteSessionTranscriptTests {

        @Test
        @DisplayName("Should soft-delete transcript and chunks")
        void shouldDeleteTranscript() {
            when(sessionTranscriptRepository.findBySessionIdOrderByCreatedAtDesc(21L))
                    .thenReturn(List.of(transcript));
            SessionTranscriptChunk chunk = TestDataFactory.createTestSessionTranscriptChunk(transcript, 0, "text");
            when(sessionTranscriptChunkRepository.findByTranscriptIdOrderByChunkIndexAsc(100L))
                    .thenReturn(List.of(chunk));
            when(sessionTranscriptRepository.save(any(SessionTranscript.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(sessionTranscriptChunkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            sessionTranscriptService.deleteSessionTranscript(21L, therapistPrincipal, "127.0.0.1", "ua");

            ArgumentCaptor<SessionTranscript> captor = ArgumentCaptor.forClass(SessionTranscript.class);
            verify(sessionTranscriptRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SessionTranscriptStatus.DELETED);
            assertThat(captor.getValue().getIsDeleted()).isTrue();
            verify(sessionTranscriptChunkRepository).saveAll(any());
        }
    }
}

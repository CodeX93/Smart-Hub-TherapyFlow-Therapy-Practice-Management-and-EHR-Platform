package com.smart.therapy.flow.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.common.config.AppProperties;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.exception.GlobalExceptionHandler;
import com.smart.therapy.flow.common.logging.SensitiveDataMasker;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.session.controller.SessionController;
import com.smart.therapy.flow.session.dto.TranscribeChunkResponse;
import com.smart.therapy.flow.session.dto.TranscribeFinalizeRequest;
import com.smart.therapy.flow.session.dto.TranscribeFinalizeResponse;
import com.smart.therapy.flow.session.dto.TranscribeStartResponse;
import com.smart.therapy.flow.session.dto.SessionTranscriptResponse;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.session.service.SessionTranscriptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Session transcript controller contract tests")
class SessionTranscriptControllerContractTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private com.smart.therapy.flow.session.service.RecurringSessionService recurringSessionService;

    @Mock
    private SessionTranscriptService sessionTranscriptService;

    @Mock
    private BillingService billingService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AppProperties appProperties;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AuthPrincipal authPrincipal;

    @BeforeEach
    void setUp() {
        SessionController controller = new SessionController(
                sessionService, recurringSessionService, sessionTranscriptService, billingService, jwtTokenProvider);
        ReflectionTestUtils.setField(controller, "appProperties", appProperties);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        authPrincipal = TestDataFactory.createAuthPrincipal(
                TestDataFactory.createTestTherapist(),
                "ROLE_THERAPIST",
                "SESSION_EDIT",
                "SESSION_VIEW");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(
                        org.mockito.Mockito.mock(AuditLogService.class),
                        new SensitiveDataMasker(new ObjectMapper()),
                        org.mockito.Mockito.mock(com.smart.therapy.flow.common.metrics.AuthAbuseMetrics.class)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setValidator(validator)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    private RequestPostProcessor authenticated() {
        return request -> {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(authPrincipal, null, authPrincipal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    @Test
    @DisplayName("POST transcribe-start returns uploadId")
    void shouldStartTranscriptUpload() throws Exception {
        when(sessionTranscriptService.startUpload(anyLong(), any(), any(AuthPrincipal.class), anyString(), nullable(String.class)))
                .thenReturn(TranscribeStartResponse.builder().uploadId("srv-contracttest000000000001").build());
        when(jwtTokenProvider.generateTranscriptionWebSocketTicket(
                any(AuthPrincipal.class), eq("srv-contracttest000000000001"), eq(60L)))
                .thenReturn("short-lived-websocket-ticket");

        mockMvc.perform(post("/api/v1/sessions/21/transcribe-start")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadId").value("srv-contracttest000000000001"))
                .andExpect(jsonPath("$.websocketTicket").value("short-lived-websocket-ticket"))
                .andExpect(jsonPath("$.websocketTicketExpiresInSeconds").value(60));
    }

    @Test
    @DisplayName("POST transcribe-start accepts legacy expectedChunks field")
    void shouldStartTranscriptUploadWithExpectedChunks() throws Exception {
        when(sessionTranscriptService.startUpload(anyLong(), any(), any(AuthPrincipal.class), anyString(), nullable(String.class)))
                .thenReturn(TranscribeStartResponse.builder().uploadId("srv-contracttest000000000001").build());
        when(jwtTokenProvider.generateTranscriptionWebSocketTicket(
                any(AuthPrincipal.class), eq("srv-contracttest000000000001"), eq(60L)))
                .thenReturn("short-lived-websocket-ticket");

        mockMvc.perform(post("/api/v1/sessions/21/transcribe-start")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"en-us\",\"expectedChunks\":5000,\"retentionDays\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadId").value("srv-contracttest000000000001"));
    }

    @Test
    @DisplayName("POST transcribe-chunk returns transcribed chunk payload")
    void shouldUploadTranscriptChunk() throws Exception {
        when(sessionTranscriptService.processChunk(
                eq(21L),
                eq("srv-contracttest000000000001"),
                eq(0),
                eq(20.0),
                any(),
                anyString(),
                eq("en"),
                any(AuthPrincipal.class),
                anyString(),
                nullable(String.class)))
                .thenReturn(TranscribeChunkResponse.builder()
                        .uploadId("srv-contracttest000000000001")
                        .chunkIndex(0)
                        .chunkText("Client discussed coping skills.")
                        .chunksReceived(1)
                        .build());

        MockMultipartFile audio = new MockMultipartFile("audio", "chunk.webm", "video/webm", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/v1/sessions/21/transcribe-chunk")
                        .file(audio)
                        .with(authenticated())
                        .param("uploadId", "srv-contracttest000000000001")
                        .param("chunkIndex", "0")
                        .param("chunkDurationSeconds", "20")
                        .param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunkText").value("Client discussed coping skills."))
                .andExpect(jsonPath("$.chunksReceived").value(1));
    }

    @Test
    @DisplayName("POST transcribe-chunk rejects non-audio content type")
    void shouldRejectNonAudioContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("audio", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/v1/sessions/21/transcribe-chunk")
                        .file(file)
                        .with(authenticated())
                        .param("uploadId", "srv-contracttest000000000001")
                        .param("chunkIndex", "0")
                        .param("chunkDurationSeconds", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only audio files are allowed"));
    }

    @Test
    @DisplayName("POST transcribe-chunk surfaces unsupported language as 400")
    void shouldSurfaceUnsupportedLanguageAsBadRequest() throws Exception {
        when(sessionTranscriptService.processChunk(
                anyLong(), anyString(), anyInt(), anyDouble(), any(), anyString(), eq("english"),
                any(AuthPrincipal.class), anyString(), nullable(String.class)))
                .thenThrow(new BadRequestException("Unsupported language code: english"));

        MockMultipartFile audio = new MockMultipartFile("audio", "chunk.webm", "video/webm", new byte[] {1, 2, 3});

        mockMvc.perform(multipart("/api/v1/sessions/21/transcribe-chunk")
                        .file(audio)
                        .with(authenticated())
                        .param("uploadId", "srv-contracttest000000000001")
                        .param("chunkIndex", "0")
                        .param("chunkDurationSeconds", "20")
                        .param("language", "english"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported language code: english"));
    }

    @Test
    @DisplayName("POST transcribe-finalize returns stitched transcript")
    void shouldFinalizeTranscriptUpload() throws Exception {
        when(sessionTranscriptService.finalizeUpload(anyLong(), any(TranscribeFinalizeRequest.class),
                any(AuthPrincipal.class), anyString(), nullable(String.class)))
                .thenReturn(TranscribeFinalizeResponse.builder()
                        .id(100L)
                        .sessionId(21L)
                        .clientId(20L)
                        .uploadId("srv-contracttest000000000001")
                        .status("ready")
                        .content("[00:00:00]\nTherapist: Final transcript text")
                        .wordCount(4)
                        .durationSeconds(20)
                        .build());

        TranscribeFinalizeRequest request = new TranscribeFinalizeRequest();
        request.setUploadId("srv-contracttest000000000001");
        request.setExpectedChunks(1);

        mockMvc.perform(post("/api/v1/sessions/21/transcribe-finalize")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.content").value("[00:00:00]\nTherapist: Final transcript text"));
    }

    @Test
    @DisplayName("POST transcribe-finalize returns 409 when chunks are missing")
    void shouldReturnConflictWhenChunksMissing() throws Exception {
        when(sessionTranscriptService.finalizeUpload(anyLong(), any(TranscribeFinalizeRequest.class),
                any(AuthPrincipal.class), anyString(), nullable(String.class)))
                .thenThrow(new ConflictException("Cannot finalize: only 0 of 2 chunks were accounted for"));

        TranscribeFinalizeRequest request = new TranscribeFinalizeRequest();
        request.setUploadId("srv-contracttest000000000001");
        request.setExpectedChunks(2);

        mockMvc.perform(post("/api/v1/sessions/21/transcribe-finalize")
                        .with(authenticated())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot finalize: only 0 of 2 chunks were accounted for"));
    }

    @Test
    @DisplayName("GET transcript returns latest transcript")
    void shouldGetSessionTranscript() throws Exception {
        when(sessionTranscriptService.getSessionTranscript(eq(21L), any(AuthPrincipal.class)))
                .thenReturn(SessionTranscriptResponse.builder()
                        .transcriptId(100L)
                        .sessionId(21L)
                        .uploadId("srv-contracttest000000000001")
                        .status("ready")
                        .finalTranscript("Final transcript text")
                        .build());

        mockMvc.perform(get("/api/v1/sessions/21/transcript").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.finalTranscript").value("Final transcript text"));
    }

    @Test
    @DisplayName("POST transcript/diarize returns speaker-labeled transcript")
    void shouldDiarizeTranscript() throws Exception {
        when(sessionTranscriptService.diarizeTranscript(
                eq(21L), any(AuthPrincipal.class), anyString(), nullable(String.class)))
                .thenReturn(SessionTranscriptResponse.builder()
                        .transcriptId(100L)
                        .sessionId(21L)
                        .uploadId("srv-contracttest000000000001")
                        .status("ready")
                        .finalTranscript("[00:00:00]\nTherapist: How are you?\n\n[00:00:10]\nTherapist: Better.")
                        .content("[00:00:00]\nTherapist: How are you?\n\n[00:00:10]\nTherapist: Better.")
                        .diarizedTranscript("[00:00:00]\nTherapist: How are you?\n\n[00:00:10]\nClient: Better.")
                        .build());

        mockMvc.perform(post("/api/v1/sessions/21/transcript/diarize").with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.diarizedTranscript").value(
                        "[00:00:00]\nTherapist: How are you?\n\n[00:00:10]\nClient: Better."))
                .andExpect(jsonPath("$.finalTranscript").value(
                        "[00:00:00]\nTherapist: How are you?\n\n[00:00:10]\nTherapist: Better."));
    }

    @Test
    @DisplayName("POST transcript/diarize returns 400 when transcript not finalized")
    void shouldReturnBadRequestWhenDiarizeNotReady() throws Exception {
        when(sessionTranscriptService.diarizeTranscript(
                eq(21L), any(AuthPrincipal.class), anyString(), nullable(String.class)))
                .thenThrow(new BadRequestException(
                        "Transcript must be finalized before diarization (status: recording)"));

        mockMvc.perform(post("/api/v1/sessions/21/transcript/diarize").with(authenticated()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("must be finalized")));
    }

    @Test
    @DisplayName("DELETE transcript returns 204")
    void shouldDeleteTranscript() throws Exception {
        doNothing().when(sessionTranscriptService).deleteSessionTranscript(
                eq(21L), any(AuthPrincipal.class), anyString(), nullable(String.class));

        mockMvc.perform(delete("/api/v1/sessions/21/transcript").with(authenticated()))
                .andExpect(status().isNoContent());
    }
}

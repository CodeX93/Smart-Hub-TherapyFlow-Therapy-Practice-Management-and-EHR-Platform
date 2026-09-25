package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.ai.service.AiService;
import com.smart.therapy.flow.ai.service.ConsentPolicyService;
import com.smart.therapy.flow.ai.service.OpenAiClient;
import com.smart.therapy.flow.session.service.TranscriptChunkRateLimiter;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptChunkRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptRepository;
import com.smart.therapy.flow.session.service.SessionTranscriptService;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionTranscriptService Audit Logging Unit Tests")
class SessionTranscriptServiceAuditTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionTranscriptRepository sessionTranscriptRepository;
    @Mock
    private SessionTranscriptChunkRepository sessionTranscriptChunkRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private SupervisorAssignmentRepository supervisorAssignmentRepository;
    @Mock
    private ConsentPolicyService consentPolicyService;
    @Mock
    private AiService aiService;
    @Mock
    private OpenAiClient openAiClient;
    @Mock
    private TranscriptChunkRateLimiter transcriptChunkRateLimiter;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private SessionTranscriptService sessionTranscriptService;

    @Test
    @DisplayName("Should persist audit log when details is null")
    void shouldPersistAuditLogWhenDetailsIsNull() {
        // Arrange
        Long userId = 12L;
        Long clientId = 34L;
        Long sessionId = 56L;
        User user = TestDataFactory.createTestAdmin();
        user.setId(userId);
        Client client = TestDataFactory.createTestClientWithId(clientId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));

        // Act
        ReflectionTestUtils.invokeMethod(
                sessionTranscriptService,
                "recordAuditEvent",
                userId,
                "admin@example.com",
                999L,
                clientId,
                "session_transcript_started",
                "127.0.0.1",
                "JUnit",
                null,
                sessionId
        );

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).write(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getAction()).isEqualTo("session_transcript_started");
        assertThat(saved.getDetails()).isNotNull();
        assertThat(saved.getDetails()).doesNotContain("null");
        assertThat(saved.getDetails()).contains("sessionId: 56");
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getClient()).isEqualTo(client);
    }
}

package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.ai.dto.AssistantChatRequest;
import com.smart.therapy.flow.ai.dto.ChatMessageDto;
import com.smart.therapy.flow.ai.dto.SessionNoteTemplateRequest;
import com.smart.therapy.flow.ai.service.AiService;
import com.smart.therapy.flow.ai.service.ConsentService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.session.repository.SessionNoteRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiService Unit Tests")
class AiServiceTest {

    @Mock
    private com.smart.therapy.flow.ai.service.OpenAiClient openAiClient;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SessionNoteRepository sessionNoteRepository;

    @Mock
    private ConsentService consentService;

    @InjectMocks
    private AiService aiService;

    private Client client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "sessionNoteModel", "gpt-4o");
        ReflectionTestUtils.setField(aiService, "assistantModel", "gpt-5");
        ReflectionTestUtils.setField(aiService, "transcriptionOrganizationModel", "gpt-4o");

        client = TestDataFactory.createTestClient();
        client.setId(1L);
    }

    @Test
    @DisplayName("Should generate session note template successfully")
    void shouldGenerateSessionNoteTemplateSuccessfully() {
        // Arrange
        SessionNoteTemplateRequest request = new SessionNoteTemplateRequest();
        SessionNoteTemplateRequest.ClientInfo clientInfo = new SessionNoteTemplateRequest.ClientInfo();
        clientInfo.setFullName("John Doe");
        request.setClient(clientInfo);

        SessionNoteTemplateRequest.SessionInfo sessionInfo = new SessionNoteTemplateRequest.SessionInfo();
        request.setSession(sessionInfo);

        when(openAiClient.createChatCompletion(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(
                        "SESSION FOCUS\n"
                                + "Mr. or Ms. [Last Name] discussed anxiety symptoms. "
                                + "Support for [Last Name] continues.");

        // Act
        String template = aiService.generateSessionNoteTemplate(request);

        // Assert
        assertThat(template).isNotNull();
        assertThat(template)
                .contains("CLIENT INFORMATION")
                .contains("Name: John Doe")
                .contains("SESSION INFORMATION")
                .contains("SESSION FOCUS")
                .contains("Mr./Ms. Doe discussed anxiety symptoms.")
                .contains("Support for Doe continues.")
                .doesNotContain("[Last Name]");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, String>>> messagesCaptor =
                ArgumentCaptor.forClass((Class) List.class);
        verify(openAiClient).createChatCompletion(
                anyString(), messagesCaptor.capture(), anyDouble(), anyInt());
        String outboundPrompt = messagesCaptor.getValue().toString();
        assertThat(outboundPrompt)
                .doesNotContain("John Doe")
                .doesNotContain("Client ID:")
                .doesNotContain("Date of Birth:")
                .contains("Subject: Client");
    }

    @Test
    @DisplayName("Should get assistant response successfully")
    void shouldGetAssistantResponseSuccessfully() {
        // Arrange
        AssistantChatRequest request = new AssistantChatRequest();
        request.setUserMessage("What is therapy?");
        request.setUserRole("THERAPIST");
        request.setConversationHistory(new ArrayList<>());

        when(openAiClient.createChatCompletion(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn("Therapy is a form of treatment...");

        // Act
        String response = aiService.getAssistantResponse(request);

        // Assert
        assertThat(response).isNotNull();
        verify(openAiClient).createChatCompletion(anyString(), anyList(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("Should include conversation history in assistant response")
    void shouldIncludeConversationHistoryInAssistantResponse() {
        // Arrange
        AssistantChatRequest request = new AssistantChatRequest();
        request.setUserMessage("Tell me more");
        request.setUserRole("THERAPIST");

        List<ChatMessageDto> history = new ArrayList<>();
        ChatMessageDto previousMessage = new ChatMessageDto();
        previousMessage.setRole("user");
        previousMessage.setContent("What is therapy?");
        history.add(previousMessage);
        request.setConversationHistory(history);

        when(openAiClient.createChatCompletion(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn("Therapy involves...");

        // Act
        String response = aiService.getAssistantResponse(request);

        // Assert
        assertThat(response).isNotNull();
        verify(openAiClient).createChatCompletion(anyString(), anyList(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("Should diarize transcript with role-only labels and no client name in prompt")
    void shouldDiarizeTranscriptWithoutClientName() {
        String raw = "[00:00:00]\nTherapist: How have you been?\n\n[00:00:15]\nTherapist: Anxious at work.";
        String diarized = "[00:00:00]\nTherapist: How have you been?\n\n[00:00:15]\nClient: Anxious at work.";

        when(openAiClient.createChatCompletion(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn(diarized);

        String result = aiService.diarizeTranscript(raw);

        assertThat(result).isEqualTo(diarized);
        assertThat(result).contains("Therapist:").contains("Client:");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Map<String, String>>> messagesCaptor =
                ArgumentCaptor.forClass((Class) List.class);
        verify(openAiClient).createChatCompletion(
                eq("gpt-4o"), messagesCaptor.capture(), eq(0.1), anyInt());

        String outbound = messagesCaptor.getValue().toString();
        assertThat(outbound)
                .doesNotContain("John Doe")
                .doesNotContain("Client1")
                .doesNotContain("MRN")
                .contains("Therapist:")
                .contains("Client:")
                .contains("authorized")
                .contains(raw);
    }

    @Test
    @DisplayName("Should retry and succeed when first diarization response is a model refusal")
    void shouldRetryWhenModelRefusesDiarization() {
        String raw = "[00:00:00]\nTherapist: How have you been?\n\n[00:00:15]\nTherapist: Anxious at work.";
        String diarized = "[00:00:00]\nTherapist: How have you been?\n\n[00:00:15]\nClient: Anxious at work.";

        when(openAiClient.createChatCompletion(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn("I'm sorry, I can't assist with that request.")
                .thenReturn(diarized);

        String result = aiService.diarizeTranscript(raw);

        assertThat(result).isEqualTo(diarized);
        verify(openAiClient, times(2)).createChatCompletion(anyString(), anyList(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("Should throw when diarization keeps returning a model refusal")
    void shouldThrowWhenDiarizationKeepsRefusing() {
        String raw = "[00:00:00]\nTherapist: How have you been?\n\n[00:00:15]\nTherapist: Anxious at work.";

        when(openAiClient.createChatCompletion(anyString(), anyList(), anyDouble(), anyInt()))
                .thenReturn("I'm sorry, I can't assist with that request.");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.smart.therapy.flow.common.exception.BadRequestException.class,
                () -> aiService.diarizeTranscript(raw));
        verify(openAiClient, times(2)).createChatCompletion(anyString(), anyList(), anyDouble(), anyInt());
    }

    @Test
    @DisplayName("Should detect OpenAI refusal text as invalid diarization")
    void shouldDetectRefusalAsInvalidDiarization() {
        assertThat(AiService.isInvalidDiarizationResult("I'm sorry, I can't assist with that request."))
                .isTrue();
        assertThat(AiService.isInvalidDiarizationResult(
                "[00:00:00]\nTherapist: Hi\n\n[00:00:05]\nClient: Hello")).isFalse();
    }

    @Test
    @DisplayName("Should return blank input unchanged when diarizing empty transcript")
    void shouldReturnBlankUnchangedWhenDiarizingEmpty() {
        assertThat(aiService.diarizeTranscript("  ")).isEqualTo("  ");
        assertThat(aiService.diarizeTranscript(null)).isNull();
        verify(openAiClient, never()).createChatCompletion(anyString(), anyList(), anyDouble(), anyInt());
    }
}


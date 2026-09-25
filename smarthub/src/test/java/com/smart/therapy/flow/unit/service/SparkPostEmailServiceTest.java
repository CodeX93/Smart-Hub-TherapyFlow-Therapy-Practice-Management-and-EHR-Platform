package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.entity.NotificationTemplate;
import com.smart.therapy.flow.notification.service.SparkPostEmailService;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SparkPostEmailService Unit Tests")
class SparkPostEmailServiceTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private SparkPostEmailService sparkPostEmailService;

    private User recipient;
    private Notification notification;
    private NotificationTemplate template;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.requestFactory(any(Supplier.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        sparkPostEmailService = new SparkPostEmailService(restTemplateBuilder, "synthetic-test-key",
                "sender@example.test", "QA", "http://127.0.0.1:1");

        recipient = TestDataFactory.createTestTherapist();
        recipient.setId(1L);
        recipient.setEmail("therapist@example.com");

        notification = Notification.builder()
                .title("Test Notification")
                .message("Test message")
                .build();

        template = NotificationTemplate.builder()
                .subject("Test Subject")
                .bodyTemplate("Test body template")
                .build();
    }

    @Test
    @DisplayName("Should send notification email successfully")
    void shouldSendNotificationEmailSuccessfully() {
        // Arrange
        Map<String, Object> entityData = new HashMap<>();
        entityData.put("clientName", "John Doe");

        @SuppressWarnings("unchecked")
        Class<Map> responseType = Map.class;
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType)))
                .thenReturn(ResponseEntity.ok(new HashMap<>()));

        // Act
        sparkPostEmailService.sendNotificationEmail(recipient, notification, template, entityData);

        // Assert
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType));
    }

    @Test
    @DisplayName("Should skip email when recipient email is null")
    void shouldSkipEmailWhenRecipientEmailIsNull() {
        // Arrange
        recipient.setEmail(null);
        Map<String, Object> entityData = new HashMap<>();

        // Act
        sparkPostEmailService.sendNotificationEmail(recipient, notification, template, entityData);

        // Assert
        @SuppressWarnings("unchecked")
        Class<Map> responseType = Map.class;
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(responseType));
    }

    @Test
    @DisplayName("Should skip email when recipient is null")
    void shouldSkipEmailWhenRecipientIsNull() {
        // Arrange
        Map<String, Object> entityData = new HashMap<>();

        // Act
        sparkPostEmailService.sendNotificationEmail(null, notification, template, entityData);

        // Assert
        @SuppressWarnings("unchecked")
        Class<Map> responseType = Map.class;
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(), eq(responseType));
    }

    @Test
    @DisplayName("Should report the transmission id from a successful response")
    void shouldAcceptTransmissionWithAcceptedRecipients() {
        Map<String, Object> results = new HashMap<>();
        results.put("id", "12345");
        results.put("total_accepted_recipients", 1);
        results.put("total_rejected_recipients", 0);
        Map<String, Object> body = new HashMap<>();
        body.put("results", results);

        @SuppressWarnings("unchecked")
        Class<Map> responseType = Map.class;
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType)))
                .thenReturn(ResponseEntity.ok(body));

        sparkPostEmailService.sendEmail("someone@example.com", "Subject", "<p>Body</p>");

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType));
    }

    @Test
    @DisplayName("Should fail loudly when SparkPost answers 200 but accepts no recipients")
    void shouldFailWhenNoRecipientAccepted() {
        Map<String, Object> results = new HashMap<>();
        results.put("id", "12345");
        results.put("total_accepted_recipients", 0);
        results.put("total_rejected_recipients", 0);
        Map<String, Object> body = new HashMap<>();
        body.put("results", results);

        @SuppressWarnings("unchecked")
        Class<Map> responseType = Map.class;
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType)))
                .thenReturn(ResponseEntity.ok(body));

        assertThatThrownBy(() -> sparkPostEmailService.sendEmail("someone@example.com", "Subject", "<p>Body</p>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email via SparkPost")
                .rootCause()
                .hasMessageContaining("accepted no recipients");
    }

    @Test
    @DisplayName("Should surface a suppressed recipient instead of logging success")
    void shouldFailWhenRecipientRejected() {
        Map<String, Object> results = new HashMap<>();
        results.put("id", "12345");
        results.put("total_accepted_recipients", 0);
        results.put("total_rejected_recipients", 1);
        Map<String, Object> body = new HashMap<>();
        body.put("results", results);

        @SuppressWarnings("unchecked")
        Class<Map> responseType = Map.class;
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType)))
                .thenReturn(ResponseEntity.ok(body));

        assertThatThrownBy(() -> sparkPostEmailService.sendNotificationEmail(recipient, notification, template, new HashMap<>()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email via SparkPost")
                .rootCause()
                .hasMessageContaining("suppressed");
    }

    @Test
    @DisplayName("Should tolerate a response without a results block")
    void shouldTolerateEmptyResponseBody() {
        @SuppressWarnings("unchecked")
        Class<Map> responseType = Map.class;
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType)))
                .thenReturn(ResponseEntity.ok(new HashMap<>()));

        sparkPostEmailService.sendEmail("someone@example.com", "Subject", "<p>Body</p>");

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType));
    }

    @Test
    @DisplayName("Should propagate provider failure to the retry interceptor")
    void shouldPropagateProviderFailure() {
        // Arrange
        Map<String, Object> entityData = new HashMap<>();
        @SuppressWarnings("unchecked")
        Class<Map> responseType = Map.class;
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType)))
                .thenThrow(new RuntimeException("Network error"));

        // A directly constructed service has no Spring retry proxy; the failure must escape.
        assertThatThrownBy(() -> sparkPostEmailService.sendNotificationEmail(recipient, notification, template, entityData))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email via SparkPost")
                .hasRootCauseMessage("Network error");

        // Assert
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(responseType));
    }
}

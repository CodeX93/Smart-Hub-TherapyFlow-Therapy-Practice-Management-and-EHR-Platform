package com.smart.therapy.flow.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.session.dto.ZoomMeetingResponse;
import com.smart.therapy.flow.session.service.ZoomApiService;
import com.smart.therapy.flow.user.entity.UserIntegration;
import com.smart.therapy.flow.user.repository.UserIntegrationRepository;
import java.time.Instant;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZoomApiService Unit Tests")
@SuppressWarnings({ "null", "unchecked" }) // Suppress null warnings from Mockito and unchecked types
class ZoomApiServiceTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserIntegrationRepository userIntegrationRepository;

    @Mock
    private UserRepository userRepository;

    private ZoomApiService zoomApiService;

    private User therapist;
    private UserIntegration zoomIntegration;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.requestFactory(any(Supplier.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        zoomApiService = new ZoomApiService(restTemplateBuilder, userRepository, userIntegrationRepository,
                5000, 15000, "http://127.0.0.1:1/v2", "http://127.0.0.1:1");

        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);

        ObjectNode settings = new ObjectMapper().createObjectNode();
        settings.put("clientId", "client-id");
        settings.put("clientSecret", "client-secret");

        zoomIntegration = UserIntegration.builder()
                .user(therapist)
                .integrationType("zoom")
                .externalUserId("zoom-account-id")
                .settings(settings)
                .accessToken("access-token")
                .tokenExpiresAt(Instant.now().plusSeconds(3600))
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should return true when therapist is configured")
    void shouldReturnTrueWhenTherapistIsConfigured() {
        when(userIntegrationRepository.findByUserAndIntegrationType(therapist, "zoom"))
                .thenReturn(java.util.Optional.of(zoomIntegration));

        // Act
        boolean isConfigured = zoomApiService.isTherapistConfigured(therapist);

        // Assert
        assertThat(isConfigured).isTrue();
    }

    @Test
    @DisplayName("Should return false when therapist is not configured")
    void shouldReturnFalseWhenTherapistIsNotConfigured() {
        // Arrange
        when(userIntegrationRepository.findByUserAndIntegrationType(therapist, "zoom"))
                .thenReturn(java.util.Optional.empty());

        // Act
        boolean isConfigured = zoomApiService.isTherapistConfigured(therapist);

        // Assert
        assertThat(isConfigured).isFalse();
    }

    @Test
    @DisplayName("Should return false when therapist is null")
    void shouldReturnFalseWhenTherapistIsNull() {
        // Act
        boolean isConfigured = zoomApiService.isTherapistConfigured(null);

        // Assert
        assertThat(isConfigured).isFalse();
    }

    @Test
    @DisplayName("Should create meeting successfully")
    void shouldCreateMeetingSuccessfully() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("topic", "Therapy Session");
        request.put("startTime", "2024-12-15T10:00:00Z");
        request.put("duration", 60);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", 123456789L);
        responseBody.put("join_url", "https://zoom.us/j/123456789");
        responseBody.put("password", "test123");

        when(userIntegrationRepository.findByUserAndIntegrationType(therapist, "zoom"))
                .thenReturn(java.util.Optional.of(zoomIntegration));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        // Act
        ZoomMeetingResponse response = zoomApiService.createMeeting(request, therapist);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMeetingId()).isEqualTo("123456789");
        assertThat(response.getJoinUrl()).isEqualTo("https://zoom.us/j/123456789");
        assertThat(response.getPassword()).isEqualTo("test123");
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class));
    }
}

package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.integration.zoom.dto.ZoomCredentials;
import com.smart.therapy.flow.integration.zoom.dto.ZoomMeetingRequest;
import com.smart.therapy.flow.integration.zoom.dto.ZoomMeetingResponse;
import com.smart.therapy.flow.integration.zoom.service.ZoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZoomService Unit Tests")
class ZoomServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ZoomService zoomService;

    private ZoomCredentials credentials;
    private ZoomMeetingRequest meetingRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(zoomService, "zoomApiBaseUrl", "https://api.zoom.us/v2");
        ReflectionTestUtils.setField(zoomService, "zoomOauthBaseUrl", "https://zoom.us");

        credentials = ZoomCredentials.builder()
                .accountId("test-account-id")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .build();

        meetingRequest = ZoomMeetingRequest.builder()
                .therapistName("Dr. Smith")
                .sessionDate(Instant.now().plusSeconds(86400))
                .duration(60)
                .timezone("America/New_York")
                .build();
    }

    @Test
    @DisplayName("Should return true when therapist is configured")
    void shouldReturnTrueWhenTherapistIsConfigured() {
        // Act
        boolean isConfigured = zoomService.isTherapistConfigured(credentials);

        // Assert
        assertThat(isConfigured).isTrue();
    }

    @Test
    @DisplayName("Should return false when credentials are null")
    void shouldReturnFalseWhenCredentialsAreNull() {
        // Act
        boolean isConfigured = zoomService.isTherapistConfigured(null);

        // Assert
        assertThat(isConfigured).isFalse();
    }

    @Test
    @DisplayName("Should return false when account ID is missing")
    void shouldReturnFalseWhenAccountIdIsMissing() {
        // Arrange
        credentials.setAccountId(null);

        // Act
        boolean isConfigured = zoomService.isTherapistConfigured(credentials);

        // Assert
        assertThat(isConfigured).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when creating meeting without credentials")
    void shouldThrowExceptionWhenCreatingMeetingWithoutCredentials() {
        // Arrange
        ZoomCredentials invalidCredentials = ZoomCredentials.builder()
                .accountId(null)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> zoomService.createMeeting(meetingRequest, invalidCredentials))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Therapist must configure");
    }

    @Test
    @DisplayName("Should throw exception when meeting request is null")
    void shouldThrowExceptionWhenMeetingRequestIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> zoomService.createMeeting(null, credentials))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Meeting request is required");
    }

    @Test
    @DisplayName("Should throw exception when credentials are null")
    void shouldThrowExceptionWhenCredentialsAreNull() {
        // Act & Assert
        assertThatThrownBy(() -> zoomService.createMeeting(meetingRequest, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Zoom credentials are required");
    }
}


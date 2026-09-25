package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.ai.service.ConsentService;
import com.smart.therapy.flow.client.entity.PatientConsent;
import com.smart.therapy.flow.client.repository.PatientConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentService Unit Tests")
class ConsentServiceTest {

    @Mock
    private PatientConsentRepository consentRepository;

    @InjectMocks
    private ConsentService consentService;

    private PatientConsent activeConsent;
    private PatientConsent withdrawnConsent;

    @BeforeEach
    void setUp() {
        activeConsent = PatientConsent.builder()
                .consentType(com.smart.therapy.flow.client.enums.ConsentType.AI_PROCESSING)
                .granted(true)
                .grantedAt(Instant.now().minusSeconds(86400))
                .withdrawnAt(null)
                .build();

        withdrawnConsent = PatientConsent.builder()
                .consentType(com.smart.therapy.flow.client.enums.ConsentType.AI_PROCESSING)
                .granted(true)
                .grantedAt(Instant.now().minusSeconds(172800))
                .withdrawnAt(Instant.now().minusSeconds(86400))
                .build();
    }

    @Test
    @DisplayName("Should return true when client has active AI consent")
    void shouldReturnTrueWhenClientHasActiveConsent() {
        // Arrange
        Long clientId = 1L;
        when(consentRepository.findByClientIdOrderByCreatedAtDesc(clientId))
                .thenReturn(List.of(activeConsent));

        // Act
        ConsentService.ConsentCheckResult result = consentService.checkAIProcessingConsent(clientId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isHasConsent()).isTrue();
        verify(consentRepository).findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Test
    @DisplayName("Should return false when client has no consent")
    void shouldReturnFalseWhenClientHasNoConsent() {
        // Arrange
        Long clientId = 1L;
        when(consentRepository.findByClientIdOrderByCreatedAtDesc(clientId))
                .thenReturn(List.of());

        // Act
        ConsentService.ConsentCheckResult result = consentService.checkAIProcessingConsent(clientId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isHasConsent()).isFalse();
        assertThat(result.getMessage()).isNotNull();
        verify(consentRepository).findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Test
    @DisplayName("Should return false when consent is withdrawn")
    void shouldReturnFalseWhenConsentIsWithdrawn() {
        // Arrange
        Long clientId = 1L;
        when(consentRepository.findByClientIdOrderByCreatedAtDesc(clientId))
                .thenReturn(List.of(withdrawnConsent));

        // Act
        ConsentService.ConsentCheckResult result = consentService.checkAIProcessingConsent(clientId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isHasConsent()).isFalse();
        assertThat(result.getMessage()).isNotNull();
        verify(consentRepository).findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Test
    @DisplayName("Should return false when consent is not granted")
    void shouldReturnFalseWhenConsentNotGranted() {
        // Arrange
        Long clientId = 1L;
        activeConsent.setGranted(false);
        when(consentRepository.findByClientIdOrderByCreatedAtDesc(clientId))
                .thenReturn(List.of(activeConsent));

        // Act
        ConsentService.ConsentCheckResult result = consentService.checkAIProcessingConsent(clientId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isHasConsent()).isFalse();
        verify(consentRepository).findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Test
    @DisplayName("Should return false on error (fail-closed)")
    void shouldReturnFalseOnError() {
        // Arrange
        Long clientId = 1L;
        when(consentRepository.findByClientIdOrderByCreatedAtDesc(clientId))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        ConsentService.ConsentCheckResult result = consentService.checkAIProcessingConsent(clientId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isHasConsent()).isFalse(); // Fail-closed
        assertThat(result.getMessage()).isNotNull();
        assertThat(result.getError()).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception when client ID is null")
    void shouldThrowExceptionWhenClientIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> consentService.checkAIProcessingConsent(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Client ID is required");
    }
}


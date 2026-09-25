package com.smart.therapy.flow.ai.service;

import com.smart.therapy.flow.client.entity.PatientConsent;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.client.repository.PatientConsentRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for GDPR consent validation and management.
 * Implements fail-closed validation: returns false on errors to comply with GDPR
 * "no processing without consent" requirement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final PatientConsentRepository consentRepository;

    /**
     * Check if client has granted consent for AI processing.
     * FAIL-CLOSED: Returns false on errors to comply with GDPR requirements.
     * 
     * @param clientId The client ID to check
     * @return Consent check result with hasConsent flag and optional message
     */
    @Transactional(readOnly = true)
    public ConsentCheckResult checkAIProcessingConsent(Long clientId) {
        Objects.requireNonNull(clientId, "Client ID is required");

        try {
            List<PatientConsent> consents = consentRepository.findByClientIdOrderByCreatedAtDesc(clientId);

            // Find the most recent AI processing consent using enum
            List<PatientConsent> aiConsents = consents.stream()
                    .filter(c -> ConsentType.AI_PROCESSING.equals(c.getConsentType()))
                    .sorted(Comparator.comparing((PatientConsent c) -> 
                            c.getGrantedAt() != null ? c.getGrantedAt() : java.time.Instant.EPOCH, 
                            Comparator.reverseOrder()))
                    .toList();

            if (aiConsents.isEmpty()) {
                return ConsentCheckResult.builder()
                        .hasConsent(false)
                        .message("AI processing consent has not been granted. Please update your privacy settings in the client portal.")
                        .build();
            }

            // Get the most recent consent
            PatientConsent latestConsent = aiConsents.get(0);

            // Check if consent is granted and not withdrawn
            if (!Boolean.TRUE.equals(latestConsent.getGranted()) || latestConsent.getWithdrawnAt() != null) {
                return ConsentCheckResult.builder()
                        .hasConsent(false)
                        .message("AI processing consent has been withdrawn. To use AI features, please grant consent in your privacy settings.")
                        .build();
            }

            return ConsentCheckResult.builder()
                    .hasConsent(true)
                    .build();
        } catch (Exception error) {
            log.error("[GDPR CRITICAL] Error checking AI consent for client {}", clientId, error);
            // FAIL-CLOSED: Deny processing on errors to comply with GDPR
            // This prevents database outages or bugs from bypassing consent requirements
            return ConsentCheckResult.builder()
                    .hasConsent(false)
                    .message("Unable to verify AI processing consent due to a system error. Please try again later or contact support.")
                    .error(error.getMessage())
                    .build();
        }
    }

    /**
     * Check if client has granted consent for a specific consent type.
     * FAIL-CLOSED: Returns false on errors to comply with GDPR requirements.
     * 
     * @param clientId The client ID to check
     * @param consentType The consent type to check
     * @return Consent check result with hasConsent flag and optional message
     */
    @Transactional(readOnly = true)
    public ConsentCheckResult checkConsent(Long clientId, ConsentType consentType) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(consentType, "Consent type is required");

        try {
            List<PatientConsent> consents = consentRepository.findByClientIdAndConsentType(clientId, consentType);

            if (consents.isEmpty()) {
                return ConsentCheckResult.builder()
                        .hasConsent(false)
                        .message(String.format("%s consent has not been granted. Please update your privacy settings in the client portal.", 
                                consentType.getDisplayName()))
                        .build();
            }

            // Get the most recent consent
            PatientConsent latestConsent = consents.stream()
                    .max(Comparator.comparing((PatientConsent c) -> 
                            c.getGrantedAt() != null ? c.getGrantedAt() : java.time.Instant.EPOCH))
                    .orElse(null);

            if (latestConsent == null) {
                return ConsentCheckResult.builder()
                        .hasConsent(false)
                        .message(String.format("%s consent has not been granted.", consentType.getDisplayName()))
                        .build();
            }

            // Check if consent is granted and not withdrawn
            if (!Boolean.TRUE.equals(latestConsent.getGranted()) || latestConsent.getWithdrawnAt() != null) {
                return ConsentCheckResult.builder()
                        .hasConsent(false)
                        .message(String.format("%s consent has been withdrawn. To use this feature, please grant consent in your privacy settings.", 
                                consentType.getDisplayName()))
                        .build();
            }

            return ConsentCheckResult.builder()
                    .hasConsent(true)
                    .build();
        } catch (Exception error) {
            log.error("[GDPR CRITICAL] Error checking consent for client {} and type {}", clientId, consentType, error);
            // FAIL-CLOSED: Deny processing on errors to comply with GDPR
            return ConsentCheckResult.builder()
                    .hasConsent(false)
                    .message("Unable to verify consent due to a system error. Please try again later or contact support.")
                    .error(error.getMessage())
                    .build();
        }
    }

    /**
     * Get the most recent active consent for a client and consent type.
     * 
     * @param clientId The client ID
     * @param consentType The consent type
     * @return Optional containing the active consent if found
     */
    @Transactional(readOnly = true)
    public Optional<PatientConsent> getActiveConsent(Long clientId, ConsentType consentType) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(consentType, "Consent type is required");

        try {
            return consentRepository.findFirstByClientIdAndConsentTypeOrderByCreatedAtDesc(clientId, consentType)
                    .filter(PatientConsent::isActive);
        } catch (Exception error) {
            log.error("Error getting active consent for client {} and type {}", clientId, consentType, error);
            return Optional.empty();
        }
    }

    /**
     * Check if client has active consent (convenience method).
     * 
     * @param clientId The client ID
     * @param consentType The consent type
     * @return true if active consent exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasActiveConsent(Long clientId, ConsentType consentType) {
        return getActiveConsent(clientId, consentType).isPresent();
    }

    @Data
    @Builder
    public static class ConsentCheckResult {
        private boolean hasConsent;
        private String message;
        private String error;
    }
}


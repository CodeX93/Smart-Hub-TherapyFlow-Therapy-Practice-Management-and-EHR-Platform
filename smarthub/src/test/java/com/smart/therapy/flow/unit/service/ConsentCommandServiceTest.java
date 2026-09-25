package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.PatientConsent;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.client.portal.dto.PortalConsentResponse;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.repository.PatientConsentRepository;
import com.smart.therapy.flow.client.service.ConsentCommandService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.AuditService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentCommandService portal toggle")
class ConsentCommandServiceTest {

    @Mock
    private PatientConsentRepository patientConsentRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ConsentCommandService consentCommandService;

    private Client client;
    private User therapist;

    @BeforeEach
    void setUp() {
        therapist = User.builder().fullName("Therapist").build();
        therapist.setId(6L);
        client = Client.builder().fullName("Client").assignedTherapist(therapist).build();
        client.setId(10L);
    }

    @Test
    @DisplayName("Re-grant creates a new record when latest consent was withdrawn")
    void regrantAfterWithdrawalCreatesNewRecord() {
        PatientConsent withdrawn = PatientConsent.builder()
                .client(client)
                .consentType(ConsentType.AI_PROCESSING)
                .consentFormVersion("1.0")
                .granted(false)
                .grantedAt(Instant.parse("2026-06-16T10:27:24Z"))
                .withdrawnAt(Instant.parse("2026-06-16T10:27:24Z"))
                .build();
        withdrawn.setId(10L);

        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(patientConsentRepository.findLatestByClientIdAndConsentType(10L, ConsentType.AI_PROCESSING))
                .thenReturn(Optional.of(withdrawn));
        when(patientConsentRepository.save(any(PatientConsent.class))).thenAnswer(invocation -> {
            PatientConsent saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        PortalConsentResponse response = consentCommandService.togglePortalConsent(
                10L,
                ConsentType.AI_PROCESSING,
                true,
                "1.0",
                "Consent granted via client portal",
                "client_portal",
                "127.0.0.1",
                "jest");

        ArgumentCaptor<PatientConsent> captor = ArgumentCaptor.forClass(PatientConsent.class);
        verify(patientConsentRepository).save(captor.capture());
        assertThat(captor.getValue().getGranted()).isTrue();
        assertThat(captor.getValue().getWithdrawnAt()).isNull();
        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getGranted()).isTrue();
        assertThat(response.getConsentType()).isEqualTo("AI Processing Consent");
    }

    @Test
    @DisplayName("No-op when latest consent already matches requested state")
    void noOpWhenLatestAlreadyMatches() {
        PatientConsent granted = PatientConsent.builder()
                .client(client)
                .consentType(ConsentType.AI_PROCESSING)
                .consentFormVersion("1.0")
                .granted(true)
                .grantedAt(Instant.parse("2026-06-16T10:30:00Z"))
                .build();
        granted.setId(11L);

        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(patientConsentRepository.findLatestByClientIdAndConsentType(10L, ConsentType.AI_PROCESSING))
                .thenReturn(Optional.of(granted));

        PortalConsentResponse response = consentCommandService.togglePortalConsent(
                10L,
                ConsentType.AI_PROCESSING,
                true,
                "1.0",
                "Consent granted via client portal",
                "client_portal",
                "127.0.0.1",
                "jest");

        verify(patientConsentRepository, never()).save(any());
        assertThat(response.getId()).isEqualTo(11L);
    }
}

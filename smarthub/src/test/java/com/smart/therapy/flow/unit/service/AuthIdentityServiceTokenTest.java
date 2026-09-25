package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthIdentityServiceTokenTest {

    @Mock private AuthIdentityRepository authIdentityRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OrganisationRepository organisationRepository;
    @InjectMocks private AuthIdentityService service;

    @Test
    void storesNewResetAndActivationTokensAsSha256Fingerprints() {
        Instant expiry = Instant.now().plusSeconds(60);
        ArgumentCaptor<String> resetCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> activationCaptor = ArgumentCaptor.forClass(String.class);

        service.setPasswordResetToken(7L, "reset-secret", expiry);
        service.setEmailVerificationToken(7L, "activation-secret", expiry);

        verify(authIdentityRepository).updatePasswordResetToken(eq(7L), resetCaptor.capture(), eq(expiry));
        verify(authIdentityRepository).updateEmailVerificationToken(eq(7L), activationCaptor.capture(), eq(expiry));
        assertThat(resetCaptor.getValue()).startsWith("sha256:").doesNotContain("reset-secret");
        assertThat(activationCaptor.getValue()).startsWith("sha256:").doesNotContain("activation-secret");
    }

    @Test
    void resolvesHashedResetTokenAndFallsBackToLegacyPlaintext() {
        AuthIdentity hashedIdentity = AuthIdentity.builder().id(7L).build();
        AuthIdentity legacyIdentity = AuthIdentity.builder().id(8L).build();
        when(authIdentityRepository.findByPasswordResetToken(
                argThat(value -> value != null && value.startsWith("sha256:"))))
                .thenReturn(Optional.of(hashedIdentity), Optional.empty());
        when(authIdentityRepository.findByPasswordResetToken("legacy-secret"))
                .thenReturn(Optional.of(legacyIdentity));

        assertThat(service.findByPasswordResetToken("new-secret")).isSameAs(hashedIdentity);
        assertThat(service.findByPasswordResetToken("legacy-secret")).isSameAs(legacyIdentity);
    }

    @Test
    void locksIdentityOnFifthSerializedFailedLogin() {
        AuthIdentity identity = AuthIdentity.builder()
                .id(9L)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .build();
        when(authIdentityRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(identity));

        for (int i = 0; i < 5; i++) {
            service.recordFailedLogin(9L);
        }

        assertThat(identity.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(identity.getAccountLocked()).isTrue();
        assertThat(identity.getLockedUntil()).isAfter(Instant.now());
    }
}

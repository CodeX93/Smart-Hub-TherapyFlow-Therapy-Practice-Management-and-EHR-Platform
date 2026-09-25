package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.*;
import com.smart.therapy.flow.auth.repository.*;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {
    @Mock AuthMfaCredentialRepository credentialRepository;
    @Mock AuthMfaRecoveryCodeRepository recoveryCodeRepository;
    @Mock AuthMfaLoginChallengeRepository challengeRepository;
    @Mock AuthIdentityRepository identityRepository;
    @Mock TotpService totpService;
    @Mock EncryptionService encryptionService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider tokenProvider;
    @Mock AuthSessionService authSessionService;
    @Mock MfaOtpService otpService;
    @Mock MfaOtpDeliveryService otpDeliveryService;
    @Mock AuditLogService auditLogService;

    private MfaService service;
    private AuthIdentity identity;
    private AuthMfaCredential credential;

    @BeforeEach
    void setUp() {
        service = new MfaService(credentialRepository, recoveryCodeRepository, challengeRepository,
                identityRepository, totpService, encryptionService, passwordEncoder, tokenProvider,
                authSessionService, otpService, otpDeliveryService, auditLogService,
                mock(com.smart.therapy.flow.common.metrics.AuthAbuseMetrics.class),
                mock(AuthKnownDeviceService.class));
        ReflectionTestUtils.setField(service, "maxAttempts", 2);
        ReflectionTestUtils.setField(service, "lockoutSeconds", 300L);
        ReflectionTestUtils.setField(service, "recoveryCodeCount", 2);
        identity = AuthIdentity.builder().id(7L).loginIdentifier("admin@example.com").isActive(true).build();
        credential = AuthMfaCredential.builder()
                .id(10L).authIdentity(identity).enabled(true).encryptedSecret("ciphertext")
                .totpEnabled(true).mfaMethod(com.smart.therapy.flow.auth.enums.MfaMethod.TOTP)
                .build();
    }

    @Test
    void confirmsEnrollmentAndReturnsRecoveryCodesOnlyAsHashesToPersistence() {
        credential.setEnabled(false);
        credential.setEncryptedPendingSecret("pending-ciphertext");
        credential.setMfaMethod(com.smart.therapy.flow.auth.enums.MfaMethod.TOTP);
        when(credentialRepository.findByAuthIdentityIdForUpdate(7L)).thenReturn(Optional.of(credential));
        when(encryptionService.decrypt("pending-ciphertext")).thenReturn("BASE32SECRET");
        when(totpService.verify(eq("BASE32SECRET"), eq("123456"), any(), eq(1), isNull()))
                .thenReturn(123L);
        when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));

        var response = service.confirmEnrollment(7L, "123456");

        assertThat(response.enabled()).isTrue();
        assertThat(response.recoveryCodes()).hasSize(2).allMatch(v -> v.contains("-"));
        assertThat(credential.getEncryptedSecret()).isEqualTo("pending-ciphertext");
        assertThat(credential.getEncryptedPendingSecret()).isNull();
        assertThat(credential.getLastAcceptedTimeStep()).isEqualTo(123L);
        assertThat(credential.getTotpEnabled()).isTrue();
        verify(authSessionService).revokeAllForAuthId(7L);
        verify(recoveryCodeRepository, times(2)).save(argThat(saved ->
                saved.getCodeHash().startsWith("hash:")
                        && !response.recoveryCodes().contains(saved.getCodeHash())));
    }

    @Test
    void rejectsRefreshTokensIssuedBeforeMfaWasConfirmed() {
        credential.setConfirmedAt(Instant.parse("2026-07-19T12:00:00Z"));
        when(credentialRepository.findByAuthIdentityId(7L)).thenReturn(Optional.of(credential));

        assertThat(service.isRefreshTokenStale(
                7L, Instant.parse("2026-07-19T11:59:59Z"))).isTrue();
        assertThat(service.isRefreshTokenStale(
                7L, Instant.parse("2026-07-19T12:00:01Z"))).isFalse();
    }

    @Test
    void consumesRecoveryCodeOnlyOnce() {
        AuthMfaRecoveryCode recovery = AuthMfaRecoveryCode.builder()
                .id(30L).credential(credential).codeHash("strong-hash").build();
        when(credentialRepository.findByAuthIdentityIdForUpdate(7L)).thenReturn(Optional.of(credential));
        when(recoveryCodeRepository.findByCredentialIdAndUsedAtIsNull(10L))
                .thenAnswer(i -> recovery.getUsedAt() == null ? List.of(recovery) : List.of());
        when(passwordEncoder.matches("ABCD1234", "strong-hash")).thenReturn(true);
        stubChallenge("challenge-1", "jti-1");

        service.verifyLoginChallenge("challenge-1", "ABCD-1234");
        assertThat(recovery.getUsedAt()).isNotNull();

        stubChallenge("challenge-2", "jti-2");
        assertThatThrownBy(() -> service.verifyLoginChallenge("challenge-2", "ABCD-1234"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void locksIdentityAfterConfiguredFailedAttempts() {
        when(credentialRepository.findByAuthIdentityIdForUpdate(7L)).thenReturn(Optional.of(credential));
        when(encryptionService.decrypt("ciphertext")).thenReturn("SECRET");
        when(totpService.verify(eq("SECRET"), eq("000000"), any(), eq(1), isNull())).thenReturn(null);
        stubChallenge("challenge-1", "jti-1");
        stubChallenge("challenge-2", "jti-2");

        assertThatThrownBy(() -> service.verifyLoginChallenge("challenge-1", "000000"))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> service.verifyLoginChallenge("challenge-2", "000000"))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(credential.getLockedUntil()).isAfter(Instant.now());
        stubChallenge("challenge-3", "jti-3");
        assertThatThrownBy(() -> service.verifyLoginChallenge("challenge-3", "000000"))
                .isInstanceOf(StoryApiException.class);
    }

    @Test
    void disablingMfaClearsSecretsAndRevokesSessions() {
        when(credentialRepository.findByAuthIdentityIdForUpdate(7L)).thenReturn(Optional.of(credential));
        when(encryptionService.decrypt("ciphertext")).thenReturn("SECRET");
        when(totpService.verify(eq("SECRET"), eq("123456"), any(), eq(1), isNull())).thenReturn(200L);

        service.disable(7L, "123456");

        assertThat(credential.getEnabled()).isFalse();
        assertThat(credential.getEncryptedSecret()).isNull();
        verify(recoveryCodeRepository).deleteByCredentialId(10L);
        verify(authSessionService).revokeAllForAuthId(7L);
    }

    @Test
    void startChangeRequiresCurrentCodeThenStoresPendingTotp() {
        when(credentialRepository.findByAuthIdentityIdForUpdate(7L)).thenReturn(Optional.of(credential));
        when(identityRepository.findById(7L)).thenReturn(Optional.of(identity));
        when(encryptionService.decrypt("ciphertext")).thenReturn("SECRET");
        when(totpService.verify(eq("SECRET"), eq("123456"), any(), eq(1), isNull())).thenReturn(111L);
        when(otpService.resolveEnrollmentMethod(eq(com.smart.therapy.flow.auth.enums.MfaMethod.EMAIL), isNull(), eq(identity)))
                .thenReturn(com.smart.therapy.flow.auth.enums.MfaMethod.EMAIL);
        when(otpService.resolveEmailDestination(identity)).thenReturn("admin@example.com");
        when(otpService.changeEnrollmentJtiForAuthId(7L)).thenReturn("change-enrollment:7");
        when(otpService.issueAndSendOtp(anyString(), eq(7L), any(), eq(com.smart.therapy.flow.auth.enums.MfaMethod.EMAIL), anyString()))
                .thenReturn("a***@example.com");
        when(otpDeliveryService.isChannelAvailable(any())).thenReturn(true);

        var response = service.startChange(7L, "123456", com.smart.therapy.flow.auth.enums.MfaMethod.EMAIL, null);

        assertThat(response.method()).isEqualTo(com.smart.therapy.flow.auth.enums.MfaMethod.EMAIL);
        assertThat(response.codeSent()).isTrue();
        assertThat(credential.getPendingMfaMethod()).isEqualTo(com.smart.therapy.flow.auth.enums.MfaMethod.EMAIL);
        assertThat(credential.getEnabled()).isTrue();
        assertThat(credential.getMfaMethod()).isEqualTo(com.smart.therapy.flow.auth.enums.MfaMethod.TOTP);
    }

    @Test
    void confirmChangeSwapsMethodAndKeepsCurrentSession() {
        credential.setPendingMfaMethod(com.smart.therapy.flow.auth.enums.MfaMethod.EMAIL);
        when(credentialRepository.findByAuthIdentityIdForUpdate(7L)).thenReturn(Optional.of(credential));
        when(otpService.changeEnrollmentJtiForAuthId(7L)).thenReturn("change-enrollment:7");
        when(otpService.verifyOtp(eq("change-enrollment:7"), any(), eq("654321"), any())).thenReturn(true);

        var response = service.confirmChange(7L, "654321", "keep-jti");

        assertThat(response.enabled()).isTrue();
        assertThat(response.recoveryCodes()).isEmpty();
        assertThat(response.enrolledMethods()).contains("TOTP", "EMAIL");
        assertThat(credential.getMfaMethod()).isEqualTo(com.smart.therapy.flow.auth.enums.MfaMethod.EMAIL);
        assertThat(credential.getEmailEnabled()).isTrue();
        assertThat(credential.getTotpEnabled()).isTrue();
        assertThat(credential.getPendingMfaMethod()).isNull();
        verify(authSessionService).revokeAllExceptJwtId(7L, "keep-jti");
        verify(authSessionService, never()).revokeAllForAuthId(7L);
    }

    @Test
    void rejectsWrongOrExpiredChallengeTypeBeforeCodeLookup() {
        when(tokenProvider.isMfaChallengeToken("expired-or-wrong")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyLoginChallenge("expired-or-wrong", "123456"))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(credentialRepository);
    }

    private void stubChallenge(String token, String jti) {
        AuthMfaLoginChallenge challenge = AuthMfaLoginChallenge.builder()
                .authIdentity(identity).expiresAt(Instant.now().plusSeconds(60)).build();
        when(tokenProvider.isMfaChallengeToken(token)).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken(token)).thenReturn(7L);
        when(tokenProvider.getJtiFromToken(token)).thenReturn(jti);
        when(challengeRepository.findByJtiHash(anyString())).thenReturn(Optional.of(challenge));
    }
}

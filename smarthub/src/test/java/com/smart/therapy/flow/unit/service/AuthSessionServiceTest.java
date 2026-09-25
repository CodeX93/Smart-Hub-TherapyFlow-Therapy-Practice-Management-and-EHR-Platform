package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.AuthSession;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthSessionRepository;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock private AuthSessionRepository authSessionRepository;
    @Mock private AuthIdentityRepository authIdentityRepository;
    @InjectMocks private AuthSessionService service;

    @Test
    void expiredSessionIsInvalidEvenWhenNotExplicitlyRevoked() {
        AuthSession session = AuthSession.builder()
                .jwtId("expired-jti")
                .expiresAt(Instant.now().minusSeconds(1))
                .revoked(false)
                .build();
        when(authSessionRepository.findByJwtIdForUpdate("expired-jti")).thenReturn(Optional.of(session));

        assertThat(service.isRevoked("expired-jti")).isTrue();
    }

    @Test
    void idleSessionIsInvalidEvenWhenAccessTokenHasNotExpired() {
        AuthSession session = AuthSession.builder()
                .jwtId("idle-jti")
                .expiresAt(Instant.now().plusSeconds(3600))
                .lastActivityAt(Instant.now().minusSeconds(1800))
                .idleExpiresAt(Instant.now().minusSeconds(1))
                .revoked(false)
                .build();
        when(authSessionRepository.findByJwtIdForUpdate("idle-jti")).thenReturn(Optional.of(session));

        assertThat(service.isRevoked("idle-jti")).isTrue();
    }

    @Test
    void activeSessionExtendsIdleDeadline() {
        Instant previousActivity = Instant.now().minusSeconds(120);
        Instant previousIdleExpiry = Instant.now().plusSeconds(60);
        AuthSession session = AuthSession.builder()
                .jwtId("active-jti")
                .expiresAt(Instant.now().plusSeconds(3600))
                .lastActivityAt(previousActivity)
                .idleExpiresAt(previousIdleExpiry)
                .revoked(false)
                .build();
        when(authSessionRepository.findByJwtIdForUpdate("active-jti")).thenReturn(Optional.of(session));

        assertThat(service.isRevoked("active-jti")).isFalse();
        assertThat(session.getLastActivityAt()).isAfter(previousActivity);
        assertThat(session.getIdleExpiresAt()).isAfter(previousIdleExpiry);
        verify(authSessionRepository).save(session);
    }

    @Test
    void backfillsMissingIpAndUserAgentOnActiveCheck() {
        AuthSession session = AuthSession.builder()
                .jwtId("bare-jti")
                .expiresAt(Instant.now().plusSeconds(3600))
                .lastActivityAt(Instant.now())
                .idleExpiresAt(Instant.now().plusSeconds(900))
                .revoked(false)
                .ipAddress(null)
                .userAgent(null)
                .build();
        when(authSessionRepository.findByJwtIdForUpdate("bare-jti")).thenReturn(Optional.of(session));

        assertThat(service.isRevoked(
                "bare-jti",
                "203.0.113.10",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X) Chrome/126.0.0.0 Safari/537.36"))
                .isFalse();
        assertThat(session.getIpAddress()).isEqualTo("203.0.113.10");
        assertThat(session.getUserAgent()).contains("Chrome");
        verify(authSessionRepository).save(session);
    }

    @Test
    void revokesOnlySessionsIssuedByEndedImpersonation() {
        AuthSession associated = AuthSession.builder()
                .jwtId("impersonated-jti")
                .expiresAt(Instant.now().plusSeconds(60))
                .revoked(false)
                .impersonationSessionId(42L)
                .build();
        when(authSessionRepository.findByImpersonationSessionId(42L)).thenReturn(List.of(associated));

        service.revokeAllForImpersonationSession(42L);

        assertThat(associated.getRevoked()).isTrue();
        assertThat(associated.getRevokedAt()).isNotNull();
        verify(authSessionRepository).save(associated);
    }
}

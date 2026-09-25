package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthRefreshToken;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthRefreshTokenRepository;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRefreshTokenServiceTest {

    @Mock
    private AuthRefreshTokenRepository refreshTokenRepository;

    @Mock
    private AuthIdentityRepository authIdentityRepository;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthRefreshTokenReuseRecorder reuseRecorder;

    private AuthRefreshTokenService service;

    private AuthIdentity identity;

    @BeforeEach
    void setUp() {
        service = new AuthRefreshTokenService(
                refreshTokenRepository, authIdentityRepository, tokenProvider, reuseRecorder);
        identity = AuthIdentity.builder()
                .id(7L)
                .loginIdentifier("user@example.com")
                .isActive(true)
                .build();
    }

    @Test
    void issueCreatesHashedRow() {
        String issuedToken = "issued-refresh-token";
        Date expiryDate = new Date(Instant.now().plusSeconds(604800).toEpochMilli());

        when(authIdentityRepository.findById(7L)).thenReturn(Optional.of(identity));
        when(tokenProvider.generateRefreshToken(eq(7L), anyString())).thenReturn(issuedToken);
        when(tokenProvider.getExpirationDateFromToken(issuedToken)).thenReturn(expiryDate);

        String result = service.issueRefreshToken(7L, "127.0.0.1", "JUnit");

        assertThat(result).isEqualTo(issuedToken);

        ArgumentCaptor<AuthRefreshToken> savedCaptor = ArgumentCaptor.forClass(AuthRefreshToken.class);
        verify(refreshTokenRepository).save(savedCaptor.capture());
        AuthRefreshToken saved = savedCaptor.getValue();
        assertThat(saved.getAuthIdentity()).isSameAs(identity);
        assertThat(saved.getTokenHash()).isEqualTo(AuthRefreshTokenService.hashToken(issuedToken));
        assertThat(saved.getFamilyId()).isNotBlank();
        assertThat(saved.getExpiresAt()).isEqualTo(expiryDate.toInstant());
        assertThat(saved.getRevoked()).isFalse();
        assertThat(saved.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(saved.getUserAgent()).isEqualTo("JUnit");
    }

    @Test
    void rotateSupersedesOldToken() {
        String presentedToken = "current-refresh-token";
        String nextToken = "next-refresh-token";
        String familyId = "family-abc";
        Instant expiry = Instant.now().plusSeconds(604800);
        AuthRefreshToken current = AuthRefreshToken.builder()
                .authIdentity(identity)
                .familyId(familyId)
                .tokenHash(AuthRefreshTokenService.hashToken(presentedToken))
                .expiresAt(expiry)
                .revoked(false)
                .build();

        when(tokenProvider.validateToken(presentedToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(presentedToken)).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken(presentedToken)).thenReturn(7L);
        when(refreshTokenRepository.findByTokenHash(AuthRefreshTokenService.hashToken(presentedToken)))
                .thenReturn(Optional.of(current));
        when(tokenProvider.generateRefreshToken(eq(7L), eq(familyId), anyLong())).thenReturn(nextToken);
        when(tokenProvider.getExpirationDateFromToken(nextToken)).thenReturn(Date.from(expiry));

        String result = service.rotateRefreshToken(presentedToken, "10.0.0.1", "agent");

        assertThat(result).isEqualTo(nextToken);
        assertThat(current.getRevoked()).isTrue();
        assertThat(current.getRevokedAt()).isNotNull();
        assertThat(current.getReplacedByHash()).isEqualTo(AuthRefreshTokenService.hashToken(nextToken));

        ArgumentCaptor<AuthRefreshToken> savedCaptor = ArgumentCaptor.forClass(AuthRefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(savedCaptor.capture());
        AuthRefreshToken successor = savedCaptor.getAllValues().stream()
                .filter(row -> row != current)
                .findFirst()
                .orElseThrow();
        assertThat(successor.getFamilyId()).isEqualTo(familyId);
        assertThat(successor.getTokenHash()).isEqualTo(AuthRefreshTokenService.hashToken(nextToken));
        assertThat(successor.getRevoked()).isFalse();
        assertThat(successor.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(successor.getUserAgent()).isEqualTo("agent");
        verify(tokenProvider).generateRefreshToken(eq(7L), eq(familyId), anyLong());
    }

    @Test
    void presentingRevokedTokenRevokesFamilyOnReuse() {
        String presentedToken = "reused-refresh-token";
        String familyId = "family-reuse";
        AuthRefreshToken revoked = AuthRefreshToken.builder()
                .id(42L)
                .authIdentity(identity)
                .familyId(familyId)
                .tokenHash(AuthRefreshTokenService.hashToken(presentedToken))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(tokenProvider.validateToken(presentedToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(presentedToken)).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken(presentedToken)).thenReturn(7L);
        when(refreshTokenRepository.findByTokenHash(AuthRefreshTokenService.hashToken(presentedToken)))
                .thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.rotateRefreshToken(presentedToken, null, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revoked");

        // The theft response goes through the REQUIRES_NEW recorder so the 401's rollback cannot undo it.
        verify(reuseRecorder).recordReuse(eq(42L), eq(familyId), any(Instant.class));
        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeFamily(anyString(), any());
        verify(tokenProvider, never()).generateRefreshToken(anyLong(), anyString());
    }

    @Test
    void presentingJustRotatedTokenWhileSuccessorLivesIssuesAnotherSuccessor() {
        String presentedToken = "just-rotated-refresh-token";
        String familyId = "family-race";
        AuthRefreshToken rotated = AuthRefreshToken.builder()
                .id(43L)
                .authIdentity(identity)
                .familyId(familyId)
                .tokenHash(AuthRefreshTokenService.hashToken(presentedToken))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .revokedAt(Instant.now().minusSeconds(2))
                .replacedByHash("successor-hash")
                .build();
        AuthRefreshToken successor = AuthRefreshToken.builder()
                .id(44L).familyId(familyId).tokenHash("successor-hash").revoked(false).build();

        when(tokenProvider.validateToken(presentedToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(presentedToken)).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken(presentedToken)).thenReturn(7L);
        when(refreshTokenRepository.findByTokenHash(AuthRefreshTokenService.hashToken(presentedToken)))
                .thenReturn(Optional.of(rotated));
        when(refreshTokenRepository.findByTokenHash("successor-hash")).thenReturn(Optional.of(successor));
        when(tokenProvider.generateRefreshToken(eq(7L), eq(familyId), anyLong())).thenReturn("sibling-token");

        assertThat(service.rotateRefreshToken(presentedToken, null, null)).isEqualTo("sibling-token");

        verify(reuseRecorder, never()).recordReuse(any(), any(), any());
        ArgumentCaptor<AuthRefreshToken> saved = ArgumentCaptor.forClass(AuthRefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(AuthRefreshTokenService.hashToken("sibling-token"));
        assertThat(saved.getValue().getFamilyId()).isEqualTo(familyId);
    }

    @Test
    void presentingJustRotatedTokenWhoseSuccessorWasRevokedIsTheft() {
        String presentedToken = "rotated-then-logged-out";
        AuthRefreshToken rotated = AuthRefreshToken.builder()
                .id(45L)
                .authIdentity(identity)
                .familyId("family-logged-out")
                .tokenHash(AuthRefreshTokenService.hashToken(presentedToken))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .revokedAt(Instant.now().minusSeconds(2))
                .replacedByHash("revoked-successor")
                .build();
        AuthRefreshToken successor = AuthRefreshToken.builder()
                .id(46L).familyId("family-logged-out").tokenHash("revoked-successor").revoked(true).build();

        when(tokenProvider.validateToken(presentedToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(presentedToken)).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken(presentedToken)).thenReturn(7L);
        when(refreshTokenRepository.findByTokenHash(AuthRefreshTokenService.hashToken(presentedToken)))
                .thenReturn(Optional.of(rotated));
        when(refreshTokenRepository.findByTokenHash("revoked-successor")).thenReturn(Optional.of(successor));

        assertThatThrownBy(() -> service.rotateRefreshToken(presentedToken, null, null))
                .isInstanceOf(UnauthorizedException.class);
        verify(reuseRecorder).recordReuse(eq(45L), eq("family-logged-out"), any(Instant.class));
    }

    @Test
    void invalidTokenThrowsUnauthorizedException() {
        when(tokenProvider.validateToken("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> service.rotateRefreshToken("bad-token", null, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(refreshTokenRepository, never()).findByTokenHash(anyString());
        verify(refreshTokenRepository, never()).revokeFamily(anyString(), any());
        verify(reuseRecorder, never()).recordReuse(any(), any(), any());
    }

    @Test
    void expiredButUnrevokedTokenIsRejectedWithoutReuseResponse() {
        String presentedToken = "expired-refresh-token";
        AuthRefreshToken expired = AuthRefreshToken.builder()
                .id(43L)
                .authIdentity(identity)
                .familyId("family-expired")
                .tokenHash(AuthRefreshTokenService.hashToken(presentedToken))
                .expiresAt(Instant.now().minusSeconds(60))
                .revoked(false)
                .build();

        when(tokenProvider.validateToken(presentedToken)).thenReturn(true);
        when(tokenProvider.isRefreshToken(presentedToken)).thenReturn(true);
        when(tokenProvider.getAuthIdFromToken(presentedToken)).thenReturn(7L);
        when(refreshTokenRepository.findByTokenHash(AuthRefreshTokenService.hashToken(presentedToken)))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotateRefreshToken(presentedToken, null, null))
                .isInstanceOf(UnauthorizedException.class);

        verify(reuseRecorder, never()).recordReuse(any(), any(), any());
    }
}

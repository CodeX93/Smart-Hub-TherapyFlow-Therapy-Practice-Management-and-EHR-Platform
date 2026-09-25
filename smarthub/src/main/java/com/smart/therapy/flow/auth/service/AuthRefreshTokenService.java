package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthRefreshToken;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthRefreshTokenRepository;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthRefreshTokenService {

    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthRefreshTokenReuseRecorder reuseRecorder;

    /**
     * How long after a rotation the token it replaced may still be presented without counting as theft.
     * A page torn down mid-refresh (reload, navigation, closed tab) never stores the rotated cookie the
     * server already issued, so its next load legitimately presents the old one.
     */
    @Value("${app.security.refresh-cookie.rotation-grace-seconds:20}")
    private long rotationGraceSeconds = 20L;

    @Value("${jwt.refresh-expiration:604800000}")
    private long defaultRefreshExpirationMs = 604800000L;

    @Transactional
    public String issueRefreshToken(Long authId, String ipAddress, String userAgent) {
        return issueRefreshToken(authId, UUID.randomUUID().toString(), ipAddress, userAgent, null);
    }

    @Transactional
    public String issueRefreshToken(Long authId, String ipAddress, String userAgent, Long refreshExpirationMs) {
        return issueRefreshToken(authId, UUID.randomUUID().toString(), ipAddress, userAgent, refreshExpirationMs);
    }

    @Transactional
    public String rotateRefreshToken(String presentedRefreshToken, String ipAddress, String userAgent) {
        if (!tokenProvider.validateToken(presentedRefreshToken) || !tokenProvider.isRefreshToken(presentedRefreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long authId = tokenProvider.getAuthIdFromToken(presentedRefreshToken);
        String tokenHash = hashToken(presentedRefreshToken);
        Optional<AuthRefreshToken> stored = refreshTokenRepository.findByTokenHash(tokenHash);

        if (stored.isEmpty()) {
            // Legacy refresh tokens issued before rotation table — accept once and enroll.
            String familyId = Optional.ofNullable(tokenProvider.getRefreshFamilyIdFromToken(presentedRefreshToken))
                    .orElseGet(() -> UUID.randomUUID().toString());
            return issueRefreshToken(authId, familyId, ipAddress, userAgent, null);
        }

        AuthRefreshToken current = stored.get();
        Instant now = Instant.now();
        if (isRotationRace(current, now)) {
            log.info("Refresh token presented {}s after its rotation; treating as a lost response, authId={} familyId={}",
                    java.time.Duration.between(current.getRevokedAt(), now).toSeconds(), authId, current.getFamilyId());
            return issueSuccessor(current, authId, ipAddress, userAgent, now).token();
        }
        if (Boolean.TRUE.equals(current.getRevoked()) || current.getExpiresAt() == null || !current.getExpiresAt().isAfter(now)) {
            if (Boolean.TRUE.equals(current.getRevoked())) {
                // Own transaction: the 401 below rolls back the caller's, and the theft response must not.
                reuseRecorder.recordReuse(current.getId(), current.getFamilyId(), now);
                log.warn("Refresh token reuse detected for authId={} familyId={}", authId, current.getFamilyId());
            }
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        Successor next = issueSuccessor(current, authId, ipAddress, userAgent, now);
        current.setRevoked(true);
        current.setRevokedAt(now);
        current.setReplacedByHash(next.hash());
        refreshTokenRepository.save(current);
        return next.token();
    }

    private record Successor(String token, String hash) {
    }

    /**
     * The token was rotated moments ago and what replaced it is still live: the client most likely never
     * received the rotation (see {@link #rotationGraceSeconds}). Logout and a detected theft both revoke
     * the successor, so neither can be undone by replaying the old token here.
     */
    private boolean isRotationRace(AuthRefreshToken current, Instant now) {
        if (!Boolean.TRUE.equals(current.getRevoked()) || Boolean.TRUE.equals(current.getReuseDetected())
                || current.getRevokedAt() == null || current.getReplacedByHash() == null
                || current.getRevokedAt().plusSeconds(rotationGraceSeconds).isBefore(now)
                || current.getExpiresAt() == null || !current.getExpiresAt().isAfter(now)) {
            return false;
        }
        return refreshTokenRepository.findByTokenHash(current.getReplacedByHash())
                .map(successor -> !Boolean.TRUE.equals(successor.getRevoked()))
                .orElse(false);
    }

    /** Mints and stores the next token of {@code current}'s family, keeping any extended lifetime. */
    private Successor issueSuccessor(AuthRefreshToken current, Long authId, String ipAddress, String userAgent,
                                     Instant now) {
        // Preserve extended stay-signed-in lifetime; otherwise use default sliding window.
        long remainingMs = Math.max(0L, current.getExpiresAt().toEpochMilli() - now.toEpochMilli());
        long nextExpirationMs = remainingMs > defaultRefreshExpirationMs
                ? remainingMs
                : defaultRefreshExpirationMs;
        String nextToken = tokenProvider.generateRefreshToken(authId, current.getFamilyId(), nextExpirationMs);
        String nextHash = hashToken(nextToken);
        Date expiry = tokenProvider.getExpirationDateFromToken(nextToken);

        AuthRefreshToken next = AuthRefreshToken.builder()
                .authIdentity(current.getAuthIdentity())
                .familyId(current.getFamilyId())
                .tokenHash(nextHash)
                .expiresAt(expiry != null ? expiry.toInstant() : now.plusSeconds(604800))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        refreshTokenRepository.save(next);
        return new Successor(nextToken, nextHash);
    }

    @Transactional
    public void revokePresentedToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hashToken(refreshToken)).ifPresent(token -> {
            Instant now = Instant.now();
            refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
        });
    }

    @Transactional
    public void revokeAllForAuthId(Long authId) {
        if (authId == null) {
            return;
        }
        refreshTokenRepository.revokeAllForAuthId(authId, Instant.now());
    }

    private String issueRefreshToken(
            Long authId,
            String familyId,
            String ipAddress,
            String userAgent,
            Long refreshExpirationMs) {
        AuthIdentity identity = authIdentityRepository.findById(authId)
                .orElseThrow(() -> new UnauthorizedException("User account is disabled"));
        String refreshToken = refreshExpirationMs != null && refreshExpirationMs > 0
                ? tokenProvider.generateRefreshToken(authId, familyId, refreshExpirationMs)
                : tokenProvider.generateRefreshToken(authId, familyId);
        Date expiry = tokenProvider.getExpirationDateFromToken(refreshToken);
        AuthRefreshToken stored = AuthRefreshToken.builder()
                .authIdentity(identity)
                .familyId(familyId)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(expiry != null ? expiry.toInstant() : Instant.now().plusSeconds(604800))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        refreshTokenRepository.save(stored);
        return refreshToken;
    }

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

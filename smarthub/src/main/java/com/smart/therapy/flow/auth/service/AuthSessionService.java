package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthSession;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enterprise session control: create session on login, revoke by jti, cleanup expired.
 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final AuthSessionRepository authSessionRepository;
    private final AuthIdentityRepository authIdentityRepository;

    @Value("${app.auth.session.idle-timeout-seconds:900}")
    private long idleTimeoutSeconds = 900;

    @Value("${app.auth.session.activity-touch-interval-seconds:60}")
    private long activityTouchIntervalSeconds = 60;

    @Transactional
    public AuthSession createSession(Long authId, String jwtId, Instant expiresAt, String ipAddress, String userAgent) {
        return createSession(authId, jwtId, expiresAt, ipAddress, userAgent, null);
    }

    @Transactional
    public AuthSession createSession(
            Long authId,
            String jwtId,
            Instant expiresAt,
            String ipAddress,
            String userAgent,
            Long impersonationSessionId
    ) {
        AuthIdentity identity = authIdentityRepository.findById(authId).orElse(null);
        if (identity == null) return null;
        Instant now = Instant.now();
        Instant absoluteExpiry = expiresAt != null ? expiresAt : now.plusSeconds(86400);
        AuthSession session = AuthSession.builder()
                .authIdentity(identity)
                .jwtId(jwtId)
                .issuedAt(now)
                .expiresAt(absoluteExpiry)
                .lastActivityAt(now)
                .idleExpiresAt(earlierOf(now.plusSeconds(idleTimeoutSeconds), absoluteExpiry))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .impersonationSessionId(impersonationSessionId)
                .build();
        return authSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<AuthSession> findByJwtId(String jwtId) {
        return authSessionRepository.findByJwtId(jwtId);
    }

    @Transactional
    public boolean isRevoked(String jwtId) {
        return isRevoked(jwtId, null, null);
    }

    /**
     * Returns true if the session is revoked/expired. When still active, optionally
     * backfills missing IP / user-agent from the current request.
     */
    @Transactional
    public boolean isRevoked(String jwtId, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        Optional<AuthSession> sessionResult = authSessionRepository.findByJwtIdForUpdate(jwtId);
        if (sessionResult.isEmpty()) {
            return true;
        }

        AuthSession session = sessionResult.get();
        if (Boolean.TRUE.equals(session.getRevoked())
                || session.getExpiresAt() == null
                || !session.getExpiresAt().isAfter(now)
                || session.getIdleExpiresAt() == null
                || !session.getIdleExpiresAt().isAfter(now)) {
            return true;
        }

        boolean dirty = false;
        Instant lastActivity = session.getLastActivityAt();
        if (lastActivity == null
                || lastActivity.plusSeconds(activityTouchIntervalSeconds).isBefore(now)) {
            session.setLastActivityAt(now);
            session.setIdleExpiresAt(earlierOf(
                    now.plusSeconds(idleTimeoutSeconds),
                    session.getExpiresAt()));
            dirty = true;
        }
        if (shouldReplaceClientHint(session.getIpAddress(), ipAddress)) {
            session.setIpAddress(ipAddress.trim());
            dirty = true;
        }
        if (shouldReplaceClientHint(session.getUserAgent(), userAgent)) {
            session.setUserAgent(userAgent.trim());
            dirty = true;
        }
        if (dirty) {
            authSessionRepository.save(session);
        }
        return false;
    }

    private static boolean shouldReplaceClientHint(String existing, String incoming) {
        if (!StringUtils.hasText(incoming) || "unknown".equalsIgnoreCase(incoming.trim())) {
            return false;
        }
        return !StringUtils.hasText(existing) || "unknown".equalsIgnoreCase(existing.trim());
    }

    /**
     * Returns true if the identity has at least one non-revoked, non-expired session.
     * Used by TokenBlacklistService fallback when Redis is unavailable.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveSession(Long authId) {
        if (authId == null) return false;
        Instant now = Instant.now();
        return authSessionRepository.findByAuthIdentityId(authId).stream()
                .anyMatch(s -> isActive(s, now));
    }

    @Transactional(readOnly = true)
    public List<AuthSession> listActiveSessions(Long authId) {
        if (authId == null) {
            return List.of();
        }
        Instant now = Instant.now();
        return authSessionRepository.findByAuthIdentityId(authId).stream()
                .filter(s -> isActive(s, now))
                .sorted(Comparator.comparing(AuthSession::getLastActivityAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public void revokeByIdForAuth(Long authId, Long sessionId) {
        AuthSession session = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (session.getAuthIdentity() == null
                || !authId.equals(session.getAuthIdentity().getId())) {
            throw new IllegalArgumentException("Session not found");
        }
        session.setRevoked(true);
        session.setRevokedAt(Instant.now());
        authSessionRepository.save(session);
    }

    @Transactional
    public void revokeAllExceptJwtId(Long authId, String keepJwtId) {
        Instant now = Instant.now();
        for (AuthSession session : authSessionRepository.findByAuthIdentityId(authId)) {
            if (keepJwtId != null && keepJwtId.equals(session.getJwtId())) {
                continue;
            }
            if (Boolean.TRUE.equals(session.getRevoked())) {
                continue;
            }
            session.setRevoked(true);
            session.setRevokedAt(now);
            authSessionRepository.save(session);
        }
    }

    private static boolean isActive(AuthSession session, Instant now) {
        return !Boolean.TRUE.equals(session.getRevoked())
                && session.getExpiresAt() != null
                && session.getExpiresAt().isAfter(now)
                && session.getIdleExpiresAt() != null
                && session.getIdleExpiresAt().isAfter(now);
    }

    @Transactional
    public void revokeByJwtId(String jwtId) {
        authSessionRepository.findByJwtId(jwtId).ifPresent(session -> {
            session.setRevoked(true);
            session.setRevokedAt(Instant.now());
            authSessionRepository.save(session);
        });
    }

    @Transactional
    public void revokeAllForAuthId(Long authId) {
        revokeSessions(authSessionRepository.findByAuthIdentityId(authId));
    }

    @Transactional
    public void revokeAllForImpersonationSession(Long impersonationSessionId) {
        if (impersonationSessionId == null) return;
        revokeSessions(authSessionRepository.findByImpersonationSessionId(impersonationSessionId));
    }

    private void revokeSessions(Iterable<AuthSession> sessions) {
        sessions.forEach(session -> {
            session.setRevoked(true);
            session.setRevokedAt(Instant.now());
            authSessionRepository.save(session);
        });
    }

    @Transactional
    public int deleteExpiredBefore(Instant before) {
        return authSessionRepository.deleteExpiredBefore(before);
    }

    public static String generateJti() {
        return UUID.randomUUID().toString();
    }

    private Instant earlierOf(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }
}

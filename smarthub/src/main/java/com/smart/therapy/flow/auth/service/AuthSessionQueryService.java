package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.dto.AuthSessionDtos;
import com.smart.therapy.flow.auth.entity.AuthSession;
import com.smart.therapy.flow.auth.util.DeviceInfoUtil;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthSessionQueryService {

    private final AuthSessionService authSessionService;
    private final JwtTokenProvider tokenProvider;

    @Transactional(readOnly = true)
    public AuthSessionDtos.SessionListResponse listSessions(Long authId, String bearerToken) {
        return listSessions(authId, bearerToken, null, null);
    }

    @Transactional
    public AuthSessionDtos.SessionListResponse listSessions(
            Long authId,
            String bearerToken,
            String requestIp,
            String requestUserAgent) {
        String currentJti = extractJti(bearerToken);
        if (StringUtils.hasText(currentJti)) {
            // Backfill IP / UA onto the current session when older rows were saved as null.
            authSessionService.isRevoked(currentJti, requestIp, requestUserAgent);
        }
        List<AuthSessionDtos.SessionItem> items = authSessionService.listActiveSessions(authId).stream()
                .map(session -> toItem(session, currentJti, requestIp, requestUserAgent))
                .toList();
        return new AuthSessionDtos.SessionListResponse(items.size(), items);
    }

    @Transactional
    public void revokeSession(Long authId, Long sessionId, String bearerToken) {
        String currentJti = extractJti(bearerToken);
        AuthSession session = authSessionService.listActiveSessions(authId).stream()
                .filter(s -> sessionId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Session not found or already ended"));
        if (currentJti != null && currentJti.equals(session.getJwtId())) {
            throw new BadRequestException("Cannot sign out the current session here. Use Logout instead.");
        }
        try {
            authSessionService.revokeByIdForAuth(authId, sessionId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Transactional
    public void revokeOtherSessions(Long authId, String bearerToken) {
        String currentJti = extractJti(bearerToken);
        if (!StringUtils.hasText(currentJti)) {
            throw new UnauthorizedException("Authentication required");
        }
        authSessionService.revokeAllExceptJwtId(authId, currentJti);
    }

    private static AuthSessionDtos.SessionItem toItem(
            AuthSession session,
            String currentJti,
            String requestIp,
            String requestUserAgent) {
        boolean current = currentJti != null && currentJti.equals(session.getJwtId());
        String ip = session.getIpAddress();
        String ua = session.getUserAgent();
        if (current) {
            if (isMissingHint(ip)) {
                ip = requestIp;
            }
            if (isMissingHint(ua)) {
                ua = requestUserAgent;
            }
        }
        return new AuthSessionDtos.SessionItem(
                session.getId(),
                DeviceInfoUtil.deviceLabel(ua),
                normalizeHint(ip),
                ua,
                session.getIssuedAt(),
                session.getLastActivityAt(),
                session.getExpiresAt(),
                current
        );
    }

    private static boolean isMissingHint(String value) {
        return !StringUtils.hasText(value) || "unknown".equalsIgnoreCase(value.trim());
    }

    private static String normalizeHint(String value) {
        return isMissingHint(value) ? null : value.trim();
    }

    private String extractJti(String bearerToken) {
        if (!StringUtils.hasText(bearerToken)) {
            return null;
        }
        String token = bearerToken.startsWith("Bearer ")
                ? bearerToken.substring(7).trim()
                : bearerToken.trim();
        try {
            return tokenProvider.getJtiFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }
}

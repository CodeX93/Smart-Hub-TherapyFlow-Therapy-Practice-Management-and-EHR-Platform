package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.auth.service.AuthRefreshTokenService;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service to manage JWT token blacklist using Redis.
 * When a user logs out, their token is added to the blacklist until it expires.
 * Falls back to AuthSessionService (auth_sessions) when Redis is unavailable or disabled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final JwtTokenProvider tokenProvider;
    private final AuthSessionService authSessionService;
    private final AuthRefreshTokenService authRefreshTokenService;

    /**
     * Add a token to the blacklist until its expiration time
     */
    public void blacklistToken(String token) {
        if (redisTemplate.isEmpty()) {
            return;
        }
        try {
            Date expirationDate = tokenProvider.getExpirationDateFromToken(token);
            long ttl = expirationDate.getTime() - System.currentTimeMillis();
            
            if (ttl > 0) {
                String key = BLACKLIST_PREFIX + token;
                redisTemplate.get().opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
                log.debug("Token blacklisted until: {}", expirationDate);
            } else {
                log.debug("Token already expired, no need to blacklist");
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
            // Don't throw - allow logout to succeed even if blacklisting fails
        }
    }

    /**
     * Check if a token is blacklisted.
     * Redis provides fast explicit-token revocation while auth_sessions remains the
     * authoritative inactivity and account-session check.
     *
     * @param token  JWT token to check
     * @param authId Auth identity ID from the token (for fallback check)
     * @return true if token is blacklisted or identity has no active sessions (fallback)
     */
    public boolean isTokenBlacklisted(String token, Long authId) {
        if (redisTemplate.isPresent()) {
            try {
                String key = BLACKLIST_PREFIX + token;
                Boolean exists = redisTemplate.get().hasKey(key);
                if (Boolean.TRUE.equals(exists)) {
                    return true; // Token is blacklisted in Redis
                }
            } catch (Exception e) {
                log.warn("Redis unavailable, falling back to AuthSessionService for authId: {}", authId, e);
            }
        }
        try {
            return !authSessionService.hasActiveSession(authId);
        } catch (Exception ex) {
            log.error("Failed to check auth sessions for authId: {}", authId, ex);
            return true; // fail closed for security
        }
    }

    /**
     * Remove a token from blacklist (if needed)
     */
    public void removeFromBlacklist(String token) {
        if (redisTemplate.isEmpty()) {
            return;
        }
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.get().delete(key);
            log.debug("Token removed from blacklist");
        } catch (Exception e) {
            log.error("Failed to remove token from blacklist", e);
        }
    }

    /**
     * Blacklist all tokens for an identity (useful for password change, account lock, etc.).
     * Revoke all sessions via AuthSessionService so all JTIs are invalidated.
     */
    public void blacklistAllUserTokens(Long authId) {
        try {
            if (authId != null) {
                authSessionService.revokeAllForAuthId(authId);
                authRefreshTokenService.revokeAllForAuthId(authId);
                log.info("Revoked all sessions and refresh families for authId: {}", authId);
            }
        } catch (Exception e) {
            log.error("Failed to revoke all sessions for authId: {}", authId, e);
        }
    }

    /**
     * Alias for blacklistAllUserTokens — revoke all sessions for an identity.
     * Used for password changes and security events.
     */
    public void blacklistUserTokens(Long authId) {
        blacklistAllUserTokens(authId);
    }
}


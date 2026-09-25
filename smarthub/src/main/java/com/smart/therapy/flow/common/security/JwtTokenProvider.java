package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.common.tenant.TenantContext;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:therapy-flow-secret-key-change-in-production-min-256-bits}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long jwtExpirationInMs;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days in milliseconds
    private long jwtRefreshExpirationInMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 256 bits (32 bytes) for HS256
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication, String jti) {
        return generateToken(authentication, jti, null, null);
    }

    public String generateToken(
            Authentication authentication,
            String jti,
            Date expiresAtOverride,
            Map<String, Object> additionalClaims
    ) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipal) {
            AuthPrincipal authPrincipal = (AuthPrincipal) principal;
            Date now = new Date();
            Date expiryDate = expiresAtOverride != null ? expiresAtOverride : new Date(now.getTime() + jwtExpirationInMs);
            String authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
            String tenantSchema = TenantContext.getSchemaName();
            if (tenantSchema == null || tenantSchema.isBlank()) {
                tenantSchema = "public";
            }
            Long orgId = TenantContext.getOrganisationId();
            JwtBuilder builder = Jwts.builder()
                    .setSubject(String.valueOf(authPrincipal.getAuthId()))
                    .setId(jti != null ? jti : java.util.UUID.randomUUID().toString())
                    .claim("loginIdentifier", authPrincipal.getLoginIdentifier())
                    .claim("authorities", authorities)
                    .claim("identityType", authPrincipal.getIdentityType().name())
                    .claim("tenantSchema", tenantSchema)
                    .claim("orgId", orgId)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256);
            if (additionalClaims != null && !additionalClaims.isEmpty()) {
                additionalClaims.forEach(builder::claim);
            }
            return builder.compact();
        }
        throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass().getName());
    }

    /** @deprecated Use generateToken(Authentication, String jti) with AuthPrincipal. */
    @Deprecated
    public String generateToken(Authentication authentication) {
        return generateToken(authentication, java.util.UUID.randomUUID().toString());
    }

    public String generateRefreshToken(Long authId) {
        return generateRefreshToken(authId, null);
    }

    public String generateRefreshToken(Long authId, String familyId) {
        return generateRefreshToken(authId, familyId, jwtRefreshExpirationInMs);
    }

    public String generateRefreshToken(Long authId, String familyId, long expirationMs) {
        Date now = new Date();
        long ttl = expirationMs > 0 ? expirationMs : jwtRefreshExpirationInMs;
        Date expiryDate = new Date(now.getTime() + ttl);
        String tenantSchema = TenantContext.getSchemaName();
        if (tenantSchema == null || tenantSchema.isBlank()) {
            tenantSchema = "public";
        }
        Long orgId = TenantContext.getOrganisationId();
        JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(authId))
                .setId(java.util.UUID.randomUUID().toString())
                .claim("type", "refresh")
                .claim("tenantSchema", tenantSchema)
                .claim("orgId", orgId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256);
        if (familyId != null && !familyId.isBlank()) {
            builder.claim("familyId", familyId);
        }
        return builder.compact();
    }

    public String generateStepUpToken(Long authId, long validitySeconds) {
        Date now = new Date();
        long seconds = validitySeconds > 0 ? validitySeconds : 900L;
        Date expiryDate = new Date(now.getTime() + seconds * 1000L);
        return Jwts.builder()
                .setSubject(String.valueOf(authId))
                .setId(java.util.UUID.randomUUID().toString())
                .claim("type", "step_up")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateTranscriptionWebSocketTicket(
            AuthPrincipal principal,
            String uploadId,
            long validitySeconds
    ) {
        if (principal == null || uploadId == null || uploadId.isBlank()) {
            throw new IllegalArgumentException("Principal and uploadId are required");
        }
        Date now = new Date();
        long seconds = validitySeconds > 0 ? validitySeconds : 60L;
        String tenantSchema = TenantContext.getSchemaName();
        if (tenantSchema == null || tenantSchema.isBlank()) {
            tenantSchema = "public";
        }
        return Jwts.builder()
                .setSubject(String.valueOf(principal.getAuthId()))
                .setId(java.util.UUID.randomUUID().toString())
                .claim("type", "transcription_ws")
                .claim("uploadId", uploadId)
                .claim("tenantSchema", tenantSchema)
                .claim("orgId", TenantContext.getOrganisationId())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + seconds * 1000L))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateTranscriptionWebSocketTicket(String token, String expectedUploadId) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return "transcription_ws".equals(claims.get("type", String.class))
                    && expectedUploadId != null
                    && expectedUploadId.equals(claims.get("uploadId", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateStepUpToken(String token, Long expectedAuthId) {
        if (token == null || token.isBlank() || expectedAuthId == null) {
            return false;
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (!"step_up".equals(claims.get("type", String.class))) {
                return false;
            }
            return expectedAuthId.toString().equals(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getRefreshFamilyIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("familyId", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String generatePasswordChangeToken(Long authId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 900000); // 15 minutes
        String tenantSchema = TenantContext.getSchemaName();
        if (tenantSchema == null || tenantSchema.isBlank()) {
            tenantSchema = "public";
        }
        return Jwts.builder()
                .setSubject(String.valueOf(authId))
                .claim("type", "password_change")
                .claim("tenantSchema", tenantSchema)
                .claim("orgId", TenantContext.getOrganisationId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateMfaChallengeToken(Long authId, long validitySeconds) {
        Date now = new Date();
        String tenantSchema = TenantContext.getSchemaName();
        if (tenantSchema == null || tenantSchema.isBlank()) {
            tenantSchema = "public";
        }
        return Jwts.builder()
                .setSubject(String.valueOf(authId))
                .setId(java.util.UUID.randomUUID().toString())
                .claim("type", "mfa_challenge")
                .claim("tenantSchema", tenantSchema)
                .claim("orgId", TenantContext.getOrganisationId())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validitySeconds * 1000L))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isMfaChallengeToken(String token) {
        try {
            return validateToken(token) && "mfa_challenge".equals(getTokenType(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String generateMfaEnrollmentToken(Long authId, long validitySeconds) {
        Date now = new Date();
        String tenantSchema = TenantContext.getSchemaName();
        if (tenantSchema == null || tenantSchema.isBlank()) {
            tenantSchema = "public";
        }
        return Jwts.builder()
                .setSubject(String.valueOf(authId))
                .setId(java.util.UUID.randomUUID().toString())
                .claim("type", "mfa_enrollment")
                .claim("tenantSchema", tenantSchema)
                .claim("orgId", TenantContext.getOrganisationId())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validitySeconds * 1000L))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isMfaEnrollmentToken(String token) {
        try {
            return validateToken(token) && "mfa_enrollment".equals(getTokenType(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validatePasswordChangeToken(String token, Long authId) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String type = claims.get("type", String.class);
            Long sub = Long.parseLong(claims.getSubject());
            return "password_change".equals(type) && sub.equals(authId);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Subject is authId in unified auth. */
    public Long getAuthIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    public String getJtiFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getId();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** @deprecated Prefer getAuthIdFromToken; subject is now authId. */
    @Deprecated
    public Long getUserIdFromToken(String token) {
        return getAuthIdFromToken(token);
    }

    /** @deprecated For client tokens subject is authId; resolve Client via ClientRepository.findByAuthId. */
    @Deprecated
    public Long getClientIdFromToken(String token) {
        return getAuthIdFromToken(token);
    }

    public String getTokenType(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("type", String.class);
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("loginIdentifier", String.class);
    }

    public String getAuthoritiesFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("authorities", String.class);
    }

    /** Tenant schema bound to this token (for cross-tenant reuse check). Returns "public" if claim missing. */
    public String getTenantSchemaFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String s = claims.get("tenantSchema", String.class);
            return s != null && !s.isBlank() ? s : "public";
        } catch (JwtException | IllegalArgumentException e) {
            return "public";
        }
    }

    /** Super-admin auth id when token was issued for impersonation; null for normal sessions. */
    public Long getImpersonatedByAuthIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Object v = claims.get("impersonatedByAuthId");
            if (v instanceof Number n) {
                return n.longValue();
            }
            if (v instanceof String s && !s.isBlank()) {
                return Long.parseLong(s);
            }
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Long getOrganisationIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Object v = claims.get("orgId");
            if (v instanceof Number n) {
                return n.longValue();
            }
            if (v instanceof String s && !s.isBlank()) {
                return Long.parseLong(s);
            }
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration();
    }

    public Date getIssuedAtFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getIssuedAt();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}

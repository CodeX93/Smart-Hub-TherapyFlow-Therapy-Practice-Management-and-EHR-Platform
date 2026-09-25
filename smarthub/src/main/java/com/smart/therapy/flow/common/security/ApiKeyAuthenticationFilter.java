package com.smart.therapy.flow.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.superadmin.entity.PlatformApiKey;
import com.smart.therapy.flow.superadmin.entity.PlatformApiKeyScope;
import com.smart.therapy.flow.superadmin.repository.PlatformApiKeyRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformApiKeyScopeRepository;
import com.smart.therapy.flow.superadmin.service.PlatformApiKeyUsageService;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String SUPER_ADMIN_PATH_PREFIX = "/api/v1/super-admin/";
    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String AUTH_SCHEME = "ApiKey ";

    private final PlatformApiKeyRepository apiKeyRepository;
    private final PlatformApiKeyScopeRepository apiKeyScopeRepository;
    private final PlatformApiKeyUsageService usageService;
    private final PlatformAuditService platformAuditService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isSuperAdminPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = resolveApiKey(request);
        if (!StringUtils.hasText(rawKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        String hash = sha256(rawKey);
        Optional<PlatformApiKey> row = apiKeyRepository.findByKeyHash(hash);
        if (row.isEmpty() || !Boolean.TRUE.equals(row.get().getIsActive())) {
            logAuthFailure(null, request, "API_KEY_INVALID", "API key invalid or revoked");
            reject(response, "API key is invalid or revoked");
            return;
        }

        PlatformApiKey key = row.get();
        if (key.getExpiresAt() != null && !key.getExpiresAt().isAfter(Instant.now())) {
            logAuthFailure(null, request, "API_KEY_EXPIRED", "API key expired");
            reject(response, "API key has expired");
            return;
        }

        List<String> scopes = resolveScopes(key);
        List<SimpleGrantedAuthority> authorities = resolveAuthorities(scopes);
        AuthPrincipal principal = new AuthPrincipal(
                0L,
                "api_key",
                "",
                authorities,
                true,
                IdentityType.API,
                null
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        usageService.markUsed(key.getId());
        filterChain.doFilter(request, response);
    }

    private static boolean isSuperAdminPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith(SUPER_ADMIN_PATH_PREFIX);
    }

    private static String resolveApiKey(HttpServletRequest request) {
        String header = request.getHeader(HEADER_API_KEY);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith(AUTH_SCHEME)) {
            return auth.substring(AUTH_SCHEME.length()).trim();
        }
        return null;
    }

    private List<String> parseScopes(String scopesJson) {
        if (!StringUtils.hasText(scopesJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(scopesJson, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static List<SimpleGrantedAuthority> resolveAuthorities(List<String> scopes) {
        boolean hasLegacyRead = scopes.stream().anyMatch(s -> "read".equalsIgnoreCase(s));
        boolean hasLegacyWrite = scopes.stream().anyMatch(s -> "write".equalsIgnoreCase(s) || "admin".equalsIgnoreCase(s));

        Set<SimpleGrantedAuthority> authorities = new java.util.LinkedHashSet<>();
        for (String scope : scopes) {
            if (!StringUtils.hasText(scope)) {
                continue;
            }
            if (scope.contains(":")) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
            } else if ("read".equalsIgnoreCase(scope)) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_platform:read"));
            } else if ("write".equalsIgnoreCase(scope) || "admin".equalsIgnoreCase(scope)) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_platform:write"));
            }
        }
        if (hasLegacyWrite) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_SUPER_ADMIN"));
        } else if (hasLegacyRead) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_AUDITOR"));
        }
        return List.copyOf(authorities);
    }

    private List<String> resolveScopes(PlatformApiKey key) {
        List<PlatformApiKeyScope> rows = apiKeyScopeRepository.findByApiKeyId(key.getId());
        if (rows != null && !rows.isEmpty()) {
            return rows.stream()
                    .map(PlatformApiKeyScope::getScope)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return parseScopes(key.getScopesJson());
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash API key", e);
        }
    }

    private static void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private void logAuthFailure(Long authId, HttpServletRequest request, String action, String details) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        String ua = request.getHeader("User-Agent");
        String path = request.getRequestURI();
        String summary = "ip=" + ip + ", path=" + path + ", ua=" + (ua != null ? ua : "");
        platformAuditService.log(authId, action, "Auth", null,
                details + " | " + summary);
    }
}

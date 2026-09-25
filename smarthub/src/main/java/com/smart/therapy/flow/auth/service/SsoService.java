package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.entity.*;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.exception.TenantUnavailableException;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.OrganisationSsoConfig;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.OrganisationSsoConfigRepository;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Hardened SSO login flow:
 * - one-time random state + nonce + PKCE
 * - strict redirect URI (must match configured URI)
 * - ID token validation via provider JWK set
 * - required organisation domain allowlist
 * - controlled JIT provisioning
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SsoService {

    private static final long SESSION_EXPIRY_SECONDS = 86400L;
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String GOOGLE_SCOPE = "openid email profile";
    private static final String GOOGLE_ISSUER_1 = "https://accounts.google.com";
    private static final String GOOGLE_ISSUER_2 = "accounts.google.com";
    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

    @Value("${app.security.sso.state-ttl-seconds:600}")
    private long stateTtlSeconds;

    @Value("${app.security.sso.callback-rate-limit.enabled:true}")
    private boolean callbackRateLimitEnabled;

    @Value("${app.security.sso.callback-rate-limit.requests-per-minute:60}")
    private int callbackRequestsPerMinute;

    private final OrganisationSsoConfigRepository ssoConfigRepository;
    private final OrganisationRepository organisationRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthIdentityDetailsService authIdentityDetailsService;
    private final JwtTokenProvider tokenProvider;
    private final AuthSessionService authSessionService;
    private final AuthIdentityService authIdentityService;
    private final AuthRefreshTokenService authRefreshTokenService;
    private final SsoSecurityService ssoSecurityService;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, Counter> callbackCounters = new ConcurrentHashMap<>();

    /**
     * Build redirect URL for provider using one-time secure state + PKCE.
     */
    @Transactional
    public String buildAuthorizationUrl(String provider, Long organisationId, String redirectUri) {
        String normalizedProvider = normalizeProvider(provider);
        OrganisationSsoConfig config = requireEnabledSsoConfig(organisationId, normalizedProvider);
        String effectiveRedirect = normalizeAndValidateRedirectUri(config, redirectUri);

        SsoSecurityService.SsoStatePayload state = ssoSecurityService.createState(
                organisationId,
                normalizedProvider,
                effectiveRedirect,
                Instant.now(),
                stateTtlSeconds
        );

        if ("GOOGLE".equals(normalizedProvider)) {
            return GOOGLE_AUTH_URL + "?client_id=" + encode(config.getClientId())
                    + "&redirect_uri=" + encode(effectiveRedirect)
                    + "&response_type=code&scope=" + encode(GOOGLE_SCOPE)
                    + "&state=" + encode(state.stateToken())
                    + "&nonce=" + encode(state.nonce())
                    + "&code_challenge=" + encode(SsoSecurityService.pkceChallengeS256(state.pkceVerifier()))
                    + "&code_challenge_method=S256"
                    + "&access_type=offline&prompt=consent";
        }
        throw new IllegalArgumentException("Unsupported SSO provider: " + normalizedProvider);
    }

    /**
     * Handle callback using server-side state lookup and OIDC token validation.
     */
    @Transactional
    public JwtAuthenticationResponse handleCallback(String code, String stateToken, String ipAddress) {
        return handleCallback(code, stateToken, ipAddress, null);
    }

    @Transactional
    public JwtAuthenticationResponse handleCallback(
            String code, String stateToken, String ipAddress, String userAgent) {
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrg = TenantContext.getOrganisationId();
        try {
            if (code == null || code.isBlank()) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Missing code");
            }

        // Resolve provider/organisation from one-time state.
        SsoSecurityService.ConsumedState consumed = ssoSecurityService.consumeStateOrThrow(
                stateToken,
                Instant.now()
        );

        // callback abuse throttling per org+ip
        if (isRateLimited(consumed.organisationId(), ipAddress)) {
            throw new StoryApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Too many SSO callback attempts");
        }

        Organisation org = organisationRepository.findById(consumed.organisationId())
                .orElseThrow(() -> new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_STATE_INVALID", "Organisation not found"));

        TenantContext.setSchemaName(org.getSchemaName());
        TenantContext.setOrganisationId(org.getId());
        if (!TenantDirectoryService.STATUS_ACTIVE.equalsIgnoreCase(org.getStatus())) {
            throw new TenantUnavailableException("Tenant is unavailable for login");
        }

        OrganisationSsoConfig config = requireEnabledSsoConfig(org.getId(), consumed.provider());
        String effectiveRedirect = normalizeAndValidateRedirectUri(config, consumed.redirectUri());

        Map<String, Object> tokenResp = exchangeCodeForToken(
                config.getClientId(),
                config.getClientSecret(),
                code,
                effectiveRedirect,
                consumed.pkceVerifier()
        );

        String accessToken = asString(tokenResp.get("access_token"));
        String idToken = asString(tokenResp.get("id_token"));
        if (accessToken == null || idToken == null) {
            audit("SSO_STATE_INVALID", org.getId(), consumed.provider(), "missing access_token or id_token");
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_TOKEN_INVALID", "Invalid token response");
        }

        Jwt idClaims = decodeAndValidateGoogleIdToken(idToken, config.getClientId(), consumed.nonce());
        String providerUserId = idClaims.getSubject();
        String email = idClaims.getClaimAsString("email");
        Boolean emailVerified = idClaims.getClaimAsBoolean("email_verified");
        if (providerUserId == null || providerUserId.isBlank() || email == null || email.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_TOKEN_INVALID", "Missing required id_token claims");
        }
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "SSO_EMAIL_NOT_VERIFIED", "Email must be verified by provider");
        }

        String normalizedEmail = AuthIdentityService.normaliseLoginIdentifier(email);
        enforceAllowedDomainOrThrow(org.getId(), normalizedEmail);
        audit("SSO_CALLBACK_VALIDATED", org.getId(), consumed.provider(), "subject=" + safeSubject(providerUserId));

        // UserInfo remains optional enrichment.
        String fullName = tryResolveUserFullName(accessToken);

        AuthProvider authProvider = AuthProvider.valueOf(consumed.provider());
        AuthIdentity identity = resolveIdentity(authProvider, providerUserId, normalizedEmail, org.getId());
        if (identity == null) {
            identity = createSsoIdentity(authProvider, providerUserId, normalizedEmail, org);
            audit("SSO_JIT_PROVISIONED", org.getId(), consumed.provider(), "subject=" + safeSubject(providerUserId));
        }

        if (!Boolean.TRUE.equals(identity.getIsActive())) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_INACTIVE", "Account is inactive");
        }

        authIdentityService.recordSuccessfulLogin(identity.getId());
        final AuthIdentity identityFinal = identity;
        final String fullNameForUser = fullName != null ? fullName : normalizedEmail;
        final boolean[] userCreated = {false};
        User user = tenantTransactionExecutor.executeWrite(org.getId(), org.getSchemaName(), () -> {
            User existing = userRepository.findByAuthId(identityFinal.getId()).orElse(null);
            if (existing != null) {
                return existing;
            }
            userCreated[0] = true;
            return createUserWithDefaultRole(identityFinal, normalizedEmail, fullNameForUser, org);
        });
        if (userCreated[0] && user != null) {
            audit("SSO_JIT_PROVISIONED", org.getId(), consumed.provider(), "userId=" + user.getId());
        }

        String jti = AuthSessionService.generateJti();
        var userDetails = authIdentityDetailsService.loadUserByAuthId(identity.getId());
        org.springframework.security.core.Authentication auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, authIdentityDetailsService.loadAuthorities(identity.getId()));

        String accessTokenJwt = tokenProvider.generateToken(auth, jti);
        Instant expiresAt = Instant.now().plusSeconds(SESSION_EXPIRY_SECONDS);
        authSessionService.createSession(identity.getId(), jti, expiresAt, ipAddress, userAgent);
        String refreshToken = authRefreshTokenService.issueRefreshToken(identity.getId(), ipAddress, userAgent);

        List<String> roles = extractRoles(auth.getAuthorities());
        List<String> permissions = extractPermissions(auth.getAuthorities());

        audit("SSO_LOGIN_SUCCESS", org.getId(), consumed.provider(), "userId=" + (user != null ? user.getId() : null));

            return JwtAuthenticationResponse.builder()
                    .accessToken(accessTokenJwt)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .userId(user != null ? user.getId() : null)
                    .username(identity.getLoginIdentifier())
                    .email(identity.getLoginIdentifier())
                    .roles(roles)
                    .permissions(permissions)
                    .expiresIn(86400000L)
                    .passwordChangeRequired(false)
                    .build();
        } finally {
            TenantContext.clear();
            if (previousSchema != null && !previousSchema.isBlank()) {
                TenantContext.setSchemaName(previousSchema);
            }
            if (previousOrg != null) {
                TenantContext.setOrganisationId(previousOrg);
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<OrganisationSsoConfig> getSsoConfig(Long organisationId, String provider) {
        return ssoConfigRepository.findByOrganisationIdAndProviderAndIsEnabledTrue(organisationId, normalizeProvider(provider));
    }

    /**
     * Returns enabled providers for the organisation. Gated by config and required domain policy.
     */
    @Transactional(readOnly = true)
    public List<String> getEnabledProvidersForOrganisation(Long organisationId) {
        List<String> domains = ssoSecurityService.getAllowedDomains(organisationId);
        if (domains.isEmpty()) {
            return List.of();
        }
        return ssoConfigRepository.findByOrganisationIdAndIsEnabledTrue(organisationId).stream()
                .map(OrganisationSsoConfig::getProvider)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getAllowedDomains(Long organisationId) {
        return ssoSecurityService.getAllowedDomains(organisationId);
    }

    @Transactional
    public List<String> setAllowedDomains(Long organisationId, List<String> domains) {
        return ssoSecurityService.replaceAllowedDomains(organisationId, domains);
    }

    private List<String> extractRoles(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> extractPermissions(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && !authority.startsWith("ROLE_"))
                .distinct()
                .collect(Collectors.toList());
    }

    private static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Provider is required");
        }
        return provider.trim().toUpperCase(Locale.ROOT);
    }

    private OrganisationSsoConfig requireEnabledSsoConfig(Long organisationId, String provider) {
        return ssoConfigRepository.findByOrganisationIdAndProviderAndIsEnabledTrue(organisationId, provider)
                .orElseThrow(() -> new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_NOT_CONFIGURED", "SSO not configured for this organisation/provider"));
    }

    private String normalizeAndValidateRedirectUri(OrganisationSsoConfig config, String redirectUri) {
        String expected = config.getRedirectUri();
        if (expected == null || expected.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_REDIRECT_URI_MISSING", "SSO redirect URI is not configured");
        }
        String candidate = redirectUri != null && !redirectUri.isBlank() ? redirectUri.trim() : expected.trim();
        if (!expected.trim().equals(candidate)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_REDIRECT_URI_INVALID", "redirect_uri is not allowed for this organisation");
        }
        return candidate;
    }

    private Map<String, Object> exchangeCodeForToken(String clientId,
                                                     String clientSecret,
                                                     String code,
                                                     String redirectUri,
                                                     String codeVerifier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret != null ? clientSecret : "");
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");
        body.add("code_verifier", codeVerifier);
        ResponseEntity<Map> resp = restTemplate.postForEntity(GOOGLE_TOKEN_URL, new HttpEntity<>(body, headers), Map.class);
        if (resp.getStatusCode().isError() || resp.getBody() == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_TOKEN_EXCHANGE_FAILED", "Token exchange failed");
        }
        return resp.getBody();
    }

    private Jwt decodeAndValidateGoogleIdToken(String idToken, String expectedAudience, String expectedNonce) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (Exception ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_TOKEN_INVALID", "Invalid id_token signature");
        }
        String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
        if (!GOOGLE_ISSUER_1.equals(issuer) && !GOOGLE_ISSUER_2.equals(issuer)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_TOKEN_INVALID", "Invalid id_token issuer");
        }
        List<String> aud = jwt.getAudience();
        if (aud == null || !aud.contains(expectedAudience)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_TOKEN_INVALID", "Invalid id_token audience");
        }
        if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_TOKEN_INVALID", "Expired id_token");
        }
        String nonce = jwt.getClaimAsString("nonce");
        if (nonce == null || !nonce.equals(expectedNonce)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_TOKEN_INVALID", "Invalid id_token nonce");
        }
        return jwt;
    }

    @SuppressWarnings("unchecked")
    private String tryResolveUserFullName(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<Map> resp = restTemplate.exchange(GOOGLE_USERINFO_URL, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return asString(resp.getBody().get("name"));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void enforceAllowedDomainOrThrow(Long organisationId, String normalizedEmail) {
        List<String> allowedDomains = ssoSecurityService.getAllowedDomains(organisationId);
        if (allowedDomains.isEmpty()) {
            audit("SSO_LOGIN_DENIED_DOMAIN", organisationId, "GOOGLE", "allowlist missing");
            throw new StoryApiException(HttpStatus.FORBIDDEN, "SSO_DOMAIN_DENIED", "No allowed domains configured for organisation");
        }
        String domain = normalizedEmail.contains("@")
                ? normalizedEmail.substring(normalizedEmail.lastIndexOf('@') + 1)
                : "";
        if (!allowedDomains.contains(domain)) {
            audit("SSO_LOGIN_DENIED_DOMAIN", organisationId, "GOOGLE", "domain=" + domain);
            throw new StoryApiException(HttpStatus.FORBIDDEN, "SSO_DOMAIN_DENIED", "Email domain is not allowed for this organisation");
        }
    }

    private AuthIdentity resolveIdentity(AuthProvider provider, String providerUserId, String normalizedEmail, Long organisationId) {
        AuthIdentity byProvider = authIdentityRepository.findByAuthProviderAndProviderUserId(provider, providerUserId).orElse(null);
        if (byProvider != null) {
            return byProvider;
        }

        // Secondary link: only strict, same-tenant, verified email identities without provider_user_id.
        AuthIdentity byEmail = authIdentityRepository.findByNormalisedLoginIdentifier(normalizedEmail).orElse(null);
        if (byEmail == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(byEmail.getEmailVerified())) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "SSO_LINKING_DENIED", "Email is not verified");
        }
        if (byEmail.getOrganisation() == null || !Objects.equals(byEmail.getOrganisation().getId(), organisationId)) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "SSO_LINKING_DENIED", "Identity belongs to another organisation");
        }
        if (byEmail.getProviderUserId() != null && !byEmail.getProviderUserId().isBlank()) {
            throw new StoryApiException(HttpStatus.CONFLICT, "SSO_LINKING_CONFLICT", "Identity already linked to another SSO subject");
        }
        byEmail.setAuthProvider(provider);
        byEmail.setProviderUserId(providerUserId);
        byEmail.setSsoEnabled(true);
        return authIdentityRepository.save(byEmail);
    }

    private AuthIdentity createSsoIdentity(AuthProvider provider, String providerUserId, String normalizedEmail, Organisation org) {
        return authIdentityRepository.save(AuthIdentity.builder()
                .organisation(org)
                .loginIdentifier(normalizedEmail)
                .normalisedLoginIdentifier(normalizedEmail)
                .passwordHash(null)
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .authProvider(provider)
                .providerUserId(providerUserId)
                .ssoEnabled(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .build());
    }

    private User createUserWithDefaultRole(AuthIdentity identity, String email, String fullName, Organisation org) {
        User user = User.builder()
                .authIdentity(identity)
                .email(email != null ? email : identity.getLoginIdentifier())
                .fullName(fullName != null && !fullName.isBlank() ? fullName : email)
                .status(com.smart.therapy.flow.auth.dto.UserStatus.ACTIVE)
                .isActive(true)
                .build();
        user = userRepository.save(user);
        Role therapistRole = roleRepository.findByNameForOrganisation(RoleName.THERAPIST.name(), org.getId())
                .orElseThrow(() -> new StoryApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "SSO_ROLE_CONFIGURATION_ERROR",
                        "Default THERAPIST role is not configured for this organisation"
                ));
        AuthIdentityRole air = AuthIdentityRole.builder()
                .authIdentity(identity)
                .organisation(org)
                .role(therapistRole)
                .build();
        authIdentityRoleRepository.save(air);
        return user;
    }

    private boolean isRateLimited(Long organisationId, String ipAddress) {
        if (!callbackRateLimitEnabled) {
            return false;
        }
        String ip = (ipAddress == null || ipAddress.isBlank()) ? "unknown" : ipAddress.trim();
        long now = Instant.now().getEpochSecond();
        String key = "sso_cb:" + organisationId + ":" + ip + ":" + (now / 60);
        if (redisTemplate.isPresent()) {
            try {
                Long count = redisTemplate.get().opsForValue().increment(key);
                if (count != null && count == 1L) {
                    redisTemplate.get().expire(key, 70, TimeUnit.SECONDS);
                }
                return count != null && count > callbackRequestsPerMinute;
            } catch (Exception ex) {
                log.warn("SSO callback rate limit redis fallback: {}", ex.getMessage());
            }
        }
        Counter counter = callbackCounters.computeIfAbsent(key, k -> new Counter(now, 0));
        synchronized (counter) {
            if (now - counter.windowStart >= 60) {
                counter.windowStart = now;
                counter.count = 0;
            }
            counter.count++;
            return counter.count > callbackRequestsPerMinute;
        }
    }

    private void audit(String event, Long organisationId, String provider, String details) {
        log.info("SSO_AUDIT event={} orgId={} provider={} details={}", event, organisationId, provider, details);
    }

    private static String safeSubject(String sub) {
        if (sub == null || sub.isBlank()) return "n/a";
        int keep = Math.min(6, sub.length());
        return sub.substring(0, keep) + "***";
    }

    private static String encode(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static final class Counter {
        private long windowStart;
        private int count;

        private Counter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

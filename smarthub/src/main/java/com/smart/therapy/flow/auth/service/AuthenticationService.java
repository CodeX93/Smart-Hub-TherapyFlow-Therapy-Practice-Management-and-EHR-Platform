package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.metrics.AuthAbuseMetrics;
import com.smart.therapy.flow.auth.dto.ChangePasswordRequest;
import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.dto.LoginRequest;
import com.smart.therapy.flow.auth.dto.MfaDtos;
import com.smart.therapy.flow.auth.dto.PasswordResetTokenValidationResponse;
import com.smart.therapy.flow.auth.dto.RefreshTokenRequest;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.LoginAttempt;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.LoginAttemptRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.PortalAccessDisabledException;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.exception.TenantUnavailableException;
import com.smart.therapy.flow.common.security.AuthIdentityDetailsService;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.security.TokenBlacklistService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.superadmin.service.PlatformTenantRoutingService;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.dao.DataAccessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final long SESSION_EXPIRY_SECONDS = 86400L; // 24 hours

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthSessionService authSessionService;
    private final AuthIdentityService authIdentityService;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityDetailsService authIdentityDetailsService;
    private final LoginAttemptRepository loginAttemptRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TenantDirectoryService tenantDirectoryService;
    private final PlatformTenantRoutingService platformTenantRoutingService;
    private final TenantResolutionService tenantResolutionService;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final ClientRepository clientRepository;
    private final ClientPortalSettingsService portalSettingsService;
    private final MfaService mfaService;
    private final AuthRefreshTokenService authRefreshTokenService;
    private final AuthKnownDeviceService authKnownDeviceService;
    private final AuditLogService auditLogService;
    private final AuthAbuseMetrics authAbuseMetrics;

    private static final long PASSWORD_RESET_TOKEN_VALIDITY_HOURS = 24;

    @PersistenceContext
    private EntityManager entityManager;

    private boolean isPublicSchemaContext() {
        String schema = TenantContext.getSchemaName();
        return schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema);
    }

    @Transactional
    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest, String ipAddress) {
        return authenticateUser(loginRequest, ipAddress, null);
    }

    @Transactional
    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest, String ipAddress, String userAgent) {
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrg = TenantContext.getOrganisationId();
        try {
            resolveTenantContextForLogin(loginRequest);

            // Enforce organisation lifecycle: if request has tenant context, reject LOCKED/ARCHIVED/DELETED/force-disabled (defensive; TenantFilter also blocks).
            String schema = TenantContext.getSchemaName();
            if (schema != null && !schema.isBlank() && !"public".equalsIgnoreCase(schema)) {
                tenantDirectoryService.findBySchemaName(schema)
                        .filter(info -> !info.isActive() || info.isForceDisabled())
                        .ifPresent(info -> {
                            throw new TenantUnavailableException(
                                    info.isForceDisabled() ? "Tenant has been disabled" : "Tenant is unavailable for login",
                                    info.isForceDisabled() ? "TENANT_002" : null);
                        });
            }

            log.debug("Attempting staff authentication");
            AuthIdentity staffIdentity = resolveStaffIdentityForLoginAttempt(loginRequest.getUsername());
            if (staffIdentity != null && authIdentityService.isLocked(staffIdentity)) {
                throw new LockedException("Account temporarily locked");
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            
            log.debug("Staff authentication successful");

            SecurityContextHolder.getContext().setAuthentication(authentication);
            AuthPrincipal authPrincipal = (AuthPrincipal) authentication.getPrincipal();
            Long authId = authPrincipal.getAuthId();

            AuthIdentity identity = authIdentityService.getById(authId);
            if (requiresPasswordChangeBeforeLogin(identity)) {
                String changePasswordToken = tokenProvider.generatePasswordChangeToken(authId);
                log.info("First-login password change required for authId {}", authId);
                return JwtAuthenticationResponse.builder()
                        .username(authPrincipal.getLoginIdentifier())
                        .email(authPrincipal.getLoginIdentifier())
                        .passwordChangeRequired(true)
                        .changePasswordToken(changePasswordToken)
                        .message("Password change is required before login.")
                        .build();
            }

            boolean mfaEnabled = mfaService.isEnabled(authId);
            boolean mfaEnrollmentRequired =
                    mfaService.isEnrollmentRequired(authentication.getAuthorities(), mfaEnabled);
            boolean staySignedIn = Boolean.TRUE.equals(loginRequest.getStaySignedIn());
            boolean trustedDeviceLogin = mfaEnabled
                    && authKnownDeviceService.isTrustedDevice(authId, loginRequest.getDeviceTrustToken());
            if (mfaEnabled && !trustedDeviceLogin) {
                String challengeToken = mfaService.createLoginChallenge(identity);
                MfaDtos.LoginChallengeInfo challengeInfo = mfaService.getLoginChallengeInfo(authId);
                SecurityContextHolder.clearContext();
                return JwtAuthenticationResponse.builder()
                        .username(authPrincipal.getLoginIdentifier())
                        .email(authPrincipal.getLoginIdentifier())
                        .roles(extractRoles(authentication.getAuthorities()))
                        .permissions(extractPermissions(authentication.getAuthorities()))
                        .mfaRequired(true)
                        .mfaChallengeToken(challengeToken)
                        .mfaEnrollmentRequired(false)
                        .mfaMethod(challengeInfo.method().name())
                        .mfaMaskedDestination(challengeInfo.maskedDestination())
                        .mfaMethods(challengeInfo.enrolledMethods())
                        .mfaSmsAvailable(challengeInfo.smsAvailable())
                        .mfaEmailAvailable(challengeInfo.emailAvailable())
                        .message("MFA verification is required.")
                        .build();
            }
            if (mfaEnrollmentRequired) {
                MfaDtos.LoginChallengeInfo challengeInfo = mfaService.getLoginChallengeInfo(authId);
                SecurityContextHolder.clearContext();
                return JwtAuthenticationResponse.builder()
                        .username(authPrincipal.getLoginIdentifier())
                        .email(authPrincipal.getLoginIdentifier())
                        .roles(extractRoles(authentication.getAuthorities()))
                        .permissions(extractPermissions(authentication.getAuthorities()))
                        .mfaRequired(false)
                        .mfaEnrollmentRequired(true)
                        .mfaChallengeToken(mfaService.createEnrollmentChallenge(authId))
                        .mfaMethods(List.of())
                        .mfaSmsAvailable(challengeInfo.smsAvailable())
                        .mfaEmailAvailable(challengeInfo.emailAvailable())
                        .message("MFA enrollment is required before login.")
                        .build();
            }

            if (trustedDeviceLogin) {
                authKnownDeviceService.touchTrustedDevice(
                        authId, loginRequest.getDeviceTrustToken(), ipAddress, userAgent);
                auditTrustedDeviceMfaSkip(authId, authPrincipal.getLoginIdentifier(), ipAddress, userAgent);
            }

            String jti = AuthSessionService.generateJti();
            String accessToken = tokenProvider.generateToken(authentication, jti);
            Instant expiresAt = Instant.now().plusSeconds(SESSION_EXPIRY_SECONDS);
            authSessionService.createSession(authId, jti, expiresAt, ipAddress, userAgent);
            authIdentityService.recordSuccessfulLogin(authId);
            authKnownDeviceService.recordSuccessfulLogin(authId, ipAddress, userAgent);

            Long refreshExpirationMs = staySignedIn
                    ? authKnownDeviceService.staySignedInRefreshExpirationMs()
                    : null;
            String refreshToken = refreshExpirationMs != null
                    ? authRefreshTokenService.issueRefreshToken(
                            authId, ipAddress, userAgent, refreshExpirationMs)
                    : authRefreshTokenService.issueRefreshToken(authId, ipAddress, userAgent);
            logSuccessfulLogin(loginRequest.getUsername(), ipAddress);

            Long userId = resolveStaffUserIdIfAvailable(authId);
            List<String> roles = extractRoles(authentication.getAuthorities());
            List<String> permissions = extractPermissions(authentication.getAuthorities());

            JwtAuthenticationResponse response = buildAuthResponse(
                    accessToken, refreshToken, userId,
                    authPrincipal.getLoginIdentifier(), roles, permissions, false);
            response.setMfaSkippedTrustedDevice(trustedDeviceLogin);
            response.setStaySignedIn(staySignedIn);
            return response;

        } catch (BadCredentialsException e) {
            recordFailedStaffLogin(loginRequest.getUsername());
            logFailedLogin(loginRequest.getUsername(), ipAddress, userAgent, "invalid_credentials");
            log.error("BadCredentialsException during staff authentication", e);
            throw new UnauthorizedException("Invalid username or password");
        } catch (LockedException e) {
            logFailedLogin(loginRequest.getUsername(), ipAddress, userAgent, "account_locked");
            throw new UnauthorizedException("Account temporarily locked. Try again later.");
        } catch (AuthenticationException e) {
            recordFailedStaffLogin(loginRequest.getUsername());
            logFailedLogin(loginRequest.getUsername(), ipAddress, userAgent, "authentication_error");
            // Log the full exception with cause to help diagnose authentication failures
            Throwable cause = e.getCause();
            if (cause != null) {
                log.error("Staff authentication failed: error={}, cause={}",
                        e.getClass().getSimpleName(), 
                        cause.getClass().getSimpleName(), 
                        e);
            } else {
                log.error("Staff authentication failed: error={}", e.getClass().getSimpleName(), e);
            }
            throw new UnauthorizedException("Invalid username or password");
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

    @Transactional(noRollbackFor = {UnauthorizedException.class, StoryApiException.class})
    public JwtAuthenticationResponse verifyMfaLogin(String challengeToken, String code, String ipAddress) {
        return verifyMfaLogin(challengeToken, code, ipAddress, null, null, null, null);
    }

    public JwtAuthenticationResponse verifyMfaLogin(
            String challengeToken, String code, String ipAddress, String userAgent) {
        return verifyMfaLogin(challengeToken, code, ipAddress, userAgent, null, null, null);
    }

    public JwtAuthenticationResponse verifyMfaLogin(
            String challengeToken,
            String code,
            String ipAddress,
            String userAgent,
            com.smart.therapy.flow.auth.enums.MfaMethod method) {
        return verifyMfaLogin(challengeToken, code, ipAddress, userAgent, method, null, null);
    }

    @Transactional(noRollbackFor = {UnauthorizedException.class, StoryApiException.class})
    public JwtAuthenticationResponse verifyMfaLogin(
            String challengeToken,
            String code,
            String ipAddress,
            String userAgent,
            com.smart.therapy.flow.auth.enums.MfaMethod method,
            Boolean trustDevice,
            Boolean staySignedIn) {
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrg = TenantContext.getOrganisationId();
        try {
            MfaService.VerifiedChallenge challenge = mfaService.verifyLoginChallenge(
                    challengeToken, code, method);
            restoreTenantFromMfaChallenge(challenge);
            AuthIdentity identity = authIdentityService.getById(challenge.authId());
            if (identity == null || !Boolean.TRUE.equals(identity.getIsActive())) {
                throw new UnauthorizedException("User account is disabled");
            }
            var userDetails = authIdentityDetailsService.loadUserByAuthId(challenge.authId());
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            String jti = AuthSessionService.generateJti();
            String accessToken = tokenProvider.generateToken(authentication, jti);
            authSessionService.createSession(challenge.authId(), jti,
                    Instant.now().plusSeconds(SESSION_EXPIRY_SECONDS), ipAddress, userAgent);

            boolean trustRequested = Boolean.TRUE.equals(trustDevice);
            boolean stay = Boolean.TRUE.equals(staySignedIn);
            String deviceTrustToken = null;
            if (trustRequested) {
                deviceTrustToken = authKnownDeviceService.trustCurrentDevice(
                        challenge.authId(), ipAddress, userAgent);
                auditDeviceTrusted(challenge.authId(), identity.getLoginIdentifier(), ipAddress, userAgent);
            }

            Long refreshExpirationMs = stay
                    ? authKnownDeviceService.staySignedInRefreshExpirationMs()
                    : null;
            String refreshToken = refreshExpirationMs != null
                    ? authRefreshTokenService.issueRefreshToken(
                            challenge.authId(), ipAddress, userAgent, refreshExpirationMs)
                    : authRefreshTokenService.issueRefreshToken(
                            challenge.authId(), ipAddress, userAgent);
            authIdentityService.recordSuccessfulLogin(challenge.authId());
            authKnownDeviceService.recordSuccessfulLogin(challenge.authId(), ipAddress, userAgent);
            Long userId = resolveStaffUserIdIfAvailable(challenge.authId());
            JwtAuthenticationResponse response = buildAuthResponse(
                    accessToken, refreshToken, userId, identity.getLoginIdentifier(),
                    extractRoles(userDetails.getAuthorities()),
                    extractPermissions(userDetails.getAuthorities()), false);
            response.setMfaRequired(false);
            response.setDeviceTrustToken(deviceTrustToken);
            response.setStaySignedIn(stay);
            return response;
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
            if (previousSchema != null && !previousSchema.isBlank()) {
                TenantContext.setSchemaName(previousSchema);
            }
            if (previousOrg != null) {
                TenantContext.setOrganisationId(previousOrg);
            }
        }
    }

    private void restoreTenantFromMfaChallenge(MfaService.VerifiedChallenge challenge) {
        String schema = challenge.tenantSchema();
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            TenantContext.setSchemaName("public");
            TenantContext.setOrganisationId(null);
            return;
        }
        tenantDirectoryService.findBySchemaName(schema).ifPresentOrElse(info -> {
            if (challenge.organisationId() != null
                    && !challenge.organisationId().equals(info.getOrganisationId())) {
                throw new UnauthorizedException("MFA challenge organisation does not match tenant");
            }
            if (!info.isActive() || info.isForceDisabled()) {
                throw new TenantUnavailableException("Tenant is unavailable", null);
            }
            TenantContext.setSchemaName(info.getSchemaName());
            TenantContext.setOrganisationId(info.getOrganisationId());
        }, () -> {
            throw new UnauthorizedException("MFA challenge tenant is invalid");
        });
    }

    private AuthIdentity resolveStaffIdentityForLoginAttempt(String username) {
        String normalised = AuthIdentityService.normaliseLoginIdentifier(username);
        if (!StringUtils.hasText(normalised)) {
            return null;
        }
        Long organisationId = TenantContext.getOrganisationId();
        if (organisationId != null && !isPublicSchemaContext()) {
            List<AuthIdentity> candidates =
                    authIdentityRepository.findAllByNormalisedLoginIdentifierAndIdentityTypeForOrganisation(
                            normalised, IdentityType.STAFF, organisationId);
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
            if (candidates.size() > 1) {
                return null;
            }
            try {
                var details = authIdentityDetailsService.loadUserByUsername(normalised);
                if (details instanceof AuthPrincipal principal
                        && principal.getIdentityType() == IdentityType.STAFF) {
                    return authIdentityService.getById(principal.getAuthId());
                }
            } catch (UsernameNotFoundException ignored) {
                // Preserve anti-enumeration behavior; authentication will return the generic failure.
            }
            return null;
        }
        return authIdentityRepository.findAllByEmailOrUsername(normalised).stream()
                .filter(identity -> identity.getIdentityType() == IdentityType.STAFF)
                .filter(identity -> Boolean.TRUE.equals(identity.getIsActive()))
                .findFirst()
                .orElse(null);
    }

    private void recordFailedStaffLogin(String username) {
        AuthIdentity identity = resolveStaffIdentityForLoginAttempt(username);
        if (identity != null) {
            authIdentityService.recordFailedLogin(identity.getId());
        }
    }

    private void resolveTenantContextForLogin(LoginRequest loginRequest) {
        if (loginRequest == null) {
            return;
        }
        if (!isPublicSchemaContext()) {
            return;
        }

        var settings = platformTenantRoutingService.getSettings();
        if (settings == null) {
            return;
        }

        // Explicit org selection wins if provided.
        String selectedIdentifier = normalizeIdentifier(loginRequest);
        if (selectedIdentifier != null) {
            var info = tenantDirectoryService.findByOrganisationIdOrSlug(selectedIdentifier)
                    .orElseThrow(() -> new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_ORG_SELECTION", "Organisation selection not found"));
            if (StringUtils.hasText(loginRequest.getUsername())
                    && !tenantResolutionService.emailBelongsToOrganisation(
                            loginRequest.getUsername(), info.getOrganisationId(), IdentityType.STAFF)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_IN_ORG", "Email does not belong to organisation");
            }
            if (StringUtils.hasText(loginRequest.getUsername())
                    && tenantResolutionService.isEmailBlockedInOrganisation(loginRequest.getUsername(), info.getOrganisationId())) {
                throw new StoryApiException(HttpStatus.FORBIDDEN, "USER_BLOCKED_IN_ORG", "User is blocked for this organisation");
            }
            setTenantContext(info);
            return;
        }

        if (Boolean.TRUE.equals(settings.getEmailAutoRouting())) {
            List<com.smart.therapy.flow.organisation.entity.UserOrganisation> rows =
                    tenantResolutionService.resolveByEmail(loginRequest.getUsername(), IdentityType.STAFF);
            if (rows.isEmpty()) {
                if (isPlatformLoginCandidate(loginRequest.getUsername())) {
                    // Platform identities can authenticate in public context without tenant selection.
                    return;
                }
                throw new StoryApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "No organisation found for email");
            }
            if (rows.size() > 1) {
                throw new StoryApiException(HttpStatus.CONFLICT, "TENANT_SELECTION_REQUIRED",
                        "Multiple organisations found for email");
            }
            var org = rows.get(0).getOrganisation();
            if (tenantResolutionService.isEmailBlockedInOrganisation(loginRequest.getUsername(), org.getId())) {
                throw new StoryApiException(HttpStatus.FORBIDDEN, "USER_BLOCKED_IN_ORG", "User is blocked for this organisation");
            }
            tenantDirectoryService.findByOrganisationId(org.getId())
                    .ifPresentOrElse(this::setTenantContext,
                            () -> { throw new StoryApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Organisation not found"); });
        }
    }

    private static String normalizeIdentifier(LoginRequest loginRequest) {
        return normalizeIdentifier(
                loginRequest.getOrgId(),
                loginRequest.getOrgSlug(),
                loginRequest.getOrgIdentifier(),
                loginRequest.getOrgValue()
        );
    }

    private static String normalizeIdentifier(String orgId, String orgSlug, String orgIdentifier, String orgValue) {
        if (orgId != null && !orgId.isBlank()) {
            return orgId.trim();
        }
        if (orgSlug != null && !orgSlug.isBlank()) {
            return orgSlug.trim();
        }
        if (orgIdentifier != null && orgValue != null) {
            String idType = orgIdentifier.trim().toLowerCase();
            String idValue = orgValue.trim();
            if ("id".equals(idType) || "slug".equals(idType)) {
                return idValue;
            }
        }
        return null;
    }

    private void setTenantContext(TenantDirectoryService.TenantInfo info) {
        if (info == null) {
            return;
        }
        TenantContext.setSchemaName(info.getSchemaName());
        TenantContext.setOrganisationId(info.getOrganisationId());
    }

    private boolean isPlatformLoginCandidate(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        String normalised = username.trim().toLowerCase();
        return authIdentityRepository.findAllByEmailOrUsername(normalised).stream()
                .anyMatch(identity -> identity.getOrganisation() == null);
    }

    @Transactional
    public JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return refreshToken(refreshTokenRequest, null, null);
    }

    @Transactional
    public JwtAuthenticationResponse refreshToken(
            RefreshTokenRequest refreshTokenRequest,
            String ipAddress,
            String userAgent) {
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrg = TenantContext.getOrganisationId();
        String refreshToken = refreshTokenRequest.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        Long authId = tokenProvider.getAuthIdFromToken(refreshToken);
        AuthIdentity identity = authIdentityService.getById(authId);
        if (identity == null || !Boolean.TRUE.equals(identity.getIsActive())) {
            throw new UnauthorizedException("User account is disabled");
        }
        Date refreshIssuedAt = tokenProvider.getIssuedAtFromToken(refreshToken);
        if (identity.getPasswordChangedAt() != null
                && (refreshIssuedAt == null
                || refreshIssuedAt.toInstant().isBefore(
                        identity.getPasswordChangedAt().truncatedTo(ChronoUnit.SECONDS)))) {
            throw new UnauthorizedException("Refresh token predates the latest password change");
        }
        if (mfaService.isRefreshTokenStale(
                authId, refreshIssuedAt != null ? refreshIssuedAt.toInstant() : null)) {
            throw new UnauthorizedException("Refresh token predates MFA enrollment");
        }
        String tenantSchemaForClient = null;
        Long tenantOrgForClient = identity.getOrganisation() != null ? identity.getOrganisation().getId() : null;
        boolean publicSchemaContext = isPublicSchemaContext();
        try {
            if (identity.getIdentityType() == IdentityType.CLIENT) {
                if (publicSchemaContext) {
                    tenantSchemaForClient = tenantOrgForClient != null
                            ? tenantDirectoryService.findByOrganisationId(tenantOrgForClient)
                                    .map(TenantDirectoryService.TenantInfo::getSchemaName)
                                    .orElse(null)
                            : null;
                    if (!StringUtils.hasText(tenantSchemaForClient) || "public".equalsIgnoreCase(tenantSchemaForClient)) {
                        throw new PortalAccessDisabledException();
                    }
                    boolean hasPortalAccess = tenantTransactionExecutor.executeReadOnly(
                            tenantOrgForClient,
                            tenantSchemaForClient,
                            () -> {
                                Client client = clientRepository.findByAuthId(authId).orElse(null);
                                return client != null && portalSettingsService.hasPortalAccess(client.getId());
                            }
                    );
                    if (!hasPortalAccess) {
                        throw new PortalAccessDisabledException();
                    }
                    TenantContext.setSchemaName(tenantSchemaForClient);
                    TenantContext.setOrganisationId(tenantOrgForClient);
                } else {
                    Client client = clientRepository.findByAuthId(authId).orElse(null);
                    if (client == null || !portalSettingsService.hasPortalAccess(client.getId())) {
                        throw new PortalAccessDisabledException();
                    }
                }
            } else if (identity.getIdentityType() == IdentityType.STAFF && publicSchemaContext) {
                // Single-API-host: restore tenant from refresh-token claims so staff roles survive refresh.
                restoreStaffTenantFromRefreshToken(refreshToken);
            }
            Long userId = null;
            String loginIdentifierForResponse;
            if (!isPublicSchemaContext()) {
                userId = resolveStaffUserIdIfAvailable(authId);
                loginIdentifierForResponse = identity.getLoginIdentifier();
            } else {
                loginIdentifierForResponse = identity.getLoginIdentifier();
            }
            org.springframework.security.core.userdetails.UserDetails userDetails = authIdentityDetailsService.loadUserByAuthId(authId);
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            String jti = AuthSessionService.generateJti();
            String newAccessToken = tokenProvider.generateToken(auth, jti);
            Instant expiresAt = Instant.now().plusSeconds(SESSION_EXPIRY_SECONDS);
            authSessionService.createSession(authId, jti, expiresAt, ipAddress, userAgent);
            String newRefreshToken = authRefreshTokenService.rotateRefreshToken(
                    refreshToken, ipAddress, userAgent);
            List<String> roles = extractRoles(userDetails.getAuthorities());
            List<String> permissions = extractPermissions(userDetails.getAuthorities());
            return buildAuthResponse(
                    newAccessToken, newRefreshToken, userId,
                    loginIdentifierForResponse, roles, permissions, null);
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

    private boolean requiresPasswordChangeBeforeLogin(AuthIdentity identity) {
        return identity != null && Boolean.TRUE.equals(identity.getMustChangePassword());
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

    private JwtAuthenticationResponse buildAuthResponse(
            String accessToken,
            String refreshToken,
            Long userId,
            String loginIdentifier,
            List<String> roles,
            List<String> permissions,
            Boolean passwordChangeRequired
    ) {
        String tenantSchema = TenantContext.getSchemaName();
        if (tenantSchema == null || tenantSchema.isBlank()) {
            tenantSchema = "public";
        }
        Long organisationId = TenantContext.getOrganisationId();
        String organisationSlug = null;
        if (organisationId != null) {
            organisationSlug = tenantDirectoryService.findByOrganisationId(organisationId)
                    .map(TenantDirectoryService.TenantInfo::getSlug)
                    .orElse(null);
        }

        return JwtAuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(userId)
                .username(loginIdentifier)
                .email(loginIdentifier)
                .roles(roles)
                .permissions(permissions)
                .expiresIn(86400000L)
                .tenantSchema(tenantSchema)
                .organisationId(organisationId)
                .organisationSlug(organisationSlug)
                .passwordChangeRequired(Boolean.TRUE.equals(passwordChangeRequired))
                .build();
    }

    private void restoreStaffTenantFromRefreshToken(String refreshToken) {
        String tokenSchema = tokenProvider.getTenantSchemaFromToken(refreshToken);
        Long tokenOrgId = tokenProvider.getOrganisationIdFromToken(refreshToken);
        if (!StringUtils.hasText(tokenSchema) || "public".equalsIgnoreCase(tokenSchema)) {
            return;
        }
        tenantDirectoryService.findBySchemaName(tokenSchema).ifPresentOrElse(info -> {
            if (tokenOrgId != null && !tokenOrgId.equals(info.getOrganisationId())) {
                throw new UnauthorizedException("Refresh token organisation does not match tenant");
            }
            if (!info.isActive() || info.isForceDisabled()) {
                throw new TenantUnavailableException(
                        info.isForceDisabled() ? "Tenant has been disabled" : "Tenant is unavailable",
                        info.isForceDisabled() ? "TENANT_002" : null);
            }
            TenantContext.setSchemaName(info.getSchemaName());
            TenantContext.setOrganisationId(info.getOrganisationId());
        }, () -> {
            throw new UnauthorizedException("Refresh token tenant is invalid");
        });
    }

    /**
     * User/Client profile tables are tenant-scoped and may be absent or not yet migrated for a tenant.
     * Authentication should still succeed based on auth_identities + auth_identity_roles.
     */
    private Long resolveStaffUserIdIfAvailable(Long authId) {
        if (isPublicSchemaContext()) {
            return null;
        }
        if (!isUsersTablePresentInCurrentSchema()) {
            log.warn("Skipping optional staff user lookup for authId {} because table 'users' is not present in schema '{}'",
                    authId, TenantContext.getSchemaName());
            return null;
        }
        try {
            Long orgId = TenantContext.getOrganisationId();
            String schema = TenantContext.getSchemaName();
            return tenantTransactionExecutor.executeReadOnly(
                    orgId,
                    schema,
                    () -> userRepository.findByAuthId(authId).map(User::getId).orElse(null)
            );
        } catch (DataAccessException ex) {
            log.warn("Skipping optional staff user lookup for authId {} due to data-access issue: {}", authId, ex.getMessage());
            return null;
        } catch (RuntimeException ex) {
            log.warn("Skipping optional staff user lookup for authId {} due to runtime issue: {}", authId, ex.getMessage());
            return null;
        }
    }

    private boolean isUsersTablePresentInCurrentSchema() {
        try {
            Object result = entityManager.createNativeQuery(
                            "select to_regclass(current_schema() || '.users')")
                    .getSingleResult();
            return result != null;
        } catch (RuntimeException ex) {
            log.warn("Could not verify users table presence in current schema: {}", ex.getMessage());
            return false;
        }
    }

    @Transactional
    public void logout(String token, Long authId) {
        String jwtToken = token != null && token.startsWith("Bearer ") ? token.substring(7) : token;
        if (jwtToken != null && !jwtToken.isEmpty()) {
            try {
                tokenBlacklistService.blacklistToken(jwtToken);
                String jti = tokenProvider.getJtiFromToken(jwtToken);
                if (jti != null) authSessionService.revokeByJwtId(jti);
                log.info("Access token revoked for authId: {}", authId);
            } catch (Exception e) {
                log.error("Failed to blacklist/revoke access token for authId: {}", authId, e);
            }
        }
    }

    @Transactional
    public void logout(String accessToken, String refreshToken, Long authId) {
        String jwtAccess = accessToken != null && accessToken.startsWith("Bearer ") ? accessToken.substring(7) : accessToken;
        if (jwtAccess != null && !jwtAccess.isEmpty()) {
            try {
                tokenBlacklistService.blacklistToken(jwtAccess);
                String jti = tokenProvider.getJtiFromToken(jwtAccess);
                if (jti != null) authSessionService.revokeByJwtId(jti);
            } catch (Exception e) {
                log.error("Failed to blacklist/revoke access token for authId: {}", authId, e);
            }
        }
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                authRefreshTokenService.revokePresentedToken(refreshToken);
                tokenBlacklistService.blacklistToken(refreshToken);
            } catch (Exception e) {
                log.debug("Revoke refresh failed", e);
            }
        } else if (authId != null) {
            try {
                authRefreshTokenService.revokeAllForAuthId(authId);
            } catch (Exception e) {
                log.debug("Revoke refresh families failed", e);
            }
        }
        log.info("AuthId {} logged out", authId);
    }

    /**
     * Log successful login attempt in a separate transaction to avoid Hibernate collection loading issues.
     * Uses REQUIRES_NEW and clears the entity manager to ensure it doesn't interfere with the main authentication transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccessfulLogin(String username, String ipAddress) {
        try {
            LoginAttempt attempt = LoginAttempt.builder()
                    .username(username)
                    .ipAddress(ipAddress)
                    .success(true)
                    .build();
            // Timestamps are automatically set by @PrePersist callback in BaseEntity
            loginAttemptRepository.save(attempt);
        } catch (Exception e) {
            // Log but don't fail authentication if logging fails
            log.error("Failed to log successful login attempt", e);
        }
    }

    /**
     * Log failed login attempt in a separate transaction to avoid Hibernate collection loading issues.
     * Uses REQUIRES_NEW and clears the entity manager to ensure it doesn't interfere with the main authentication transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedLogin(String username, String ipAddress) {
        logFailedLogin(username, ipAddress, null, "invalid_credentials");
    }

    /**
     * Log failed login attempt (LoginAttempt row + HIPAA audit event) in a separate transaction to avoid
     * Hibernate collection loading issues. Audit logging is fail-soft: it never breaks authentication.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedLogin(String username, String ipAddress, String userAgent, String reason) {
        try {
            LoginAttempt attempt = LoginAttempt.builder()
                    .username(username)
                    .ipAddress(ipAddress)
                    .success(false)
                    .build();
            // Timestamps are automatically set by @PrePersist callback in BaseEntity
            loginAttemptRepository.save(attempt);
        } catch (Exception e) {
            // Log but don't fail authentication if logging fails
            log.error("Failed to log failed login attempt", e);
        }
        auditFailedLogin(username, ipAddress, userAgent, reason);
    }

    private void auditFailedLogin(String username, String ipAddress, String userAgent, String reason) {
        try {
            authAbuseMetrics.incrementFailedLogin();
            Long userId = resolveAuditUserIdForUsername(username);
            Map<String, Object> details = new HashMap<>();
            details.put("reason", reason != null ? reason : "invalid_credentials");
            auditLogService.logAuthEvent(userId, username, "login_failed", ipAddress, userAgent, "failure", details);
        } catch (Exception e) {
            log.warn("Failed to record login_failed audit event", e);
        }
    }

    private Long resolveAuditUserIdForUsername(String username) {
        try {
            AuthIdentity identity = resolveStaffIdentityForLoginAttempt(username);
            return identity != null ? resolveStaffUserIdIfAvailable(identity.getId()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public JwtAuthenticationResponse changePassword(
            ChangePasswordRequest request,
            AuthPrincipal principal,
            String ipAddress,
            String userAgent) {
        Objects.requireNonNull(request, "Request is required");
        Long authId = resolveAuthIdForPasswordChange(request, principal);
        var identity = authIdentityService.getById(authId);
        if (identity == null) throw new BadRequestException("Identity not found");

        boolean firstLoginPasswordChange = StringUtils.hasText(request.getChangePasswordToken());

        if (firstLoginPasswordChange) {
            if (!tokenProvider.validatePasswordChangeToken(request.getChangePasswordToken(), authId)) {
                throw new UnauthorizedException("Invalid or expired password change token");
            }
        } else {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new BadRequestException("Current password is required for regular password change");
            }
            if (!authIdentityService.validatePassword(identity, request.getCurrentPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        identity.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        identity.setPasswordChangedAt(Instant.now());
        identity.setMustChangePassword(false);
        authIdentityRepository.save(identity);
        tokenBlacklistService.blacklistUserTokens(authId);
        authKnownDeviceService.revokeAllTrustedDevices(authId);
        authRefreshTokenService.revokeAllForAuthId(authId);
        log.info("Password changed successfully for authId: {}", authId);

        if (firstLoginPasswordChange) {
            return continueLoginAfterRequiredPasswordChange(
                    authId, request.getChangePasswordToken(), ipAddress, userAgent);
        }

        return JwtAuthenticationResponse.builder()
                .message("Password changed successfully. Please login again.")
                .statusSuccess(true)
                .passwordChangeRequired(false)
                .build();
    }

    /**
     * After forced password change on first login, continue into MFA enrollment or full session.
     * Avoids forcing the user to sign in again with the new password.
     */
    private JwtAuthenticationResponse continueLoginAfterRequiredPasswordChange(
            Long authId,
            String passwordChangeToken,
            String ipAddress,
            String userAgent) {
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrg = TenantContext.getOrganisationId();
        try {
            restoreTenantFromAuthToken(passwordChangeToken);

            AuthIdentity identity = authIdentityService.getById(authId);
            if (identity == null || !Boolean.TRUE.equals(identity.getIsActive())) {
                throw new UnauthorizedException("User account is disabled");
            }

            var userDetails = authIdentityDetailsService.loadUserByAuthId(authId);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            boolean mfaEnabled = mfaService.isEnabled(authId);
            boolean mfaEnrollmentRequired =
                    mfaService.isEnrollmentRequired(userDetails.getAuthorities(), mfaEnabled);

            if (mfaEnabled) {
                String challengeToken = mfaService.createLoginChallenge(identity);
                MfaDtos.LoginChallengeInfo challengeInfo = mfaService.getLoginChallengeInfo(authId);
                return JwtAuthenticationResponse.builder()
                        .username(identity.getLoginIdentifier())
                        .email(identity.getLoginIdentifier())
                        .roles(extractRoles(userDetails.getAuthorities()))
                        .permissions(extractPermissions(userDetails.getAuthorities()))
                        .mfaRequired(true)
                        .mfaChallengeToken(challengeToken)
                        .mfaEnrollmentRequired(false)
                        .mfaMethod(challengeInfo.method().name())
                        .mfaMaskedDestination(challengeInfo.maskedDestination())
                        .mfaMethods(challengeInfo.enrolledMethods())
                        .mfaSmsAvailable(challengeInfo.smsAvailable())
                        .mfaEmailAvailable(challengeInfo.emailAvailable())
                        .message("MFA verification is required.")
                        .statusSuccess(true)
                        .build();
            }

            if (mfaEnrollmentRequired) {
                MfaDtos.LoginChallengeInfo challengeInfo = mfaService.getLoginChallengeInfo(authId);
                return JwtAuthenticationResponse.builder()
                        .username(identity.getLoginIdentifier())
                        .email(identity.getLoginIdentifier())
                        .roles(extractRoles(userDetails.getAuthorities()))
                        .permissions(extractPermissions(userDetails.getAuthorities()))
                        .mfaRequired(false)
                        .mfaEnrollmentRequired(true)
                        .mfaChallengeToken(mfaService.createEnrollmentChallenge(authId))
                        .mfaMethods(List.of())
                        .mfaSmsAvailable(challengeInfo.smsAvailable())
                        .mfaEmailAvailable(challengeInfo.emailAvailable())
                        .message("MFA enrollment is required before login.")
                        .statusSuccess(true)
                        .build();
            }

            String jti = AuthSessionService.generateJti();
            String accessToken = tokenProvider.generateToken(authentication, jti);
            authSessionService.createSession(authId, jti,
                    Instant.now().plusSeconds(SESSION_EXPIRY_SECONDS), ipAddress, userAgent);
            authIdentityService.recordSuccessfulLogin(authId);
            authKnownDeviceService.recordSuccessfulLogin(authId, ipAddress, userAgent);
            String refreshToken =
                    authRefreshTokenService.issueRefreshToken(authId, ipAddress, userAgent);
            Long userId = resolveStaffUserIdIfAvailable(authId);
            JwtAuthenticationResponse response = buildAuthResponse(
                    accessToken,
                    refreshToken,
                    userId,
                    identity.getLoginIdentifier(),
                    extractRoles(userDetails.getAuthorities()),
                    extractPermissions(userDetails.getAuthorities()),
                    false);
            response.setStatusSuccess(true);
            response.setMessage("Password changed successfully.");
            return response;
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
            if (previousSchema != null && !previousSchema.isBlank()) {
                TenantContext.setSchemaName(previousSchema);
            }
            if (previousOrg != null) {
                TenantContext.setOrganisationId(previousOrg);
            }
        }
    }

    /**
     * Completes required (pre-login) MFA enrollment and issues a full staff session so the user
     * does not need to sign in again after verifying the MFA code.
     */
    @Transactional(noRollbackFor = {UnauthorizedException.class, StoryApiException.class})
    public JwtAuthenticationResponse completeRequiredMfaEnrollment(
            String enrollmentToken,
            String code,
            String ipAddress,
            String userAgent,
            Boolean trustDevice,
            Boolean staySignedIn) {
        String previousSchema = TenantContext.getSchemaName();
        Long previousOrg = TenantContext.getOrganisationId();
        try {
            restoreTenantFromAuthToken(enrollmentToken);
            MfaDtos.ConfirmResponse confirm =
                    mfaService.confirmRequiredEnrollment(enrollmentToken, code);

            Long authId = tokenProvider.getAuthIdFromToken(enrollmentToken);
            AuthIdentity identity = authIdentityService.getById(authId);
            if (identity == null || !Boolean.TRUE.equals(identity.getIsActive())) {
                throw new UnauthorizedException("User account is disabled");
            }
            if (requiresPasswordChangeBeforeLogin(identity)) {
                throw new UnauthorizedException("Password change is still required");
            }

            var userDetails = authIdentityDetailsService.loadUserByAuthId(authId);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            String jti = AuthSessionService.generateJti();
            String accessToken = tokenProvider.generateToken(authentication, jti);
            authSessionService.createSession(authId, jti,
                    Instant.now().plusSeconds(SESSION_EXPIRY_SECONDS), ipAddress, userAgent);

            boolean stay = Boolean.TRUE.equals(staySignedIn);
            String deviceTrustToken = null;
            if (Boolean.TRUE.equals(trustDevice)) {
                deviceTrustToken = authKnownDeviceService.trustCurrentDevice(
                        authId, ipAddress, userAgent);
                auditDeviceTrusted(authId, identity.getLoginIdentifier(), ipAddress, userAgent);
            }

            Long refreshExpirationMs = stay
                    ? authKnownDeviceService.staySignedInRefreshExpirationMs()
                    : null;
            String refreshToken = refreshExpirationMs != null
                    ? authRefreshTokenService.issueRefreshToken(
                            authId, ipAddress, userAgent, refreshExpirationMs)
                    : authRefreshTokenService.issueRefreshToken(authId, ipAddress, userAgent);

            authIdentityService.recordSuccessfulLogin(authId);
            authKnownDeviceService.recordSuccessfulLogin(authId, ipAddress, userAgent);
            Long userId = resolveStaffUserIdIfAvailable(authId);

            JwtAuthenticationResponse response = buildAuthResponse(
                    accessToken,
                    refreshToken,
                    userId,
                    identity.getLoginIdentifier(),
                    extractRoles(userDetails.getAuthorities()),
                    extractPermissions(userDetails.getAuthorities()),
                    false);
            response.setMfaRequired(false);
            response.setMfaEnrollmentRequired(false);
            response.setDeviceTrustToken(deviceTrustToken);
            response.setStaySignedIn(stay);
            response.setMfaRecoveryCodes(confirm.recoveryCodes());
            response.setEnrolledMethods(confirm.enrolledMethods());
            response.setCanAddMoreMethods(confirm.canAddMoreMethods());
            response.setStatusSuccess(true);
            response.setMessage("MFA enrollment completed.");
            return response;
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
            if (previousSchema != null && !previousSchema.isBlank()) {
                TenantContext.setSchemaName(previousSchema);
            }
            if (previousOrg != null) {
                TenantContext.setOrganisationId(previousOrg);
            }
        }
    }

    private void restoreTenantFromAuthToken(String token) {
        String schema = tokenProvider.getTenantSchemaFromToken(token);
        Long orgId = tokenProvider.getOrganisationIdFromToken(token);
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            TenantContext.setSchemaName("public");
            TenantContext.setOrganisationId(null);
            return;
        }
        tenantDirectoryService.findBySchemaName(schema).ifPresentOrElse(info -> {
            if (orgId != null && !orgId.equals(info.getOrganisationId())) {
                throw new UnauthorizedException("Token organisation does not match tenant");
            }
            if (!info.isActive() || info.isForceDisabled()) {
                throw new TenantUnavailableException("Tenant is unavailable", null);
            }
            TenantContext.setSchemaName(info.getSchemaName());
            TenantContext.setOrganisationId(info.getOrganisationId());
        }, () -> {
            throw new UnauthorizedException("Token tenant is invalid");
        });
    }

    private Long resolveAuthIdForPasswordChange(ChangePasswordRequest request, AuthPrincipal principal) {
        if (principal != null) {
            return principal.getAuthId();
        }
        if (!StringUtils.hasText(request.getChangePasswordToken())) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!tokenProvider.validateToken(request.getChangePasswordToken())) {
            throw new UnauthorizedException("Invalid or expired password change token");
        }
        String tokenType = tokenProvider.getTokenType(request.getChangePasswordToken());
        if (!"password_change".equals(tokenType)) {
            throw new UnauthorizedException("Invalid password change token");
        }
        return tokenProvider.getAuthIdFromToken(request.getChangePasswordToken());
    }

    /**
     * Initiate password reset for staff (admin/therapist/supervisor). Sends email with reset link if the account exists.
     * Always returns success to avoid revealing whether the email is registered.
     */
    @Transactional
    public String requestPasswordResetStaff(String email) {
        return requestPasswordResetStaff(email, null, null, null, null);
    }

    /**
     * Initiate password reset for staff with optional org selection for multi-org routing.
     *
     * @return optional auth hint for the frontend (e.g. {@code TENANT_SELECTION_REQUIRED}); body stays
     *         anti-enumeration success either way.
     */
    @Transactional
    public String requestPasswordResetStaff(
            String email,
            String orgId,
            String orgSlug,
            String orgIdentifier,
            String orgValue
    ) {
        String normalised = email != null ? email.toLowerCase().trim() : "";
        if (normalised.isEmpty()) {
            return null;
        }

        try {
            boolean orgSelectionProvided = StringUtils.hasText(orgId)
                    || StringUtils.hasText(orgSlug)
                    || (StringUtils.hasText(orgIdentifier) && StringUtils.hasText(orgValue));

            Long targetOrganisationId;
            if (orgSelectionProvided || !isPublicSchemaContext()) {
                OrgResolutionResult orgResult = resolveTargetOrganisationIdForForgotPasswordStrict(
                        orgId, orgSlug, orgIdentifier, orgValue, orgSelectionProvided);
                if (orgResult.invalidSelection()) {
                    log.warn("Forgot-password request used an invalid organisation selection");
                    return null;
                }
                targetOrganisationId = orgResult.organisationId();
                if (targetOrganisationId != null
                        && !tenantResolutionService.emailBelongsToOrganisation(normalised, targetOrganisationId)) {
                    log.debug("Forgot-password identifier does not belong to organisationId={}",
                            targetOrganisationId);
                    return null;
                }
            } else {
                targetOrganisationId = null;
            }

            ForgotPasswordIdentityResult identityResult =
                    resolveStaffIdentityForForgotPasswordWithHint(normalised, targetOrganisationId);
            if (identityResult.hint() != null) {
                return identityResult.hint();
            }
            AuthIdentity identity = identityResult.identity();
            if (identity == null) {
                return null;
            }

            String token = authIdentityService.generateSecureToken();
            Instant expiry = Instant.now().plusSeconds(TimeUnit.HOURS.toSeconds(PASSWORD_RESET_TOKEN_VALIDITY_HOURS));
            authIdentityService.setPasswordResetToken(identity.getId(), token, expiry);

            String to = identity.getLoginIdentifier();
            String name = identity.getLoginIdentifier();
            User user = resolveStaffUserProfileForForgotPassword(identity.getId(), targetOrganisationId);
            if (user != null) {
                if (StringUtils.hasText(user.getEmail())) {
                    to = user.getEmail();
                }
                if (StringUtils.hasText(user.getFullName())) {
                    name = user.getFullName();
                }
            }

            emailService.sendStaffPasswordResetEmail(to, name, token);
            log.info("Staff password reset email sent for authId: {}", identity.getId());
            return null;
        } catch (Exception ex) {
            // Preserve anti-enumeration behavior: endpoint should always return success.
            log.error("Forgot-password processing failed: type={}",
                    ex.getClass().getSimpleName(), ex);
            return null;
        }
    }

    private OrgResolutionResult resolveTargetOrganisationIdForForgotPasswordStrict(
            String orgId,
            String orgSlug,
            String orgIdentifier,
            String orgValue,
            boolean orgSelectionProvided
    ) {
        if (!isPublicSchemaContext()) {
            return new OrgResolutionResult(TenantContext.getOrganisationId(), false);
        }
        if (!orgSelectionProvided) {
            return new OrgResolutionResult(null, false);
        }
        String selectedIdentifier = normalizeIdentifier(orgId, orgSlug, orgIdentifier, orgValue);
        if (!StringUtils.hasText(selectedIdentifier)) {
            return new OrgResolutionResult(null, true);
        }
        return tenantDirectoryService.findByOrganisationIdOrSlug(selectedIdentifier)
                .map(info -> new OrgResolutionResult(info.getOrganisationId(), false))
                .orElseGet(() -> new OrgResolutionResult(null, true));
    }

    private ForgotPasswordIdentityResult resolveStaffIdentityForForgotPasswordWithHint(
            String normalisedEmail, Long organisationId) {
        List<AuthIdentity> candidates = organisationId != null
                ? authIdentityRepository.findAllByNormalisedLoginIdentifierAndIdentityTypeForOrganisation(
                        normalisedEmail, IdentityType.STAFF, organisationId)
                : authIdentityRepository.findAllByNormalisedLoginIdentifierAndIdentityType(
                        normalisedEmail, IdentityType.STAFF);

        List<AuthIdentity> activeCandidates = candidates.stream()
                .filter(identity -> Boolean.TRUE.equals(identity.getIsActive()))
                .toList();

        // When username != profile email (common after username edits / multi-org provisioning),
        // resolve the STAFF identity via user_organisations + tenant users.email for the selected org.
        if (activeCandidates.isEmpty() && organisationId != null) {
            AuthIdentity viaMembership = resolveStaffIdentityViaOrgMembership(normalisedEmail, organisationId);
            if (viaMembership != null) {
                activeCandidates = List.of(viaMembership);
            }
        }

        if (activeCandidates.isEmpty()) {
            log.debug("Forgot-password requested for an unknown, non-staff, or inactive identifier");
            return new ForgotPasswordIdentityResult(null, null);
        }

        if (activeCandidates.size() > 1) {
            if (organisationId == null) {
                log.warn("Forgot-password identifier matched {} STAFF identities; organisation selection required",
                        activeCandidates.size());
                return new ForgotPasswordIdentityResult(null, "TENANT_SELECTION_REQUIRED");
            }
            log.error("Forgot-password identifier matched {} STAFF identities in orgId={}",
                    activeCandidates.size(), organisationId);
            return new ForgotPasswordIdentityResult(null, null);
        }

        return new ForgotPasswordIdentityResult(activeCandidates.get(0), null);
    }

    /**
     * Resolve STAFF identity for an org when the login identifier is not the profile email
     * (e.g. username was changed, or a second-org identity uses a non-email login_identifier).
     */
    private AuthIdentity resolveStaffIdentityViaOrgMembership(String normalisedEmail, Long organisationId) {
        return tenantResolutionService.resolveByEmail(normalisedEmail, IdentityType.STAFF).stream()
                .filter(row -> row.getOrganisation() != null
                        && organisationId.equals(row.getOrganisation().getId()))
                .map(com.smart.therapy.flow.organisation.entity.UserOrganisation::getAuth)
                .filter(identity -> identity != null
                        && Boolean.TRUE.equals(identity.getIsActive())
                        && identity.getIdentityType() == IdentityType.STAFF)
                .findFirst()
                .orElseGet(() -> resolveStaffIdentityViaTenantUserEmail(normalisedEmail, organisationId));
    }

    private AuthIdentity resolveStaffIdentityViaTenantUserEmail(String normalisedEmail, Long organisationId) {
        String schema = tenantDirectoryService.findByOrganisationId(organisationId)
                .map(TenantDirectoryService.TenantInfo::getSchemaName)
                .orElse(null);
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            return null;
        }
        try {
            Long authId = tenantTransactionExecutor.executeReadOnly(
                    organisationId,
                    schema,
                    () -> userRepository.findByEmail(normalisedEmail)
                            .map(user -> user.getAuthIdentity() != null ? user.getAuthIdentity().getId() : null)
                            .orElse(null)
            );
            if (authId == null) {
                return null;
            }
            AuthIdentity identity = authIdentityRepository.findById(authId).orElse(null);
            if (identity == null
                    || !Boolean.TRUE.equals(identity.getIsActive())
                    || identity.getIdentityType() != IdentityType.STAFF) {
                return null;
            }
            return identity;
        } catch (Exception ex) {
            log.debug("Forgot-password tenant identifier fallback failed for orgId={} type={}",
                    organisationId, ex.getClass().getSimpleName());
            return null;
        }
    }

    private record OrgResolutionResult(Long organisationId, boolean invalidSelection) {
    }

    private record ForgotPasswordIdentityResult(AuthIdentity identity, String hint) {
    }

    private User resolveStaffUserProfileForForgotPassword(Long authId, Long organisationId) {
        Long effectiveOrganisationId = organisationId != null
                ? organisationId
                : TenantContext.getOrganisationId();
        if (effectiveOrganisationId == null) {
            return null;
        }

        String schema = tenantDirectoryService.findByOrganisationId(effectiveOrganisationId)
                .map(TenantDirectoryService.TenantInfo::getSchemaName)
                .orElse(null);
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            return null;
        }

        try {
            return tenantTransactionExecutor.executeReadOnly(
                    effectiveOrganisationId,
                    schema,
                    () -> userRepository.findByAuthId(authId).orElse(null)
            );
        } catch (Exception ex) {
            log.warn("Staff profile lookup unavailable for forgot-password authId={} orgId={}: {}",
                    authId, effectiveOrganisationId, ex.getMessage());
            return null;
        }
    }

    /**
     * Validate a password-reset token from email (used by set-new-password pages on load).
     */
    @Transactional(readOnly = true)
    public PasswordResetTokenValidationResponse validatePasswordResetToken(String token) {
        AuthIdentity identity = authIdentityService.validatePasswordResetToken(token);
        if (identity == null) {
            return PasswordResetTokenValidationResponse.builder()
                    .valid(false)
                    .message("Invalid or expired reset token")
                    .build();
        }
        String accountType = identity.getIdentityType() == IdentityType.CLIENT ? "client" : "staff";
        return PasswordResetTokenValidationResponse.builder()
                .valid(true)
                .accountType(accountType)
                .build();
    }

    /**
     * Reset staff password using the token from the forgot-password email. Token must belong to a STAFF identity.
     */
    @Transactional
    public void resetPasswordStaff(String token, String newPassword) {
        String normalizedToken = AuthIdentityService.normalizeResetToken(token);
        AuthIdentity identity = authIdentityService.validatePasswordResetToken(normalizedToken);
        if (identity == null) {
            throw new UnauthorizedException("Invalid or expired reset token");
        }
        if (identity.getIdentityType() == IdentityType.CLIENT) {
            throw new UnauthorizedException(
                    "This reset link is for the client portal. Use /auth/set-new-password or POST /api/v1/portal/reset-password.");
        }
        if (identity.getIdentityType() != IdentityType.STAFF) {
            throw new UnauthorizedException("Invalid or expired reset token");
        }
        AuthIdentity updated = authIdentityService.resetPasswordByToken(normalizedToken, newPassword);
        if (updated == null || !authIdentityService.validatePassword(updated, newPassword)) {
            throw new BadRequestException("Failed to save new password. Please request a new reset link.");
        }
        tokenBlacklistService.blacklistAllUserTokens(identity.getId());
        authKnownDeviceService.revokeAllTrustedDevices(identity.getId());
        authRefreshTokenService.revokeAllForAuthId(identity.getId());
        log.info("Staff password reset completed for authId: {}", identity.getId());
    }

    private void auditTrustedDeviceMfaSkip(Long authId, String username, String ipAddress, String userAgent) {
        try {
            Long userId = resolveStaffUserIdIfAvailable(authId);
            Map<String, Object> details = new HashMap<>();
            details.put("reason", "trusted_device");
            auditLogService.logAuthEvent(
                    userId, username, "mfa_skipped_trusted_device", ipAddress, userAgent, "success", details);
        } catch (Exception e) {
            log.warn("Failed to audit trusted-device MFA skip for authId {}: {}", authId, e.getMessage());
        }
    }

    private void auditDeviceTrusted(Long authId, String username, String ipAddress, String userAgent) {
        try {
            Long userId = resolveStaffUserIdIfAvailable(authId);
            Map<String, Object> details = new HashMap<>();
            details.put("trustDays", authKnownDeviceService.getTrustDays());
            auditLogService.logAuthEvent(
                    userId, username, "device_trusted", ipAddress, userAgent, "success", details);
        } catch (Exception e) {
            log.warn("Failed to audit device trust for authId {}: {}", authId, e.getMessage());
        }
    }
}

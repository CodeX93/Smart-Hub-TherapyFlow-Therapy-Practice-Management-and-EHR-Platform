package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthProvider;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unified identity: password validation, lockout, token generation,
 * and tenant-scoped email/username uniqueness.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthIdentityService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 1800;
    private static final String TOKEN_HASH_PREFIX = "sha256:";

    private final AuthIdentityRepository authIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganisationRepository organisationRepository;
    private final AuditLogService auditLogService;

    public static String normaliseLoginIdentifier(String loginIdentifier) {
        return loginIdentifier != null ? loginIdentifier.toLowerCase().trim() : "";
    }

    /** Trim and URL-decode opaque tokens (activation / password-reset) from email links. */
    public static String normalizeOpaqueToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "";
        }
        String trimmed = token.trim();
        try {
            return URLDecoder.decode(trimmed, StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException ex) {
            return trimmed;
        }
    }

    public static String normalizeResetToken(String token) {
        return normalizeOpaqueToken(token);
    }

    @Transactional(readOnly = true)
    public AuthIdentity getById(Long authId) {
        return authIdentityRepository.findById(authId).orElse(null);
    }

    @Transactional(readOnly = true)
    public AuthIdentity getByNormalisedLogin(String normalised) {
        List<AuthIdentity> matches = authIdentityRepository.findAllByEmailOrUsername(normalised);
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null) {
            return matches.stream()
                    .filter(a -> a.getOrganisation() != null && orgId.equals(a.getOrganisation().getId()))
                    .findFirst()
                    .orElse(null);
        }
        return matches.stream()
                .filter(a -> a.getOrganisation() == null)
                .findFirst()
                .orElse(matches.get(0));
    }

    /**
     * Update staff username (and display login_identifier alias). Uniqueness is per organisation.
     */
    @Transactional
    public AuthIdentity updateLoginIdentifier(AuthIdentity identity, String newLoginIdentifier) {
        return updateUsername(identity, newLoginIdentifier);
    }

    @Transactional
    public AuthIdentity updateUsername(AuthIdentity identity, String newUsername) {
        if (identity == null || !StringUtils.hasText(newUsername)) {
            return identity;
        }
        if (identity.getIdentityType() == IdentityType.CLIENT) {
            throw new BadRequestException("Clients cannot have a username");
        }
        String trimmed = newUsername.trim();
        String normalised = normaliseLoginIdentifier(trimmed);
        if (normalised.equals(identity.getNormalisedUsername())) {
            return identity;
        }
        assertUsernameAvailable(identity.getOrganisation(), normalised, identity.getId());
        identity.setUsername(trimmed);
        identity.setNormalisedUsername(normalised);
        syncLoginAlias(identity);
        identity.setLoginIdentifierChangedAt(Instant.now());
        return authIdentityRepository.save(identity);
    }

    @Transactional
    public AuthIdentity updateEmail(AuthIdentity identity, String newEmail) {
        if (identity == null || !StringUtils.hasText(newEmail)) {
            return identity;
        }
        String trimmed = newEmail.trim();
        String normalised = normaliseLoginIdentifier(trimmed);
        if (normalised.equals(identity.getNormalisedEmail())) {
            return identity;
        }
        assertEmailAvailable(identity.getOrganisation(), normalised, identity.getId());
        identity.setEmail(trimmed);
        identity.setNormalisedEmail(normalised);
        if (identity.getIdentityType() == IdentityType.CLIENT) {
            syncLoginAlias(identity);
        }
        return authIdentityRepository.save(identity);
    }

    public boolean validatePassword(AuthIdentity identity, String rawPassword) {
        if (identity == null || rawPassword == null) return false;
        return passwordEncoder.matches(rawPassword, identity.getPasswordHash());
    }

    @Transactional
    public void recordSuccessfulLogin(Long authId) {
        AuthIdentity identity = getById(authId);
        if (identity == null) return;
        identity.setFailedLoginAttempts(0);
        identity.setLastSuccessfulLogin(Instant.now());
        identity.setAccountLocked(false);
        identity.setLockedUntil(null);
        identity.setLockedReason(null);
        authIdentityRepository.save(identity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(Long authId) {
        AuthIdentity identity = authIdentityRepository.findByIdForUpdate(authId).orElse(null);
        if (identity == null) return;
        int attempts = (identity.getFailedLoginAttempts() != null ? identity.getFailedLoginAttempts() : 0) + 1;
        identity.setFailedLoginAttempts(attempts);
        identity.setLastFailedLogin(Instant.now());
        boolean justLocked = false;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            identity.setAccountLocked(true);
            identity.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_SECONDS));
            identity.setLockedReason("Too many failed login attempts");
            justLocked = true;
        }
        authIdentityRepository.save(identity);
        if (justLocked) {
            auditAccountLocked(identity, attempts);
        }
    }

    /**
     * Fail-soft audit of an account lockout triggered by repeated failed logins.
     * Never allowed to break the caller's authentication flow.
     */
    private void auditAccountLocked(AuthIdentity identity, int failedAttempts) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("failedAttempts", failedAttempts);
            details.put("identityType", identity.getIdentityType() != null ? identity.getIdentityType().name() : null);
            details.put("lockDurationSeconds", LOCK_DURATION_SECONDS);
            auditLogService.logAuthEvent(null, identity.getLoginIdentifier(), "account_locked",
                    null, null, "blocked", details);
        } catch (Exception e) {
            log.warn("Failed to record account_locked audit event for authId: {}", identity.getId(), e);
        }
    }

    public boolean isLocked(AuthIdentity identity) {
        if (identity == null || !Boolean.TRUE.equals(identity.getAccountLocked())) return false;
        if (identity.getLockedUntil() != null && Instant.now().isAfter(identity.getLockedUntil())) {
            return false;
        }
        return true;
    }

    /**
     * Create a tenant or platform identity.
     * For STAFF: {@code loginIdentifier} is treated as username; pass email separately via
     * {@link #createStaffIdentity}. For CLIENT: {@code loginIdentifier} is the portal email.
     */
    @Transactional
    public AuthIdentity createIdentity(String loginIdentifier, String rawPassword, IdentityType identityType, Long createdBy) {
        if (identityType == IdentityType.CLIENT) {
            return createClientIdentity(loginIdentifier, rawPassword, createdBy);
        }
        // STAFF legacy: username = loginIdentifier, email = loginIdentifier when email-shaped
        String username = loginIdentifier;
        String email = loginIdentifier != null && loginIdentifier.contains("@") ? loginIdentifier : null;
        if (email == null) {
            Long orgId = TenantContext.getOrganisationId();
            email = normaliseLoginIdentifier(username) + "@org-" + (orgId != null ? orgId : "platform") + ".local";
        }
        return createStaffIdentity(username, email, rawPassword, createdBy);
    }

    @Transactional
    public AuthIdentity createStaffIdentity(String username, String email, String rawPassword, Long createdBy) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(email)) {
            throw new BadRequestException("Username and email are required");
        }
        String normalisedUsername = normaliseLoginIdentifier(username);
        String normalisedEmail = normaliseLoginIdentifier(email);
        Organisation organisation = resolveOrganisationFromContext();
        assertUsernameAvailable(organisation, normalisedUsername, null);
        assertEmailAvailable(organisation, normalisedEmail, null);

        AuthIdentity identity = AuthIdentity.builder()
                .organisation(organisation)
                .username(username.trim())
                .normalisedUsername(normalisedUsername)
                .email(email.trim())
                .normalisedEmail(normalisedEmail)
                .loginIdentifier(username.trim())
                .normalisedLoginIdentifier(normalisedUsername)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .authProvider(AuthProvider.LOCAL)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .createdBy(createdBy)
                .build();
        return authIdentityRepository.save(identity);
    }

    @Transactional
    public AuthIdentity createClientIdentity(String portalEmail, String rawPassword, Long createdBy) {
        if (!StringUtils.hasText(portalEmail)) {
            throw new BadRequestException("Portal email is required");
        }
        String normalisedEmail = normaliseLoginIdentifier(portalEmail);
        Organisation organisation = resolveOrganisationFromContext();
        if (organisation == null) {
            throw new BadRequestException("Organisation context is required to create a client identity");
        }
        assertEmailAvailable(organisation, normalisedEmail, null);

        AuthIdentity identity = AuthIdentity.builder()
                .organisation(organisation)
                .email(portalEmail.trim())
                .normalisedEmail(normalisedEmail)
                .username(null)
                .normalisedUsername(null)
                .loginIdentifier(portalEmail.trim())
                .normalisedLoginIdentifier(normalisedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .identityType(IdentityType.CLIENT)
                .isActive(true)
                .authProvider(AuthProvider.LOCAL)
                .failedLoginAttempts(0)
                .accountLocked(false)
                .createdBy(createdBy)
                .build();
        return authIdentityRepository.save(identity);
    }

    public void assertEmailAvailable(Organisation organisation, String normalisedEmail, Long excludeAuthId) {
        if (!StringUtils.hasText(normalisedEmail)) {
            return;
        }
        boolean taken;
        if (organisation == null || organisation.getId() == null) {
            taken = authIdentityRepository.existsPlatformByNormalisedEmail(normalisedEmail, excludeAuthId);
        } else {
            taken = authIdentityRepository.existsByOrganisationIdAndNormalisedEmail(
                    organisation.getId(), normalisedEmail, excludeAuthId);
        }
        if (taken) {
            throw new BadRequestException("Email already in use");
        }
    }

    public void assertUsernameAvailable(Organisation organisation, String normalisedUsername, Long excludeAuthId) {
        if (!StringUtils.hasText(normalisedUsername)) {
            return;
        }
        boolean taken;
        if (organisation == null || organisation.getId() == null) {
            taken = authIdentityRepository.existsPlatformByNormalisedUsername(normalisedUsername, excludeAuthId);
        } else {
            taken = authIdentityRepository.existsByOrganisationIdAndNormalisedUsername(
                    organisation.getId(), normalisedUsername, excludeAuthId);
        }
        if (taken) {
            throw new BadRequestException("Username already in use");
        }
    }

    private Organisation resolveOrganisationFromContext() {
        Long organisationId = TenantContext.getOrganisationId();
        if (organisationId == null) {
            return null;
        }
        return organisationRepository.findById(organisationId).orElse(null);
    }

    private static void syncLoginAlias(AuthIdentity identity) {
        if (identity.getIdentityType() == IdentityType.CLIENT) {
            String email = identity.getEmail();
            identity.setLoginIdentifier(email);
            identity.setNormalisedLoginIdentifier(normaliseLoginIdentifier(email));
        } else {
            String username = identity.getUsername();
            identity.setLoginIdentifier(username);
            identity.setNormalisedLoginIdentifier(normaliseLoginIdentifier(username));
        }
    }

    @Transactional
    public void setPasswordResetToken(Long authId, String token, Instant expiry) {
        if (authId == null) {
            return;
        }
        authIdentityRepository.updatePasswordResetToken(
                authId, hashOpaqueToken(normalizeResetToken(token)), expiry);
    }

    @Transactional
    public void clearPasswordResetToken(Long authId) {
        if (authId == null) {
            return;
        }
        authIdentityRepository.clearPasswordResetTokenById(authId);
    }

    public String generatePasswordResetTokenValue() {
        return generateSecureToken();
    }

    @Transactional
    public void updatePassword(Long authId, String rawNewPassword) {
        AuthIdentity identity = getById(authId);
        if (identity != null) {
            identity.setPasswordHash(passwordEncoder.encode(rawNewPassword));
            identity.setPasswordChangedAt(Instant.now());
            identity.setMustChangePassword(false);
            authIdentityRepository.save(identity);
        }
    }

    @Transactional
    public void setEmailVerificationToken(Long authId, String token, Instant expiry) {
        if (authId == null) {
            return;
        }
        authIdentityRepository.updateEmailVerificationToken(
                authId, hashOpaqueToken(normalizeOpaqueToken(token)), expiry);
    }

    @Transactional
    public void setEmailVerified(Long authId, boolean verified) {
        AuthIdentity identity = getById(authId);
        if (identity != null) {
            identity.setEmailVerified(verified);
            authIdentityRepository.save(identity);
        }
    }

    @Transactional
    public void setActive(Long authId, boolean active) {
        AuthIdentity identity = getById(authId);
        if (identity != null) {
            identity.setIsActive(active);
            authIdentityRepository.save(identity);
        }
    }

    @Transactional(readOnly = true)
    public AuthIdentity findByEmailVerificationToken(String token) {
        String normalized = normalizeOpaqueToken(token);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return authIdentityRepository.findByEmailVerificationToken(hashOpaqueToken(normalized))
                .or(() -> authIdentityRepository.findByEmailVerificationToken(normalized))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public AuthIdentity validateEmailVerificationToken(String token) {
        AuthIdentity identity = findByEmailVerificationToken(token);
        if (identity == null || !isEmailVerificationTokenValid(identity)) {
            return null;
        }
        return identity;
    }

    public boolean isEmailVerificationTokenValid(AuthIdentity identity) {
        if (identity == null || identity.getEmailVerificationToken() == null) return false;
        if (identity.getEmailVerificationExpiry() == null) return false;
        return Instant.now().isBefore(identity.getEmailVerificationExpiry());
    }

    @Transactional
    public AuthIdentity activateByEmailToken(String token) {
        AuthIdentity identity = findByEmailVerificationToken(token);
        if (identity == null) return null;
        if (!isEmailVerificationTokenValid(identity)) return null;
        identity.setEmailVerified(true);
        identity.setEmailVerificationToken(null);
        identity.setEmailVerificationExpiry(null);
        return authIdentityRepository.save(identity);
    }

    @Transactional
    public AuthIdentity activateClientPortalAccount(String token, String rawNewPassword) {
        AuthIdentity identity = findByEmailVerificationToken(token);
        if (identity == null) {
            return null;
        }
        if (!isEmailVerificationTokenValid(identity)) {
            return null;
        }
        identity.setEmailVerified(true);
        identity.setEmailVerificationToken(null);
        identity.setEmailVerificationExpiry(null);
        identity.setPasswordHash(passwordEncoder.encode(rawNewPassword));
        identity.setPasswordChangedAt(Instant.now());
        identity.setMustChangePassword(false);
        identity.setFailedLoginAttempts(0);
        identity.setAccountLocked(false);
        identity.setLockedUntil(null);
        identity.setLockedReason(null);
        return authIdentityRepository.saveAndFlush(identity);
    }

    @Transactional(readOnly = true)
    public AuthIdentity findByPasswordResetToken(String token) {
        String normalized = normalizeResetToken(token);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return authIdentityRepository.findByPasswordResetToken(hashOpaqueToken(normalized))
                .or(() -> authIdentityRepository.findByPasswordResetToken(normalized))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public AuthIdentity validatePasswordResetToken(String token) {
        AuthIdentity identity = findByPasswordResetToken(token);
        if (identity == null || !isPasswordResetTokenValid(identity)) {
            return null;
        }
        return identity;
    }

    public boolean isPasswordResetTokenValid(AuthIdentity identity) {
        if (identity == null || identity.getPasswordResetToken() == null) return false;
        if (identity.getPasswordResetExpiry() == null) return false;
        return Instant.now().isBefore(identity.getPasswordResetExpiry());
    }

    @Transactional
    public AuthIdentity resetPasswordByToken(String token, String rawNewPassword) {
        AuthIdentity identity = findByPasswordResetToken(token);
        if (identity == null) return null;
        if (!isPasswordResetTokenValid(identity)) return null;
        identity.setPasswordHash(passwordEncoder.encode(rawNewPassword));
        identity.setPasswordChangedAt(Instant.now());
        identity.setMustChangePassword(false);
        identity.setPasswordResetToken(null);
        identity.setPasswordResetExpiry(null);
        identity.setFailedLoginAttempts(0);
        identity.setAccountLocked(false);
        identity.setLockedUntil(null);
        identity.setLockedReason(null);
        return authIdentityRepository.saveAndFlush(identity);
    }

    public String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    static String hashOpaqueToken(String normalizedToken) {
        if (!StringUtils.hasText(normalizedToken)) {
            return "";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizedToken.getBytes(StandardCharsets.UTF_8));
            return TOKEN_HASH_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}

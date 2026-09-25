package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.metrics.AuthAbuseMetrics;
import com.smart.therapy.flow.auth.dto.MfaDtos;
import com.smart.therapy.flow.auth.enums.MfaMethod;
import com.smart.therapy.flow.auth.entity.*;
import com.smart.therapy.flow.auth.repository.*;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import com.smart.therapy.flow.common.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MfaService {
    public enum EnforcementMode { OPTIONAL, PRIVILEGED, ALL }

    private final AuthMfaCredentialRepository credentialRepository;
    private final AuthMfaRecoveryCodeRepository recoveryCodeRepository;
    private final AuthMfaLoginChallengeRepository challengeRepository;
    private final AuthIdentityRepository identityRepository;
    private final TotpService totpService;
    private final EncryptionService encryptionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthSessionService authSessionService;
    private final MfaOtpService otpService;
    private final MfaOtpDeliveryService otpDeliveryService;
    private final AuditLogService auditLogService;
    private final AuthAbuseMetrics authAbuseMetrics;
    private final AuthKnownDeviceService authKnownDeviceService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.mfa.enforcement:OPTIONAL}")
    private EnforcementMode enforcementMode = EnforcementMode.OPTIONAL;
    @Value("${app.auth.mfa.challenge-validity-seconds:300}")
    private long challengeValiditySeconds = 300;
    @Value("${app.auth.mfa.max-attempts:10}")
    private int maxAttempts = 10;
    @Value("${app.auth.mfa.lockout-seconds:300}")
    private long lockoutSeconds = 300;
    @Value("${app.auth.mfa.recovery-code-count:10}")
    private int recoveryCodeCount = 10;
    @Value("${app.auth.mfa.issuer:TherapyFlow}")
    private String issuer = "TherapyFlow";
    @Value("${app.auth.mfa.step-up-validity-seconds:900}")
    private long stepUpValiditySeconds = 900;

    @Transactional(readOnly = true)
    public MfaMethod getMethod(Long authId) {
        return credentialRepository.findByAuthIdentityId(authId)
                .map(c -> c.getMfaMethod() != null ? c.getMfaMethod() : MfaMethod.TOTP)
                .orElse(MfaMethod.TOTP);
    }

    @Transactional(readOnly = true)
    public MfaDtos.LoginChallengeInfo getLoginChallengeInfo(Long authId) {
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityId(authId).orElse(null);
        if (credential == null || !Boolean.TRUE.equals(credential.getEnabled())) {
            return new MfaDtos.LoginChallengeInfo(
                    MfaMethod.TOTP,
                    null,
                    otpDeliveryService.isChannelAvailable(MfaMethod.SMS),
                    otpDeliveryService.isChannelAvailable(MfaMethod.EMAIL),
                    List.of());
        }
        ensureMethodFlags(credential);
        AuthIdentity identity = identityRepository.findById(authId).orElse(null);
        List<String> enrolled = enrolledMethodNames(credential);
        MfaMethod preferred = resolvePreferredMethod(credential, enrolled);
        String masked = identity != null ? maskedDestination(credential, identity, preferred) : null;
        return new MfaDtos.LoginChallengeInfo(
                preferred,
                masked,
                otpDeliveryService.isChannelAvailable(MfaMethod.SMS),
                otpDeliveryService.isChannelAvailable(MfaMethod.EMAIL),
                enrolled);
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(Long authId) {
        return credentialRepository.findByAuthIdentityId(authId)
                .map(c -> Boolean.TRUE.equals(c.getEnabled()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isRefreshTokenStale(Long authId, Instant issuedAt) {
        return credentialRepository.findByAuthIdentityId(authId)
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()) && c.getConfirmedAt() != null)
                .map(c -> issuedAt == null || issuedAt.isBefore(c.getConfirmedAt()))
                .orElse(false);
    }

    public boolean isEnrollmentRequired(Collection<? extends GrantedAuthority> authorities, boolean enabled) {
        if (enabled || enforcementMode == EnforcementMode.OPTIONAL) {
            return false;
        }
        if (enforcementMode == EnforcementMode.ALL) {
            return true;
        }
        return authorities != null && authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .anyMatch(a -> a.startsWith("ROLE_")
                        && (a.contains("ADMIN") || a.startsWith("ROLE_PLATFORM_")));
    }

    public String createEnrollmentChallenge(Long authId) {
        return tokenProvider.generateMfaEnrollmentToken(authId, challengeValiditySeconds);
    }

    @Transactional
    public MfaDtos.EnrollmentStartResponse startRequiredEnrollment(
            String enrollmentToken,
            MfaMethod method,
            String phone) {
        Long authId = validateEnrollmentToken(enrollmentToken);
        AuthIdentity identity = identityRepository.findById(authId)
                .orElseThrow(() -> new UnauthorizedException("Identity not found"));
        return startEnrollment(authId, identity.getLoginIdentifier(), method, phone, enrollmentToken);
    }

    @Transactional
    public MfaDtos.ConfirmResponse confirmRequiredEnrollment(String enrollmentToken, String code) {
        return confirmEnrollment(validateEnrollmentToken(enrollmentToken), code, enrollmentToken);
    }

    private Long validateEnrollmentToken(String token) {
        if (!tokenProvider.isMfaEnrollmentToken(token)) {
            throw new UnauthorizedException("Invalid or expired MFA enrollment challenge");
        }
        return tokenProvider.getAuthIdFromToken(token);
    }

    @Transactional
    public String createLoginChallenge(AuthIdentity identity) {
        String token = tokenProvider.generateMfaChallengeToken(identity.getId(), challengeValiditySeconds);
        String jti = tokenProvider.getJtiFromToken(token);
        AuthMfaLoginChallenge challenge = AuthMfaLoginChallenge.builder()
                .authIdentity(identity)
                .jtiHash(sha256(jti))
                .expiresAt(tokenProvider.getExpirationDateFromToken(token).toInstant())
                .build();
        challengeRepository.save(challenge);
        credentialRepository.findByAuthIdentityId(identity.getId()).ifPresent(credential -> {
            if (!Boolean.TRUE.equals(credential.getEnabled())) {
                return;
            }
            ensureMethodFlags(credential);
            List<String> enrolled = enrolledMethodNames(credential);
            // Auto-send only when exactly one OTP method is enrolled (no picker needed).
            if (enrolled.size() == 1) {
                MfaMethod only = MfaMethod.valueOf(enrolled.get(0));
                if (only == MfaMethod.SMS || only == MfaMethod.EMAIL) {
                    String destination = destinationForCredential(credential, identity, only);
                    otpService.issueAndSendOtp(
                            jti,
                            identity.getId(),
                            AuthMfaOtpChallenge.Purpose.LOGIN,
                            only,
                            destination);
                }
            }
        });
        return token;
    }

    @Transactional
    public MfaDtos.SendLoginCodeResponse sendLoginCode(String challengeToken) {
        return sendLoginCode(challengeToken, null);
    }

    @Transactional
    public MfaDtos.SendLoginCodeResponse sendLoginCode(String challengeToken, MfaMethod requestedMethod) {
        if (!tokenProvider.isMfaChallengeToken(challengeToken)) {
            throw new UnauthorizedException("Invalid or expired MFA challenge");
        }
        Long authId = tokenProvider.getAuthIdFromToken(challengeToken);
        String jti = tokenProvider.getJtiFromToken(challengeToken);
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityId(authId)
                .orElseThrow(() -> new UnauthorizedException("MFA is not enabled"));
        if (!Boolean.TRUE.equals(credential.getEnabled())) {
            throw new UnauthorizedException("MFA is not enabled");
        }
        ensureMethodFlags(credential);
        MfaMethod method = resolveLoginMethod(credential, requestedMethod);
        if (method != MfaMethod.SMS && method != MfaMethod.EMAIL) {
            throw new BadRequestException("Authenticator app codes cannot be resent");
        }
        AuthIdentity identity = identityRepository.findById(authId)
                .orElseThrow(() -> new UnauthorizedException("Identity not found"));
        String masked = otpService.issueAndSendOtp(
                jti,
                authId,
                AuthMfaOtpChallenge.Purpose.LOGIN,
                method,
                destinationForCredential(credential, identity, method));
        return new MfaDtos.SendLoginCodeResponse(true, method.name(), masked);
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = {UnauthorizedException.class, StoryApiException.class}
    )
    public VerifiedChallenge verifyLoginChallenge(String token, String code) {
        if (!tokenProvider.isMfaChallengeToken(token)) {
            throw new UnauthorizedException("Invalid or expired MFA challenge");
        }
        Long authId = tokenProvider.getAuthIdFromToken(token);
        AuthMfaLoginChallenge challenge = challengeRepository.findByJtiHash(sha256(tokenProvider.getJtiFromToken(token)))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired MFA challenge"));
        Instant now = Instant.now();
        if (!authId.equals(challenge.getAuthIdentity().getId())
                || challenge.getConsumedAt() != null
                || !challenge.getExpiresAt().isAfter(now)) {
            throw new UnauthorizedException("Invalid or expired MFA challenge");
        }
        verifyCodeLocked(authId, code, now, tokenProvider.getJtiFromToken(token), null);
        challenge.setConsumedAt(now);
        challengeRepository.save(challenge);
        return new VerifiedChallenge(authId, tokenProvider.getTenantSchemaFromToken(token),
                tokenProvider.getOrganisationIdFromToken(token));
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = {UnauthorizedException.class, StoryApiException.class}
    )
    public VerifiedChallenge verifyLoginChallenge(String token, String code, MfaMethod requestedMethod) {
        if (!tokenProvider.isMfaChallengeToken(token)) {
            throw new UnauthorizedException("Invalid or expired MFA challenge");
        }
        Long authId = tokenProvider.getAuthIdFromToken(token);
        AuthMfaLoginChallenge challenge = challengeRepository.findByJtiHash(sha256(tokenProvider.getJtiFromToken(token)))
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired MFA challenge"));
        Instant now = Instant.now();
        if (!authId.equals(challenge.getAuthIdentity().getId())
                || challenge.getConsumedAt() != null
                || !challenge.getExpiresAt().isAfter(now)) {
            throw new UnauthorizedException("Invalid or expired MFA challenge");
        }
        verifyCodeLocked(authId, code, now, tokenProvider.getJtiFromToken(token), requestedMethod);
        challenge.setConsumedAt(now);
        challengeRepository.save(challenge);
        return new VerifiedChallenge(authId, tokenProvider.getTenantSchemaFromToken(token),
                tokenProvider.getOrganisationIdFromToken(token));
    }

    @Transactional(readOnly = true)
    public MfaDtos.StatusResponse status(Long authId, Collection<? extends GrantedAuthority> authorities) {
        Optional<AuthMfaCredential> credentialOpt = credentialRepository.findByAuthIdentityId(authId);
        AuthMfaCredential credential = credentialOpt.orElse(null);
        if (credential != null) {
            ensureMethodFlags(credential);
        }
        boolean enabled = credential != null && Boolean.TRUE.equals(credential.getEnabled());
        int unused = credential != null
                ? recoveryCodeRepository.findByCredentialIdAndUsedAtIsNull(credential.getId()).size()
                : 0;
        List<String> enrolled = credential != null ? enrolledMethodNames(credential) : List.of();
        MfaMethod preferred = credential != null
                ? resolvePreferredMethod(credential, enrolled)
                : MfaMethod.TOTP;
        String masked = credential != null
                ? identityRepository.findById(authId)
                        .map(i -> maskedDestination(credential, i, preferred)).orElse(null)
                : null;
        return new MfaDtos.StatusResponse(
                enabled,
                isEnrollmentRequired(authorities, enabled),
                enforcementMode.name(),
                unused,
                preferred.name(),
                masked,
                enrolled,
                otpDeliveryService.isChannelAvailable(MfaMethod.SMS),
                otpDeliveryService.isChannelAvailable(MfaMethod.EMAIL));
    }

    @Transactional
    public MfaDtos.EnrollmentStartResponse startEnrollment(Long authId, String loginIdentifier) {
        return startEnrollment(authId, loginIdentifier, MfaMethod.TOTP, null, null);
    }

    @Transactional
    public MfaDtos.EnrollmentStartResponse startEnrollment(
            Long authId,
            String loginIdentifier,
            MfaMethod requestedMethod,
            String phone,
            String enrollmentToken) {
        AuthIdentity identity = identityRepository.findById(authId)
                .orElseThrow(() -> new UnauthorizedException("Identity not found"));
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityIdForUpdate(authId)
                .orElseGet(() -> AuthMfaCredential.builder().authIdentity(identity).enabled(false).build());
        ensureMethodFlags(credential);
        boolean alreadyEnabled = Boolean.TRUE.equals(credential.getEnabled());
        // Allow adding another method during required enrollment (token present) or when not yet enabled.
        if (alreadyEnabled && !StringUtils.hasText(enrollmentToken)) {
            throw new BadRequestException("MFA is already enabled. Use Security settings to add a method.");
        }
        MfaMethod method = otpService.resolveEnrollmentMethod(requestedMethod, phone, identity);
        if (alreadyEnabled && isMethodEnabled(credential, method)) {
            throw new BadRequestException(method.name() + " verification is already set up");
        }

        credential.setPendingMfaMethod(method);
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setEncryptedPendingSecret(null);
        credential.setPendingPhoneE164(null);

        if (method == MfaMethod.TOTP) {
            String secret = totpService.generateSecret();
            credential.setEncryptedPendingSecret(encryptionService.encrypt(secret));
            credentialRepository.save(credential);
            String label = issuer + ":" + loginIdentifier;
            String uri = "otpauth://totp/" + UriUtils.encodePathSegment(label, StandardCharsets.UTF_8)
                    + "?secret=" + secret
                    + "&issuer=" + UriUtils.encodeQueryParam(issuer, StandardCharsets.UTF_8)
                    + "&algorithm=SHA1&digits=6&period=30";
            return enrollmentStart(MfaMethod.TOTP, secret, uri, null, false, credential);
        }

        String destination;
        String jti = StringUtils.hasText(enrollmentToken)
                ? tokenProvider.getJtiFromToken(enrollmentToken)
                : otpService.enrollmentJtiForAuthId(authId);
        if (method == MfaMethod.SMS) {
            destination = otpService.resolveSmsDestination(phone, identity);
            credential.setPendingPhoneE164(
                    com.smart.therapy.flow.common.util.PhoneNormalizationUtil.normalizePhoneE164(destination));
        } else {
            destination = otpService.resolveEmailDestination(identity);
        }
        credentialRepository.save(credential);
        String masked = otpService.issueAndSendOtp(
                jti, authId, AuthMfaOtpChallenge.Purpose.ENROLLMENT, method, destination);
        return enrollmentStart(method, null, null, masked, true, credential);
    }

    @Transactional
    public MfaDtos.ConfirmResponse confirmEnrollment(Long authId, String code) {
        return confirmEnrollment(authId, code, null);
    }

    @Transactional
    public MfaDtos.ConfirmResponse confirmEnrollment(Long authId, String code, String enrollmentToken) {
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityIdForUpdate(authId)
                .orElseThrow(() -> new BadRequestException("MFA enrollment has not been started"));
        ensureMethodFlags(credential);
        Instant now = Instant.now();
        MfaMethod method = credential.getPendingMfaMethod() != null
                ? credential.getPendingMfaMethod()
                : (credential.getMfaMethod() != null ? credential.getMfaMethod() : MfaMethod.TOTP);
        boolean firstEnable = !Boolean.TRUE.equals(credential.getEnabled());

        if (method == MfaMethod.TOTP) {
            if (credential.getEncryptedPendingSecret() == null) {
                throw new BadRequestException("MFA enrollment has not been started");
            }
            String secret = encryptionService.decrypt(credential.getEncryptedPendingSecret());
            Long step = totpService.verify(secret, code, now, 1, null);
            if (step == null) {
                throw new UnauthorizedException("Invalid verification code");
            }
            credential.setEncryptedSecret(credential.getEncryptedPendingSecret());
            credential.setLastAcceptedTimeStep(step);
        } else {
            String jti = StringUtils.hasText(enrollmentToken)
                    ? tokenProvider.getJtiFromToken(enrollmentToken)
                    : otpService.enrollmentJtiForAuthId(authId);
            if (!otpService.verifyOtp(jti, AuthMfaOtpChallenge.Purpose.ENROLLMENT, code, now)) {
                throw new UnauthorizedException("Invalid verification code");
            }
            if (method == MfaMethod.SMS) {
                if (StringUtils.hasText(credential.getPendingPhoneE164())) {
                    credential.setPhoneE164(credential.getPendingPhoneE164());
                }
            }
        }

        enableMethodFlag(credential, method);
        credential.setMfaMethod(method);
        credential.setPendingMfaMethod(null);
        credential.setPendingPhoneE164(null);
        credential.setEncryptedPendingSecret(null);
        credential.setEnabled(true);
        credential.setConfirmedAt(now);
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credentialRepository.save(credential);

        List<String> codes = List.of();
        if (firstEnable) {
            codes = replaceRecoveryCodes(credential);
            authSessionService.revokeAllForAuthId(authId);
        }
        List<String> enrolled = enrolledMethodNames(credential);
        return new MfaDtos.ConfirmResponse(true, codes, enrolled, canAddMoreMethods(credential));
    }

    @Transactional
    public MfaDtos.SendSettingsCodeResponse sendSettingsCode(Long authId) {
        AuthIdentity identity = identityRepository.findById(authId)
                .orElseThrow(() -> new UnauthorizedException("Identity not found"));
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityId(authId)
                .orElseThrow(() -> new BadRequestException("MFA is not enabled"));
        if (!Boolean.TRUE.equals(credential.getEnabled())) {
            throw new BadRequestException("MFA is not enabled");
        }
        ensureMethodFlags(credential);
        MfaMethod method = resolvePreferredMethod(credential, enrolledMethodNames(credential));
        if (method != MfaMethod.SMS && method != MfaMethod.EMAIL) {
            // Prefer any enrolled OTP channel for settings verification.
            if (Boolean.TRUE.equals(credential.getSmsEnabled())) {
                method = MfaMethod.SMS;
            } else if (Boolean.TRUE.equals(credential.getEmailEnabled())) {
                method = MfaMethod.EMAIL;
            } else {
                throw new BadRequestException("Authenticator app codes cannot be resent. Enter the code from your app.");
            }
        }
        String masked = otpService.issueAndSendOtp(
                otpService.settingsJtiForAuthId(authId),
                authId,
                AuthMfaOtpChallenge.Purpose.SETTINGS,
                method,
                destinationForCredential(credential, identity, method));
        return new MfaDtos.SendSettingsCodeResponse(true, method.name(), masked);
    }

    /**
     * Verifies a current MFA code, then starts adding/updating a method.
     * Existing enrolled methods stay active until confirm (and remain after).
     */
    @Transactional(noRollbackFor = {UnauthorizedException.class, StoryApiException.class})
    public MfaDtos.EnrollmentStartResponse startChange(
            Long authId,
            String currentCode,
            MfaMethod requestedMethod,
            String phone) {
        AuthIdentity identity = identityRepository.findById(authId)
                .orElseThrow(() -> new UnauthorizedException("Identity not found"));
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityIdForUpdate(authId)
                .orElseThrow(() -> new BadRequestException("MFA is not enabled"));
        if (!Boolean.TRUE.equals(credential.getEnabled())) {
            throw new BadRequestException("MFA is not enabled");
        }
        ensureMethodFlags(credential);
        verifyCodeLocked(authId, currentCode, Instant.now());

        MfaMethod newMethod = otpService.resolveEnrollmentMethod(requestedMethod, phone, identity);
        credential.setPendingMfaMethod(newMethod);
        credential.setEncryptedPendingSecret(null);
        credential.setPendingPhoneE164(null);
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);

        if (newMethod == MfaMethod.TOTP) {
            String secret = totpService.generateSecret();
            credential.setEncryptedPendingSecret(encryptionService.encrypt(secret));
            credentialRepository.save(credential);
            String label = issuer + ":" + identity.getLoginIdentifier();
            String uri = "otpauth://totp/" + UriUtils.encodePathSegment(label, StandardCharsets.UTF_8)
                    + "?secret=" + secret
                    + "&issuer=" + UriUtils.encodeQueryParam(issuer, StandardCharsets.UTF_8)
                    + "&algorithm=SHA1&digits=6&period=30";
            return enrollmentStart(MfaMethod.TOTP, secret, uri, null, false, credential);
        }

        String destination;
        if (newMethod == MfaMethod.SMS) {
            destination = otpService.resolveSmsDestination(phone, identity);
            credential.setPendingPhoneE164(
                    com.smart.therapy.flow.common.util.PhoneNormalizationUtil.normalizePhoneE164(destination));
        } else {
            destination = otpService.resolveEmailDestination(identity);
        }
        credentialRepository.save(credential);
        String masked = otpService.issueAndSendOtp(
                otpService.changeEnrollmentJtiForAuthId(authId),
                authId,
                AuthMfaOtpChallenge.Purpose.ENROLLMENT,
                newMethod,
                destination);
        return enrollmentStart(newMethod, null, null, masked, true, credential);
    }

    @Transactional(noRollbackFor = {UnauthorizedException.class, StoryApiException.class})
    public MfaDtos.ConfirmResponse confirmChange(Long authId, String code, String keepJwtId) {
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityIdForUpdate(authId)
                .orElseThrow(() -> new BadRequestException("MFA change has not been started"));
        if (!Boolean.TRUE.equals(credential.getEnabled())) {
            throw new BadRequestException("MFA is not enabled");
        }
        MfaMethod pending = credential.getPendingMfaMethod();
        if (pending == null) {
            throw new BadRequestException("MFA change has not been started");
        }
        Instant now = Instant.now();
        boolean accepted = false;
        if (pending == MfaMethod.TOTP) {
            if (credential.getEncryptedPendingSecret() == null) {
                throw new BadRequestException("MFA change has not been started");
            }
            String secret = encryptionService.decrypt(credential.getEncryptedPendingSecret());
            Long step = totpService.verify(secret, code, now, 1, null);
            if (step != null) {
                accepted = true;
                credential.setEncryptedSecret(credential.getEncryptedPendingSecret());
                credential.setLastAcceptedTimeStep(step);
            }
        } else {
            accepted = otpService.verifyOtp(
                    otpService.changeEnrollmentJtiForAuthId(authId),
                    AuthMfaOtpChallenge.Purpose.ENROLLMENT,
                    code,
                    now);
            if (accepted) {
                if (pending == MfaMethod.SMS && StringUtils.hasText(credential.getPendingPhoneE164())) {
                    credential.setPhoneE164(credential.getPendingPhoneE164());
                }
            }
        }
        if (!accepted) {
            int attempts = Optional.ofNullable(credential.getFailedAttempts()).orElse(0) + 1;
            credential.setFailedAttempts(attempts);
            if (attempts >= maxAttempts) {
                credential.setLockedUntil(now.plusSeconds(lockoutSeconds));
                credential.setFailedAttempts(0);
            }
            credentialRepository.save(credential);
            throw new UnauthorizedException("Invalid verification code");
        }

        enableMethodFlag(credential, pending);
        credential.setMfaMethod(pending);
        credential.setPendingMfaMethod(null);
        credential.setPendingPhoneE164(null);
        credential.setEncryptedPendingSecret(null);
        credential.setConfirmedAt(now);
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credentialRepository.save(credential);
        List<String> enrolled = enrolledMethodNames(credential);
        if (StringUtils.hasText(keepJwtId)) {
            authSessionService.revokeAllExceptJwtId(authId, keepJwtId);
        } else {
            authSessionService.revokeAllForAuthId(authId);
        }
        authKnownDeviceService.revokeAllTrustedDevices(authId);
        return new MfaDtos.ConfirmResponse(true, List.of(), enrolled, canAddMoreMethods(credential));
    }

    private static String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        try {
            return com.smart.therapy.flow.common.util.PhoneNormalizationUtil.normalizePhoneE164(phone);
        } catch (Exception e) {
            return phone.trim();
        }
    }

    @Transactional(noRollbackFor = {UnauthorizedException.class, StoryApiException.class})
    public void disable(Long authId, String code) {
        if (enforcementMode != EnforcementMode.OPTIONAL) {
            throw new BadRequestException(
                    "MFA cannot be disabled while enforcement is " + enforcementMode.name());
        }
        verifyCodeLocked(authId, code, Instant.now());
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityIdForUpdate(authId)
                .orElseThrow(() -> new BadRequestException("MFA is not enabled"));
        credential.setEnabled(false);
        credential.setEncryptedSecret(null);
        credential.setEncryptedPendingSecret(null);
        credential.setLastAcceptedTimeStep(null);
        credential.setMfaMethod(MfaMethod.TOTP);
        credential.setPhoneE164(null);
        credential.setTotpEnabled(false);
        credential.setSmsEnabled(false);
        credential.setEmailEnabled(false);
        credential.setPendingMfaMethod(null);
        credential.setPendingPhoneE164(null);
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        recoveryCodeRepository.deleteByCredentialId(credential.getId());
        credentialRepository.save(credential);
        authSessionService.revokeAllForAuthId(authId);
        authKnownDeviceService.revokeAllTrustedDevices(authId);
    }

    /**
     * Step-up MFA for sensitive actions while already authenticated (e.g. after trusted-device login).
     * Returns a short-lived step-up token the client can present to privileged flows.
     */
    @Transactional(noRollbackFor = {UnauthorizedException.class, StoryApiException.class})
    public MfaDtos.StepUpResponse createStepUpToken(Long authId, String code) {
        if (!isEnabled(authId)) {
            throw new BadRequestException("MFA is not enabled");
        }
        verifyCodeLocked(authId, code, Instant.now());
        String token = tokenProvider.generateStepUpToken(authId, stepUpValiditySeconds);
        return new MfaDtos.StepUpResponse(token, stepUpValiditySeconds);
    }

    @Transactional(readOnly = true)
    public boolean isValidStepUpToken(Long authId, String stepUpToken) {
        return tokenProvider.validateStepUpToken(stepUpToken, authId);
    }

    @Transactional(noRollbackFor = {UnauthorizedException.class, StoryApiException.class})
    public MfaDtos.RecoveryCodesResponse regenerateRecoveryCodes(Long authId, String code) {
        verifyCodeLocked(authId, code, Instant.now());
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityIdForUpdate(authId)
                .orElseThrow(() -> new BadRequestException("MFA is not enabled"));
        return new MfaDtos.RecoveryCodesResponse(replaceRecoveryCodes(credential));
    }

    private void verifyCodeLocked(Long authId, String code, Instant now) {
        verifyCodeLocked(authId, code, now, null, null);
    }

    private void verifyCodeLocked(Long authId, String code, Instant now, String jti) {
        verifyCodeLocked(authId, code, now, jti, null);
    }

    private void verifyCodeLocked(
            Long authId, String code, Instant now, String jti, MfaMethod requestedMethod) {
        AuthMfaCredential credential = credentialRepository.findByAuthIdentityIdForUpdate(authId)
                .orElseThrow(() -> new UnauthorizedException("MFA is not enabled"));
        if (!Boolean.TRUE.equals(credential.getEnabled())) {
            throw new UnauthorizedException("MFA is not enabled");
        }
        ensureMethodFlags(credential);
        MfaMethod method = resolveLoginMethod(credential, requestedMethod);
        List<String> enrolled = enrolledMethodNames(credential);
        if (enrolled.isEmpty()) {
            throw new UnauthorizedException("MFA is not enabled");
        }
        if (method == MfaMethod.TOTP
                && requestedMethod == MfaMethod.TOTP
                && credential.getEncryptedSecret() == null) {
            throw new UnauthorizedException("MFA is not enabled");
        }
        if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(now)) {
            long retryAfterSeconds = Math.max(1L,
                    java.time.Duration.between(now, credential.getLockedUntil()).getSeconds());
            throw new StoryApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "MFA_TEMPORARILY_LOCKED",
                    formatMfaLockMessage(retryAfterSeconds),
                    Map.of("retryAfterSeconds", retryAfterSeconds),
                    (int) Math.min(retryAfterSeconds, Integer.MAX_VALUE));
        }

        boolean accepted = false;
        if (code != null && code.matches("\\d{6}")) {
            List<MfaMethod> candidates = requestedMethod != null
                    ? List.of(method)
                    : enrolled.stream().map(MfaMethod::valueOf).toList();
            for (MfaMethod candidate : candidates) {
                if (candidate == MfaMethod.TOTP) {
                    if (credential.getEncryptedSecret() == null) {
                        continue;
                    }
                    String secret = encryptionService.decrypt(credential.getEncryptedSecret());
                    // Settings/change checks (no login jti) must accept the current window even if
                    // the same step was already used at sign-in — otherwise "Add method" fails
                    // immediately after login with the same authenticator code.
                    Long lastStep = StringUtils.hasText(jti) ? credential.getLastAcceptedTimeStep() : null;
                    Long step = totpService.verify(secret, code, now, 1, lastStep);
                    if (step != null) {
                        if (StringUtils.hasText(jti)) {
                            credential.setLastAcceptedTimeStep(step);
                        }
                        accepted = true;
                        break;
                    }
                } else if (StringUtils.hasText(jti)) {
                    if (otpService.verifyOtp(jti, AuthMfaOtpChallenge.Purpose.LOGIN, code, now)) {
                        accepted = true;
                        break;
                    }
                } else if (otpService.verifyOtp(
                        otpService.settingsJtiForAuthId(authId),
                        AuthMfaOtpChallenge.Purpose.SETTINGS,
                        code,
                        now)) {
                    accepted = true;
                    break;
                }
            }
        } else {
            String normalized = normalizeRecoveryCode(code);
            for (AuthMfaRecoveryCode recovery : recoveryCodeRepository
                    .findByCredentialIdAndUsedAtIsNull(credential.getId())) {
                if (passwordEncoder.matches(normalized, recovery.getCodeHash())) {
                    recovery.setUsedAt(now);
                    recoveryCodeRepository.save(recovery);
                    accepted = true;
                    break;
                }
            }
        }
        if (!accepted) {
            int attempts = Optional.ofNullable(credential.getFailedAttempts()).orElse(0) + 1;
            credential.setFailedAttempts(attempts);
            boolean justLockedOut = false;
            if (attempts >= maxAttempts) {
                credential.setLockedUntil(now.plusSeconds(lockoutSeconds));
                credential.setFailedAttempts(0);
                justLockedOut = true;
            }
            credentialRepository.save(credential);
            auditMfaFailure(authId, method, attempts, justLockedOut);
            throw new UnauthorizedException("Invalid verification code");
        }
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credentialRepository.save(credential);
    }

    private void auditMfaFailure(Long authId, MfaMethod method, int attempts, boolean lockedOut) {
        try {
            AuthIdentity identity = identityRepository.findById(authId).orElse(null);
            String username = identity != null ? identity.getLoginIdentifier() : null;
            Map<String, Object> details = new HashMap<>();
            details.put("method", method != null ? method.name() : null);
            details.put("failedAttempts", attempts);
            if (lockedOut) {
                details.put("lockoutSeconds", lockoutSeconds);
                authAbuseMetrics.incrementMfaLockout();
                auditLogService.logAuthEvent(null, username, "mfa_lockout", null, null, "blocked", details);
            } else {
                auditLogService.logAuthEvent(null, username, "mfa_failed", null, null, "failure", details);
            }
        } catch (Exception e) {
            log.warn("Failed to record MFA failure audit event for authId: {}", authId, e);
        }
    }

    private static String formatMfaLockMessage(long retryAfterSeconds) {
        long seconds = Math.max(1L, retryAfterSeconds);
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes <= 0) {
            return "Too many MFA attempts. Try again in " + seconds
                    + (seconds == 1 ? " second." : " seconds.");
        }
        if (remainingSeconds == 0) {
            return "Too many MFA attempts. Try again in " + minutes
                    + (minutes == 1 ? " minute." : " minutes.");
        }
        return "Too many MFA attempts. Try again in " + minutes
                + (minutes == 1 ? " minute" : " minutes")
                + " and " + remainingSeconds
                + (remainingSeconds == 1 ? " second." : " seconds.");
    }

    private String destinationForCredential(
            AuthMfaCredential credential, AuthIdentity identity, MfaMethod method) {
        if (method == MfaMethod.SMS) {
            return StringUtils.hasText(credential.getPhoneE164())
                    ? credential.getPhoneE164()
                    : otpService.resolveSmsDestination(null, identity);
        }
        return otpService.resolveEmailDestination(identity);
    }

    private String maskedDestination(
            AuthMfaCredential credential, AuthIdentity identity, MfaMethod method) {
        if (credential == null || method == null) {
            return null;
        }
        if (method == MfaMethod.SMS) {
            return MfaOtpDeliveryService.maskDestination(MfaMethod.SMS, credential.getPhoneE164());
        }
        if (method == MfaMethod.EMAIL) {
            String email = otpService.resolveEmailDestination(identity);
            return MfaOtpDeliveryService.maskDestination(MfaMethod.EMAIL, email);
        }
        return null;
    }

    private MfaDtos.EnrollmentStartResponse enrollmentStart(
            MfaMethod method,
            String secret,
            String uri,
            String masked,
            boolean codeSent,
            AuthMfaCredential credential) {
        return new MfaDtos.EnrollmentStartResponse(
                method,
                secret,
                uri,
                masked,
                codeSent,
                otpDeliveryService.isChannelAvailable(MfaMethod.SMS),
                otpDeliveryService.isChannelAvailable(MfaMethod.EMAIL),
                enrolledMethodNames(credential));
    }

    private void ensureMethodFlags(AuthMfaCredential credential) {
        if (credential == null || !Boolean.TRUE.equals(credential.getEnabled())) {
            return;
        }
        boolean any = Boolean.TRUE.equals(credential.getTotpEnabled())
                || Boolean.TRUE.equals(credential.getSmsEnabled())
                || Boolean.TRUE.equals(credential.getEmailEnabled());
        if (any) {
            return;
        }
        MfaMethod method = credential.getMfaMethod() != null ? credential.getMfaMethod() : MfaMethod.TOTP;
        enableMethodFlag(credential, method);
        credentialRepository.save(credential);
    }

    private static void enableMethodFlag(AuthMfaCredential credential, MfaMethod method) {
        if (method == MfaMethod.SMS) {
            credential.setSmsEnabled(true);
        } else if (method == MfaMethod.EMAIL) {
            credential.setEmailEnabled(true);
        } else {
            credential.setTotpEnabled(true);
        }
    }

    private static boolean isMethodEnabled(AuthMfaCredential credential, MfaMethod method) {
        if (method == MfaMethod.SMS) {
            return Boolean.TRUE.equals(credential.getSmsEnabled());
        }
        if (method == MfaMethod.EMAIL) {
            return Boolean.TRUE.equals(credential.getEmailEnabled());
        }
        return Boolean.TRUE.equals(credential.getTotpEnabled());
    }

    private List<String> enrolledMethodNames(AuthMfaCredential credential) {
        List<String> methods = new ArrayList<>(3);
        if (Boolean.TRUE.equals(credential.getTotpEnabled())) {
            methods.add(MfaMethod.TOTP.name());
        }
        if (Boolean.TRUE.equals(credential.getSmsEnabled())) {
            methods.add(MfaMethod.SMS.name());
        }
        if (Boolean.TRUE.equals(credential.getEmailEnabled())) {
            methods.add(MfaMethod.EMAIL.name());
        }
        return List.copyOf(methods);
    }

    private MfaMethod resolvePreferredMethod(AuthMfaCredential credential, List<String> enrolled) {
        if (credential.getMfaMethod() != null && enrolled.contains(credential.getMfaMethod().name())) {
            return credential.getMfaMethod();
        }
        if (!enrolled.isEmpty()) {
            return MfaMethod.valueOf(enrolled.get(0));
        }
        return MfaMethod.TOTP;
    }

    private MfaMethod resolveLoginMethod(AuthMfaCredential credential, MfaMethod requested) {
        List<String> enrolled = enrolledMethodNames(credential);
        if (requested != null) {
            if (!enrolled.contains(requested.name())) {
                throw new BadRequestException(requested.name() + " is not set up for this account");
            }
            return requested;
        }
        return resolvePreferredMethod(credential, enrolled);
    }

    private boolean canAddMoreMethods(AuthMfaCredential credential) {
        List<String> enrolled = enrolledMethodNames(credential);
        if (!enrolled.contains(MfaMethod.TOTP.name())) {
            return true;
        }
        if (!enrolled.contains(MfaMethod.SMS.name())
                && otpDeliveryService.isChannelAvailable(MfaMethod.SMS)) {
            return true;
        }
        return !enrolled.contains(MfaMethod.EMAIL.name())
                && otpDeliveryService.isChannelAvailable(MfaMethod.EMAIL);
    }

    private List<String> replaceRecoveryCodes(AuthMfaCredential credential) {
        recoveryCodeRepository.deleteByCredentialId(credential.getId());
        List<String> plaintext = new ArrayList<>(recoveryCodeCount);
        for (int i = 0; i < recoveryCodeCount; i++) {
            byte[] random = new byte[10];
            secureRandom.nextBytes(random);
            String compact = Base64.getUrlEncoder().withoutPadding().encodeToString(random)
                    .toUpperCase(Locale.ROOT);
            String displayed = compact.substring(0, 4) + "-" + compact.substring(4, 8)
                    + "-" + compact.substring(8, 12) + "-" + compact.substring(12);
            recoveryCodeRepository.save(AuthMfaRecoveryCode.builder()
                    .credential(credential)
                    .codeHash(passwordEncoder.encode(normalizeRecoveryCode(displayed)))
                    .build());
            plaintext.add(displayed);
        }
        return List.copyOf(plaintext);
    }

    private static String normalizeRecoveryCode(String code) {
        return code == null ? "" : code.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record VerifiedChallenge(Long authId, String tenantSchema, Long organisationId) {}
}

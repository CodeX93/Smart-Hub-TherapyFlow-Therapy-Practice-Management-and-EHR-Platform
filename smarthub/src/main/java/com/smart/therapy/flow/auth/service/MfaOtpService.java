package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthMfaOtpChallenge;
import com.smart.therapy.flow.auth.enums.MfaMethod;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthMfaOtpChallengeRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class MfaOtpService {

    private final AuthMfaOtpChallengeRepository otpChallengeRepository;
    private final AuthIdentityRepository identityRepository;
    private final MfaOtpDeliveryService deliveryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.mfa.otp-validity-seconds:300}")
    private long otpValiditySeconds;

    @Value("${app.auth.mfa.otp-resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Transactional
    public String issueAndSendOtp(
            String jti,
            Long authId,
            AuthMfaOtpChallenge.Purpose purpose,
            MfaMethod channel,
            String destination
    ) {
        if (!deliveryService.isChannelAvailable(channel)) {
            throw new BadRequestException(channel + " verification is not available right now");
        }
        String code = generateCode();
        Instant now = Instant.now();
        String jtiHash = hashJti(jti);
        AuthMfaOtpChallenge challenge = otpChallengeRepository
                .findByJtiHashAndPurpose(jtiHash, purpose)
                .orElseGet(() -> AuthMfaOtpChallenge.builder()
                        .authIdentity(identityRepository.getReferenceById(authId))
                        .jtiHash(jtiHash)
                        .purpose(purpose)
                        .channel(channel)
                        .build());
        if (challenge.getLastSentAt() != null
                && challenge.getLastSentAt().plusSeconds(resendCooldownSeconds).isAfter(now)
                && challenge.getId() != null) {
            throw new StoryApiException(HttpStatus.TOO_MANY_REQUESTS, "MFA_OTP_COOLDOWN",
                    "Please wait before requesting another code.");
        }
        challenge.setChannel(channel);
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setExpiresAt(now.plusSeconds(otpValiditySeconds));
        challenge.setConsumedAt(null);
        challenge.setLastSentAt(now);
        otpChallengeRepository.save(challenge);
        deliveryService.sendOtp(channel, destination, code);
        return MfaOtpDeliveryService.maskDestination(channel, destination);
    }

    @Transactional
    public boolean verifyOtp(String jti, AuthMfaOtpChallenge.Purpose purpose, String code, Instant now) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        AuthMfaOtpChallenge challenge = otpChallengeRepository
                .findByJtiHashAndPurpose(hashJti(jti), purpose)
                .orElse(null);
        if (challenge == null
                || challenge.getConsumedAt() != null
                || challenge.getExpiresAt() == null
                || !challenge.getExpiresAt().isAfter(now)) {
            return false;
        }
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            return false;
        }
        challenge.setConsumedAt(now);
        otpChallengeRepository.save(challenge);
        return true;
    }

    @Transactional(readOnly = true)
    public MfaMethod resolveEnrollmentMethod(MfaMethod requested, String phone, AuthIdentity identity) {
        if (requested == null || requested == MfaMethod.TOTP) {
            return MfaMethod.TOTP;
        }
        if (requested == MfaMethod.SMS) {
            if (!StringUtils.hasText(phone) && !StringUtils.hasText(identity.getPhone())) {
                throw new BadRequestException("Mobile number is required for SMS verification");
            }
            if (!deliveryService.isChannelAvailable(MfaMethod.SMS)) {
                throw new BadRequestException("SMS verification is not configured. Try email or authenticator app.");
            }
            return MfaMethod.SMS;
        }
        if (requested == MfaMethod.EMAIL) {
            if (!deliveryService.isChannelAvailable(MfaMethod.EMAIL)) {
                throw new BadRequestException("Email verification is not available.");
            }
            return MfaMethod.EMAIL;
        }
        throw new BadRequestException("Unsupported MFA method");
    }

    public String resolveSmsDestination(String phone, AuthIdentity identity) {
        String raw = StringUtils.hasText(phone) ? phone : identity.getPhone();
        if (!StringUtils.hasText(raw)) {
            throw new BadRequestException("Mobile number is required for SMS verification");
        }
        return raw;
    }

    public String resolveEmailDestination(AuthIdentity identity) {
        String email = StringUtils.hasText(identity.getEmail()) ? identity.getEmail() : identity.getLoginIdentifier();
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            throw new BadRequestException("A valid email login is required for email verification");
        }
        return email;
    }

    public String enrollmentJtiForAuthId(Long authId) {
        return "enrollment:" + authId;
    }

    public String settingsJtiForAuthId(Long authId) {
        return "settings:" + authId;
    }

    public String changeEnrollmentJtiForAuthId(Long authId) {
        return "change-enrollment:" + authId;
    }

    public static String hashJti(String jti) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(jti.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String generateCode() {
        int value = secureRandom.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}

package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.auth.dto.AuthDeviceDtos;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthKnownDevice;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.AuthKnownDeviceRepository;
import com.smart.therapy.flow.auth.util.DeviceInfoUtil;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthKnownDeviceService {

    private final AuthKnownDeviceRepository knownDeviceRepository;
    private final AuthIdentityRepository identityRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.auth.trusted-device.enabled:true}")
    private boolean trustedDeviceEnabled;

    @Value("${app.auth.trusted-device.trust-days:30}")
    private int trustDays;

    @Value("${app.auth.trusted-device.stay-signed-in-refresh-days:30}")
    private int staySignedInRefreshDays;

    public boolean isTrustedDeviceEnabled() {
        return trustedDeviceEnabled;
    }

    public int getTrustDays() {
        return Math.max(1, trustDays);
    }

    public long staySignedInRefreshExpirationMs() {
        return Math.max(1, staySignedInRefreshDays) * 24L * 60L * 60L * 1000L;
    }

    /**
     * Records the device used for a successful login. Sends a security email on first sighting.
     */
    @Transactional
    public void recordSuccessfulLogin(Long authId, String ipAddress, String userAgent) {
        upsertDevice(authId, ipAddress, userAgent, false);
    }

    /**
     * Returns true when the presented opaque trust token is valid for this identity.
     */
    @Transactional(readOnly = true)
    public boolean isTrustedDevice(Long authId, String deviceTrustToken) {
        return findActiveTrustedDevice(authId, deviceTrustToken).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<AuthKnownDevice> findActiveTrustedDevice(Long authId, String deviceTrustToken) {
        if (!trustedDeviceEnabled || authId == null || !StringUtils.hasText(deviceTrustToken)) {
            return Optional.empty();
        }
        String hash = hashToken(deviceTrustToken.trim());
        return knownDeviceRepository.findByAuthIdentityIdAndTrustTokenHash(authId, hash)
                .filter(device -> device.isTrustActive(Instant.now()));
    }

    /**
     * Marks the current browser/device as trusted and returns a one-time plaintext trust token
     * for the client to store and send on future logins.
     */
    @Transactional
    public String trustCurrentDevice(Long authId, String ipAddress, String userAgent) {
        if (!trustedDeviceEnabled) {
            return null;
        }
        AuthKnownDevice device = upsertDevice(authId, ipAddress, userAgent, true);
        if (device == null) {
            return null;
        }
        Instant now = Instant.now();
        String plaintext = generateTrustToken();
        device.setTrusted(true);
        device.setTrustedAt(now);
        device.setTrustExpiresAt(now.plus(getTrustDays(), ChronoUnit.DAYS));
        device.setTrustRevokedAt(null);
        device.setTrustTokenHash(hashToken(plaintext));
        knownDeviceRepository.save(device);
        log.info("Trusted device registered for authId={} deviceId={} expiresAt={}",
                authId, device.getId(), device.getTrustExpiresAt());
        return plaintext;
    }

    @Transactional
    public void touchTrustedDevice(Long authId, String deviceTrustToken, String ipAddress, String userAgent) {
        findActiveTrustedDevice(authId, deviceTrustToken).ifPresent(device -> {
            Instant now = Instant.now();
            device.setLastSeenAt(now);
            if (StringUtils.hasText(ipAddress)) {
                device.setLastIp(ipAddress);
            }
            if (StringUtils.hasText(userAgent)) {
                device.setUserAgent(userAgent);
                device.setDeviceLabel(DeviceInfoUtil.deviceLabel(userAgent));
            }
            knownDeviceRepository.save(device);
        });
    }

    @Transactional(readOnly = true)
    public AuthDeviceDtos.DeviceListResponse listDevices(
            Long authId, String currentDeviceTrustToken, String requestUserAgent) {
        Instant now = Instant.now();
        String currentHash = StringUtils.hasText(currentDeviceTrustToken)
                ? hashToken(currentDeviceTrustToken.trim()) : null;
        String currentFingerprint = DeviceInfoUtil.fingerprintHash(authId, requestUserAgent);
        List<AuthDeviceDtos.DeviceItem> items = knownDeviceRepository
                .findByAuthIdentityIdOrderByLastSeenAtDesc(authId)
                .stream()
                .map(device -> {
                    boolean trusted = device.isTrustActive(now);
                    boolean current = (currentHash != null && currentHash.equals(device.getTrustTokenHash()))
                            || (currentFingerprint != null
                            && currentFingerprint.equals(device.getFingerprintHash()));
                    return new AuthDeviceDtos.DeviceItem(
                            device.getId(),
                            device.getDeviceLabel(),
                            device.getLastIp(),
                            device.getUserAgent(),
                            device.getFirstSeenAt(),
                            device.getLastSeenAt(),
                            trusted,
                            trusted ? device.getTrustExpiresAt() : null,
                            current);
                })
                .toList();
        int trustedCount = (int) items.stream().filter(AuthDeviceDtos.DeviceItem::trusted).count();
        return new AuthDeviceDtos.DeviceListResponse(items.size(), trustedCount, items);
    }

    @Transactional
    public void revokeDevice(Long authId, Long deviceId) {
        AuthKnownDevice device = knownDeviceRepository.findByIdAndAuthIdentityId(deviceId, authId)
                .orElseThrow(() -> new BadRequestException("Device not found"));
        Instant now = Instant.now();
        device.setTrusted(false);
        device.setTrustRevokedAt(now);
        device.setTrustTokenHash(null);
        knownDeviceRepository.save(device);
        log.info("Revoked device trust authId={} deviceId={}", authId, deviceId);
    }

    @Transactional
    public int revokeAllTrustedDevices(Long authId) {
        if (authId == null) {
            return 0;
        }
        int revoked = knownDeviceRepository.revokeAllTrustedForAuthId(authId, Instant.now());
        if (revoked > 0) {
            log.info("Revoked {} trusted device(s) for authId={}", revoked, authId);
        }
        return revoked;
    }

    private AuthKnownDevice upsertDevice(
            Long authId, String ipAddress, String userAgent, boolean skipNewDeviceEmail) {
        if (authId == null) {
            return null;
        }
        AuthIdentity identity = identityRepository.findById(authId).orElse(null);
        if (identity == null) {
            return null;
        }
        Instant now = Instant.now();
        String fingerprint = DeviceInfoUtil.fingerprintHash(authId, userAgent);
        String label = DeviceInfoUtil.deviceLabel(userAgent);
        AuthKnownDevice device = knownDeviceRepository
                .findByAuthIdentityIdAndFingerprintHash(authId, fingerprint)
                .orElse(null);
        boolean isNew = device == null;
        if (isNew) {
            device = AuthKnownDevice.builder()
                    .authIdentity(identity)
                    .fingerprintHash(fingerprint)
                    .deviceLabel(label)
                    .userAgent(userAgent)
                    .lastIp(ipAddress)
                    .firstSeenAt(now)
                    .lastSeenAt(now)
                    .lastNotifiedAt(now)
                    .trusted(false)
                    .build();
            knownDeviceRepository.save(device);
            if (!skipNewDeviceEmail) {
                sendNewDeviceEmail(identity, label, ipAddress, userAgent, now);
            }
            return device;
        }
        device.setDeviceLabel(label);
        device.setUserAgent(userAgent);
        device.setLastIp(ipAddress);
        device.setLastSeenAt(now);
        return knownDeviceRepository.save(device);
    }

    private String generateTrustToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private void sendNewDeviceEmail(
            AuthIdentity identity,
            String deviceLabel,
            String ipAddress,
            String userAgent,
            Instant at
    ) {
        String to = identity.getLoginIdentifier();
        if (!StringUtils.hasText(to) || !to.contains("@")) {
            log.info("Skipping new-device email for authId {}: no email login", identity.getId());
            return;
        }
        try {
            emailService.sendNewDeviceLoginAlert(
                    to,
                    to,
                    deviceLabel,
                    ipAddress != null ? ipAddress : "unknown",
                    userAgent != null ? userAgent : "unknown",
                    at);
        } catch (Exception e) {
            log.warn("Failed to send new-device login email for authId {}: {}", identity.getId(), e.getMessage());
        }
    }
}

package com.smart.therapy.flow.auth.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.OrganisationSsoAllowedDomain;
import com.smart.therapy.flow.organisation.entity.OrganisationSsoState;
import com.smart.therapy.flow.organisation.repository.OrganisationSsoAllowedDomainRepository;
import com.smart.therapy.flow.organisation.repository.OrganisationSsoStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SsoSecurityService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrganisationSsoStateRepository stateRepository;
    private final OrganisationSsoAllowedDomainRepository allowedDomainRepository;

    @Transactional
    public SsoStatePayload createState(Long organisationId,
                                       String provider,
                                       String redirectUri,
                                       Instant now,
                                       long ttlSeconds) {
        if (now == null) {
            now = Instant.now();
        }
        stateRepository.deleteByExpiresAtBefore(now.minusSeconds(60));
        String stateToken = randomToken(32);
        String nonce = randomToken(24);
        String pkceVerifier = randomToken(32);
        OrganisationSsoState saved = stateRepository.save(OrganisationSsoState.builder()
                .stateToken(stateToken)
                .organisationId(organisationId)
                .provider(provider.toUpperCase(Locale.ROOT))
                .redirectUri(redirectUri)
                .nonce(nonce)
                .pkceVerifier(pkceVerifier)
                .createdAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .build());
        return new SsoStatePayload(saved.getStateToken(), saved.getNonce(), saved.getPkceVerifier());
    }

    @Transactional
    public ConsumedState consumeStateOrThrow(String stateToken, Instant now) {
        if (stateToken == null || stateToken.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_STATE_INVALID", "Missing state");
        }
        if (now == null) {
            now = Instant.now();
        }
        OrganisationSsoState state = stateRepository.findByStateToken(stateToken)
                .orElseThrow(() -> new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_STATE_INVALID", "Invalid state"));
        if (state.getUsedAt() != null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_STATE_INVALID", "State already consumed");
        }
        if (state.getExpiresAt() == null || !state.getExpiresAt().isAfter(now)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "SSO_STATE_INVALID", "State expired");
        }
        state.setUsedAt(now);
        stateRepository.save(state);
        return new ConsumedState(
                state.getOrganisationId(),
                state.getProvider(),
                state.getRedirectUri(),
                state.getNonce(),
                state.getPkceVerifier()
        );
    }

    @Transactional(readOnly = true)
    public List<String> getAllowedDomains(Long organisationId) {
        return allowedDomainRepository.findByOrganisationIdOrderByDomainAsc(organisationId).stream()
                .map(OrganisationSsoAllowedDomain::getDomain)
                .filter(Objects::nonNull)
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    @Transactional
    public List<String> replaceAllowedDomains(Long organisationId, List<String> domains) {
        allowedDomainRepository.deleteByOrganisationId(organisationId);
        if (domains == null || domains.isEmpty()) {
            return List.of();
        }
        List<String> normalized = domains.stream()
                .filter(Objects::nonNull)
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .filter(v -> !v.isBlank())
                .distinct()
                .toList();
        for (String domain : normalized) {
            allowedDomainRepository.save(OrganisationSsoAllowedDomain.builder()
                    .organisationId(organisationId)
                    .domain(domain)
                    .build());
        }
        return normalized;
    }

    public static String pkceChallengeS256(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create PKCE challenge", ex);
        }
    }

    private static String randomToken(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record SsoStatePayload(String stateToken, String nonce, String pkceVerifier) {}

    public record ConsumedState(Long organisationId, String provider, String redirectUri, String nonce, String pkceVerifier) {}
}

package com.smart.therapy.flow.superadmin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.service.EncryptionService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminIntegrationResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminIntegrationTestResponse;
import com.smart.therapy.flow.superadmin.entity.PlatformApiKey;
import com.smart.therapy.flow.superadmin.entity.PlatformApiKeyScope;
import com.smart.therapy.flow.superadmin.entity.PlatformIntegrationConfig;
import com.smart.therapy.flow.superadmin.repository.PlatformApiKeyRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformApiKeyScopeRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformIntegrationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminIntegrationService {

    private static final Set<String> ALLOWED_KEYS = Set.of("stripe", "zoom", "openai", "sparkpost", "ses");
    private static final Set<String> OAUTH_KEYS = Set.of("zoom");
    private static final String SECRET_MASK = "***";
    private static final Set<String> LEGACY_API_KEY_SCOPES = Set.of("read", "write", "admin");
    private static final int MAX_ACTIVE_API_KEYS = 50;

    private final PlatformIntegrationConfigRepository integrationRepository;
    private final PlatformApiKeyRepository apiKeyRepository;
    private final PlatformApiKeyScopeRepository apiKeyScopeRepository;
    private final ObjectMapper objectMapper;
    private final EncryptionService encryptionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<SuperAdminIntegrationResponse> listIntegrations() {
        Map<String, PlatformIntegrationConfig> existingByKey = integrationRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        PlatformIntegrationConfig::getIntegrationKey,
                        row -> row,
                        (a, b) -> a
                ));
        Set<String> orderedKeys = new LinkedHashSet<>();
        orderedKeys.add("stripe");
        orderedKeys.add("zoom");
        orderedKeys.addAll(ALLOWED_KEYS);
        return orderedKeys.stream()
                .map(key -> toResponse(key, existingByKey.get(key)))
                .toList();
    }

    @Transactional(readOnly = true)
    public SuperAdminIntegrationResponse getIntegration(String key) {
        String normalized = normalizeKey(key);
        PlatformIntegrationConfig row = integrationRepository.findByIntegrationKey(normalized)
                .orElse(null);
        return toResponse(normalized, row);
    }

    @Transactional
    public SuperAdminIntegrationResponse upsertIntegration(String key,
                                                           Boolean enabled,
                                                           String clientId,
                                                           String publishableKey,
                                                           String secret,
                                                           String connectClientSecret,
                                                           String connectWebhookSecret,
                                                           String platformWebhookSecret,
                                                           String webhookUrl,
                                                           Long actorAuthId) {
        String normalized = normalizeKey(key);
        if (enabled == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "enabled is required");
        }
        if (StringUtils.hasText(webhookUrl)) {
            validateWebhookUrl(webhookUrl);
        }
        if (StringUtils.hasText(connectWebhookSecret)) {
            validateWebhookSigningSecret(connectWebhookSecret, "connectWebhookSecret");
        }
        if (StringUtils.hasText(platformWebhookSecret)) {
            validateWebhookSigningSecret(platformWebhookSecret, "platformWebhookSecret");
        }

        Instant now = Instant.now();
        PlatformIntegrationConfig row = integrationRepository.findByIntegrationKey(normalized)
                .orElseGet(() -> PlatformIntegrationConfig.builder()
                        .integrationKey(normalized)
                        .createdAt(now)
                        .build());

        Map<String, Object> config = readConfigMap(row.getConfigJson());
        migratePlainSecretsToEncrypted(config);
        applyRequestToConfig(normalized, config, clientId, publishableKey, secret, connectClientSecret,
                connectWebhookSecret, platformWebhookSecret, webhookUrl);
        validateEnabledCredentials(normalized, enabled, config);

        row.setEnabled(enabled);
        row.setUpdatedByAuthId(actorAuthId);
        row.setUpdatedAt(now);
        row.setConfigJson(writeConfigJson(config));
        row.setMaskedSummary(buildMaskedSummary(config));

        PlatformIntegrationConfig saved = integrationRepository.save(row);
        return toResponse(normalized, saved);
    }

    @Transactional(readOnly = true)
    public SuperAdminIntegrationTestResponse testIntegration(String key) {
        String normalized = normalizeKey(key);
        PlatformIntegrationConfig row = integrationRepository.findByIntegrationKey(normalized)
                .orElse(null);

        Map<String, Object> config = row == null ? Map.of() : readConfigMap(row.getConfigJson());
        migratePlainSecretsToEncrypted(config);

        long startNs = System.nanoTime();
        try {
            if ("stripe".equals(normalized)) {
                String stripeSecret = firstPresentDecrypted(config, "secret", "platformSecretKey", "secretKey");
                if (!StringUtils.hasText(stripeSecret)) {
                    return testFailure(startNs, "Stripe secret is not configured");
                }
                HttpResponse<String> res = executeHttpGet(
                        "https://api.stripe.com/v1/customers?limit=1",
                        Map.of("Authorization", "Bearer " + stripeSecret)
                );
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    return testSuccess(startNs);
                }
                return testFailure(startNs, "Stripe API returned HTTP " + res.statusCode());
            }

            if ("zoom".equals(normalized)) {
                String cid = firstPresentDecrypted(config, "clientId");
                String sec = firstPresentDecrypted(config, "secret", "clientSecret");
                if (!StringUtils.hasText(cid) || !StringUtils.hasText(sec)) {
                    return testFailure(startNs, "Zoom clientId/secret are not configured");
                }
                HttpResponse<String> res = executeHttpGet("https://api.zoom.us/v2/users/me", Map.of());
                if (res.statusCode() < 500) {
                    return testSuccess(startNs);
                }
                return testFailure(startNs, "Zoom API returned HTTP " + res.statusCode());
            }

            HttpResponse<String> res = executeHttpGet("https://" + normalized + ".com", Map.of());
            if (res.statusCode() < 500) {
                return testSuccess(startNs);
            }
            return testFailure(startNs, "Provider returned HTTP " + res.statusCode());
        } catch (Exception ex) {
            return testFailure(startNs, ex.getMessage());
        }
    }

    @Transactional
    public CreatedApiKey createApiKey(String keyName, List<String> scopes, Instant expiresAt, Long actorAuthId) {
        return createApiKeyInternal(keyName, scopes, expiresAt, actorAuthId, false);
    }

    @Transactional(readOnly = true)
    public List<PlatformApiKey> listApiKeys() {
        return apiKeyRepository.findAll();
    }

    public List<String> resolveApiKeyScopes(PlatformApiKey key) {
        List<PlatformApiKeyScope> rows = apiKeyScopeRepository.findByApiKeyId(key.getId());
        if (rows != null && !rows.isEmpty()) {
            return rows.stream()
                    .map(PlatformApiKeyScope::getScope)
                    .filter(StringUtils::hasText)
                    .toList();
        }
        return parseScopes(key.getScopesJson());
    }

    @Transactional
    public void revokeApiKey(Long keyId) {
        PlatformApiKey row = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "KEY_NOT_FOUND", "API key not found"));
        row.setIsActive(false);
        row.setRevokedAt(Instant.now());
        apiKeyRepository.save(row);
    }

    @Transactional
    public CreatedApiKey rotateApiKey(Long keyId, Long actorAuthId) {
        PlatformApiKey existing = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "KEY_NOT_FOUND", "API key not found"));
        existing.setIsActive(false);
        existing.setRevokedAt(Instant.now());
        apiKeyRepository.save(existing);

        return createApiKeyInternal(existing.getKeyName(), parseScopes(existing.getScopesJson()),
                existing.getExpiresAt(), actorAuthId, true);
    }

    private SuperAdminIntegrationResponse toResponse(String key, PlatformIntegrationConfig row) {
        Map<String, Object> config = row == null ? Map.of() : readConfigMap(row.getConfigJson());
        String clientId = firstPresentDecrypted(config, "clientId", "connectClientId");
        String publishableKey = firstPresentDecrypted(config, "publishableKey", "platformPublishableKey");
        String secret = hasSecret(config) ? SECRET_MASK : null;
        String connectClientSecret = hasConnectClientSecret(config) ? SECRET_MASK : null;
        String connectWebhookSecret = hasConnectWebhookSecret(config) ? SECRET_MASK : null;
        String platformWebhookSecret = hasPlatformWebhookSecret(config) ? SECRET_MASK : null;
        String webhookUrl = firstPresentDecrypted(config, "webhookUrl");
        return SuperAdminIntegrationResponse.builder()
                .key(key)
                .enabled(row != null && Boolean.TRUE.equals(row.getEnabled()))
                .clientId(clientId)
                .publishableKey(publishableKey)
                .secret(secret)
                .connectClientSecret(connectClientSecret)
                .connectWebhookSecret(connectWebhookSecret)
                .platformWebhookSecret(platformWebhookSecret)
                .webhookUrl(webhookUrl)
                .lastConfiguredAt(row != null ? row.getUpdatedAt() : null)
                .build();
    }

    private String normalizeKey(String key) {
        String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_KEYS.contains(normalized)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "INTEGRATION_NOT_FOUND", "Integration not found: " + key);
        }
        return normalized;
    }

    private static void validateWebhookUrl(String webhookUrl) {
        String value = webhookUrl.trim();
        if (value.length() > 255 || !value.startsWith("https://")) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "WEBHOOK_NOT_HTTPS",
                    "webhookUrl must start with https:// and be <= 255 chars");
        }
    }

    private void validateEnabledCredentials(String key, Boolean enabled, Map<String, Object> config) {
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }

        if (OAUTH_KEYS.contains(key)) {
            String clientId = firstPresentDecrypted(config, "clientId", "connectClientId");
            String secret = firstPresentDecrypted(config, "secret", "clientSecret", "connectClientSecret");
            if (!StringUtils.hasText(clientId) || !StringUtils.hasText(secret)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "CREDENTIALS_REQUIRED",
                        "clientId and secret are required when integration is enabled");
            }
            return;
        }

        if ("stripe".equals(key)) {
            String secret = firstPresentDecrypted(config, "secret", "platformSecretKey", "secretKey");
            if (!StringUtils.hasText(secret)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "CREDENTIALS_REQUIRED",
                        "secret is required when stripe integration is enabled");
            }
            String clientId = firstPresentDecrypted(config, "clientId", "connectClientId");
            String connectClientSecret = firstPresentDecrypted(config, "connectClientSecret", "clientSecret");
            if (StringUtils.hasText(clientId) && !StringUtils.hasText(connectClientSecret)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "CREDENTIALS_REQUIRED",
                        "connectClientSecret is required when Stripe Connect clientId is configured");
            }
        }
    }

    private void applyRequestToConfig(String key,
                                      Map<String, Object> config,
                                      String clientId,
                                      String publishableKey,
                                      String secret,
                                      String connectClientSecret,
                                      String connectWebhookSecret,
                                      String platformWebhookSecret,
                                      String webhookUrl) {
        if (StringUtils.hasText(clientId)) {
            String encryptedClientId = encryptionService.encrypt(clientId.trim());
            config.put("clientIdEncrypted", encryptedClientId);
            if ("stripe".equals(key)) {
                config.put("connectClientIdEncrypted", encryptedClientId);
            }
        }
        if (StringUtils.hasText(publishableKey)) {
            String normalized = publishableKey.trim();
            if ("stripe".equals(key) && !(normalized.startsWith("pk_") || normalized.startsWith("rk_"))) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "stripe publishableKey must start with pk_ (or rk_ for restricted keys)");
            }
            config.put("publishableKey", normalized);
            if ("stripe".equals(key)) {
                config.put("platformPublishableKey", normalized);
            }
        }
        if (StringUtils.hasText(secret)) {
            String encryptedSecret = encryptionService.encrypt(secret.trim());
            config.put("secretEncrypted", encryptedSecret);
            if ("stripe".equals(key)) {
                config.put("platformSecretKeyEncrypted", encryptedSecret);
                config.put("secretKeyEncrypted", encryptedSecret);
            }
            if ("zoom".equals(key)) {
                config.put("clientSecretEncrypted", encryptedSecret);
            }
        }
        if (StringUtils.hasText(connectClientSecret)) {
            String encryptedConnectClientSecret = encryptionService.encrypt(connectClientSecret.trim());
            config.put("connectClientSecretEncrypted", encryptedConnectClientSecret);
        }
        if (StringUtils.hasText(connectWebhookSecret)) {
            String encryptedConnectWebhookSecret = encryptionService.encrypt(connectWebhookSecret.trim());
            config.put("connectWebhookSecretEncrypted", encryptedConnectWebhookSecret);
        }
        if (StringUtils.hasText(platformWebhookSecret)) {
            String encryptedPlatformWebhookSecret = encryptionService.encrypt(platformWebhookSecret.trim());
            config.put("platformWebhookSecretEncrypted", encryptedPlatformWebhookSecret);
            config.put("webhookSecretEncrypted", encryptedPlatformWebhookSecret);
        }
        if (StringUtils.hasText(webhookUrl)) {
            config.put("webhookUrl", webhookUrl.trim());
        }
    }

    private void migratePlainSecretsToEncrypted(Map<String, Object> config) {
        encryptIfPlainPresent(config, "secret");
        encryptIfPlainPresent(config, "clientSecret");
        encryptIfPlainPresent(config, "connectClientSecret");
        encryptIfPlainPresent(config, "connectWebhookSecret");
        encryptIfPlainPresent(config, "platformWebhookSecret");
        encryptIfPlainPresent(config, "webhookSecret");
        encryptIfPlainPresent(config, "platformSecretKey");
        encryptIfPlainPresent(config, "secretKey");
        encryptIfPlainPresent(config, "clientId");
        encryptIfPlainPresent(config, "connectClientId");
    }

    private void encryptIfPlainPresent(Map<String, Object> config, String key) {
        Object raw = config.get(key);
        if (!(raw instanceof String value) || !StringUtils.hasText(value)) {
            return;
        }
        String encryptedKey = key + "Encrypted";
        if (!config.containsKey(encryptedKey)) {
            config.put(encryptedKey, encryptionService.encrypt(value.trim()));
        }
        config.remove(key);
    }

    private boolean hasSecret(Map<String, Object> config) {
        return StringUtils.hasText(firstPresentDecrypted(config, "secret", "platformSecretKey", "secretKey", "clientSecret"));
    }

    private boolean hasConnectClientSecret(Map<String, Object> config) {
        return StringUtils.hasText(firstPresentDecrypted(config, "connectClientSecret"));
    }

    private boolean hasConnectWebhookSecret(Map<String, Object> config) {
        return StringUtils.hasText(firstPresentDecrypted(config, "connectWebhookSecret"));
    }

    private boolean hasPlatformWebhookSecret(Map<String, Object> config) {
        return StringUtils.hasText(firstPresentDecrypted(config, "platformWebhookSecret", "webhookSecret"));
    }

    private static void validateWebhookSigningSecret(String secret, String fieldName) {
        String value = secret.trim();
        if (!value.startsWith("whsec_")) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    fieldName + " must start with whsec_");
        }
    }

    private String firstPresentDecrypted(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            Object raw = config.get(key);
            if (raw instanceof String str && StringUtils.hasText(str)) {
                return str.trim();
            }
            Object encryptedRaw = config.get(key + "Encrypted");
            if (encryptedRaw instanceof String enc && StringUtils.hasText(enc)) {
                try {
                    return encryptionService.decrypt(enc).trim();
                } catch (RuntimeException ignored) {
                    // ignore bad/deprecated payload
                }
            }
        }
        return null;
    }

    private Map<String, Object> readConfigMap(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return new HashMap<>();
        }
        try {
            return new HashMap<>(objectMapper.readValue(configJson, new TypeReference<>() {}));
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    private String writeConfigJson(Map<String, Object> config) {
        try {
            return objectMapper.writeValueAsString(config != null ? config : Map.of());
        } catch (Exception e) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid integration payload");
        }
    }

    private String buildMaskedSummary(Map<String, Object> config) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("clientId", firstPresentDecrypted(config, "clientId", "connectClientId"));
        summary.put("publishableKey", firstPresentDecrypted(config, "publishableKey", "platformPublishableKey"));
        summary.put("secret", hasSecret(config) ? SECRET_MASK : null);
        summary.put("connectClientSecret", hasConnectClientSecret(config) ? SECRET_MASK : null);
        summary.put("connectWebhookSecret", hasConnectWebhookSecret(config) ? SECRET_MASK : null);
        summary.put("platformWebhookSecret", hasPlatformWebhookSecret(config) ? SECRET_MASK : null);
        summary.put("webhookUrl", firstPresentDecrypted(config, "webhookUrl"));
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{\"secret\":\"***\"}";
        }
    }

    private static HttpResponse<String> executeHttpGet(String url, Map<String, String> headers) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET();
        headers.forEach(builder::header);

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private SuperAdminIntegrationTestResponse testSuccess(long startNs) {
        return SuperAdminIntegrationTestResponse.builder()
                .success(true)
                .latencyMs(elapsedMs(startNs))
                .error(null)
                .build();
    }

    private SuperAdminIntegrationTestResponse testFailure(long startNs, String error) {
        String message = StringUtils.hasText(error) ? error : "Provider connectivity check failed";
        return SuperAdminIntegrationTestResponse.builder()
                .success(false)
                .latencyMs(elapsedMs(startNs))
                .error(message)
                .build();
    }

    private static long elapsedMs(long startNs) {
        return Math.max(0L, (System.nanoTime() - startNs) / 1_000_000L);
    }

    private String serializeScopes(List<String> scopes) {
        try {
            return objectMapper.writeValueAsString(scopes != null ? scopes : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    public List<String> parseScopes(String scopesJson) {
        try {
            return objectMapper.readValue(scopesJson, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private CreatedApiKey createApiKeyInternal(String keyName,
                                                 List<String> scopes,
                                                 Instant expiresAt,
                                                 Long actorAuthId,
                                                 boolean allowNameReuseAfterRotate) {
        String normalizedName = normalizeKeyName(keyName);
        List<String> normalizedScopes = normalizeScopes(scopes);
        validateExpiry(expiresAt);

        if (!allowNameReuseAfterRotate && apiKeyRepository.existsByKeyNameIgnoreCase(normalizedName)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name must be unique");
        }

        long activeCount = apiKeyRepository.countByIsActiveTrue();
        if (activeCount >= MAX_ACTIVE_API_KEYS) {
            throw new StoryApiException(HttpStatus.CONFLICT, "KEY_LIMIT_REACHED",
                    "Maximum of " + MAX_ACTIVE_API_KEYS + " active API keys reached");
        }
        if (apiKeyRepository.existsByKeyNameIgnoreCaseAndIsActiveTrue(normalizedName)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name already in use");
        }

        String raw = generateRawApiKey();
        String prefix = raw.substring(0, Math.min(18, raw.length()));
        String hash = sha256(raw);
        Instant now = Instant.now();

        PlatformApiKey row = PlatformApiKey.builder()
                .keyName(normalizedName)
                .keyPrefix(prefix)
                .keyHash(hash)
                .scopesJson(serializeScopes(normalizedScopes))
                .isActive(true)
                .expiresAt(expiresAt)
                .createdByAuthId(actorAuthId)
                .createdAt(now)
                .build();
        PlatformApiKey saved = apiKeyRepository.save(row);
        persistScopes(saved, normalizedScopes, now);
        return new CreatedApiKey(saved, raw);
    }

    private static String normalizeKeyName(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name is required");
        }
        String trimmed = keyName.trim();
        if (trimmed.length() < 2 || trimmed.length() > 80) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name must be 2-80 characters");
        }
        return trimmed;
    }

    private static void validateExpiry(Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "expiresAt must be in the future");
        }
    }

    private static List<String> normalizeScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "scopes are required");
        }
        List<String> normalized = scopes.stream()
                .filter(StringUtils::hasText)
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
        if (normalized.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "scopes are required");
        }
        for (String scope : normalized) {
            if (LEGACY_API_KEY_SCOPES.contains(scope)) {
                continue;
            }
            if (!scope.matches("^[a-z][a-z0-9_-]*:[a-z][a-z0-9_-]*$")) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "Invalid scope: " + scope);
            }
        }
        return normalized;
    }

    private void persistScopes(PlatformApiKey saved, List<String> scopes, Instant now) {
        if (scopes == null || scopes.isEmpty()) {
            return;
        }
        List<PlatformApiKeyScope> rows = scopes.stream()
                .map(scope -> PlatformApiKeyScope.builder()
                        .apiKey(saved)
                        .scope(scope)
                        .createdAt(now)
                        .build())
                .toList();
        apiKeyScopeRepository.saveAll(rows);
    }

    private String generateRawApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "tfp_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash API key", e);
        }
    }

    public record CreatedApiKey(PlatformApiKey saved, String rawKey) {}
}

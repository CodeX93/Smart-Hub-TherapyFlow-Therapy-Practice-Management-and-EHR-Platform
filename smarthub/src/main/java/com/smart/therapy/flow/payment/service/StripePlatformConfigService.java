package com.smart.therapy.flow.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.service.EncryptionService;
import com.smart.therapy.flow.superadmin.entity.PlatformIntegrationConfig;
import com.smart.therapy.flow.superadmin.repository.PlatformIntegrationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePlatformConfigService {

    private static final String STRIPE_INTEGRATION_KEY = "stripe";

    private final PlatformIntegrationConfigRepository integrationRepository;
    private final ObjectMapper objectMapper;
    private final EncryptionService encryptionService;

    // New: All Stripe configuration from Key Vault (via application.yml)
    @Value("${stripe.publishable-key:}")
    private String kvPublishableKey;

    @Value("${stripe.secret-key:}")
    private String kvSecretKey;

    @Value("${stripe.connect.client-id:}")
    private String kvConnectClientId;

    @Value("${stripe.connect.client-secret:}")
    private String kvConnectClientSecret;

    @Value("${stripe.webhook.connect-secret:}")
    private String kvConnectWebhookSecret;

    @Value("${stripe.webhook.platform-secret:}")
    private String kvPlatformWebhookSecret;

    @Value("${stripe.webhook.url:}")
    private String kvWebhookUrl;

    // Legacy fallback values (deprecated)
    @Value("${stripe.webhook-secret:}")
    private String legacyWebhookSecret;

    public String requirePlatformSecretKey() {
        // Priority 1: Key Vault (via application.yml)
        if (StringUtils.hasText(kvSecretKey)) {
            log.debug("Using Stripe secret key from Key Vault");
            return kvSecretKey.trim();
        }

        // Priority 2: Database (fallback for backward compatibility)
        log.debug("Key Vault secret key not found, falling back to database config");
        String dbKey = firstPresentFromDb(
                "platformSecretKey",
                "secretKey",
                "secret_key",
                "apiKey"
        );
        if (StringUtils.hasText(dbKey)) {
            return dbKey;
        }

        throw new BadRequestException("Platform Stripe secret key is not configured");
    }

    public String requirePlatformPublishableKey() {
        // Priority 1: Key Vault (via application.yml)
        if (StringUtils.hasText(kvPublishableKey)) {
            log.debug("Using Stripe publishable key from Key Vault");
            return kvPublishableKey.trim();
        }

        // Priority 2: Database (fallback for backward compatibility)
        log.debug("Key Vault publishable key not found, falling back to database config");
        String dbKey = firstPresentFromDb("platformPublishableKey", "publishableKey", "publishable_key");
        if (StringUtils.hasText(dbKey)) {
            return dbKey;
        }

        throw new BadRequestException("Platform Stripe publishable key is not configured");
    }

    public String requirePlatformWebhookSecret() {
        // Priority 1: Key Vault (via application.yml)
        if (StringUtils.hasText(kvPlatformWebhookSecret)) {
            log.debug("Using Stripe platform webhook secret from Key Vault");
            return kvPlatformWebhookSecret.trim();
        }

        // Priority 2: Legacy webhook secret
        if (StringUtils.hasText(legacyWebhookSecret)) {
            log.debug("Using legacy webhook secret from environment");
            return legacyWebhookSecret.trim();
        }

        // Priority 3: Database (fallback for backward compatibility)
        log.debug("Key Vault webhook secret not found, falling back to database config");
        String dbSecret = firstPresentFromDb("platformWebhookSecret", "webhookSecret", "webhook_secret");
        if (StringUtils.hasText(dbSecret)) {
            return dbSecret;
        }

        throw new BadRequestException("Platform Stripe webhook secret is not configured");
    }

    public String requireConnectClientId() {
        // Priority 1: Key Vault (via application.yml)
        if (StringUtils.hasText(kvConnectClientId)) {
            log.debug("Using Stripe Connect client ID from Key Vault");
            return kvConnectClientId.trim();
        }

        // Priority 2: Database (fallback for backward compatibility)
        log.debug("Key Vault Connect client ID not found, falling back to database config");
        String dbClientId = firstPresentFromDb("connectClientId", "clientId", "connect_client_id");
        if (StringUtils.hasText(dbClientId)) {
            return dbClientId;
        }

        throw new BadRequestException("Stripe Connect client id is not configured");
    }

    public String requireConnectClientSecret() {
        // Priority 1: Key Vault (via application.yml)
        if (StringUtils.hasText(kvConnectClientSecret)) {
            log.debug("Using Stripe Connect client secret from Key Vault");
            return kvConnectClientSecret.trim();
        }

        // Priority 2: Database (fallback for backward compatibility)
        log.debug("Key Vault Connect client secret not found, falling back to database config");
        String dbClientSecret = firstPresentFromDb("connectClientSecret", "clientSecret", "connect_client_secret");
        if (StringUtils.hasText(dbClientSecret)) {
            return dbClientSecret;
        }

        throw new BadRequestException("Stripe Connect client secret is not configured");
    }

    public String requireConnectWebhookSecret() {
        // Priority 1: Key Vault (via application.yml)
        if (StringUtils.hasText(kvConnectWebhookSecret)) {
            log.debug("Using Stripe Connect webhook secret from Key Vault");
            return kvConnectWebhookSecret.trim();
        }

        // Priority 2: Database (fallback for backward compatibility)
        log.debug("Key Vault Connect webhook secret not found, falling back to database config");
        String dbWebhookSecret = firstPresentFromDb("connectWebhookSecret", "connect_webhook_secret");
        if (StringUtils.hasText(dbWebhookSecret)) {
            return dbWebhookSecret;
        }

        throw new BadRequestException("Stripe Connect webhook secret is not configured");
    }

    public String getWebhookUrl() {
        // Priority 1: Key Vault (via application.yml)
        if (StringUtils.hasText(kvWebhookUrl)) {
            return kvWebhookUrl.trim();
        }

        // Priority 2: Database (fallback for backward compatibility)
        String dbWebhookUrl = firstPresentFromDb("webhookUrl");
        if (StringUtils.hasText(dbWebhookUrl)) {
            return dbWebhookUrl;
        }

        // Default fallback
        return "https://api.therapyflow.pro/api/v1/stripe/webhook/connect";
    }

    /**
     * Load value from database config (fallback for backward compatibility).
     * This method is used when Key Vault values are not configured.
     */
    private String firstPresentFromDb(String... keys) {
        Map<String, Object> config = loadStripeConfigFromDb();
        for (String key : keys) {
            // Try plain value first
            Object value = config.get(key);
            if (value instanceof String str && StringUtils.hasText(str)) {
                return str.trim();
            }
            // Try encrypted value
            Object encrypted = config.get(key + "Encrypted");
            if (encrypted instanceof String enc && StringUtils.hasText(enc)) {
                try {
                    return encryptionService.decrypt(enc).trim();
                } catch (RuntimeException ex) {
                    log.warn("Failed to decrypt Stripe config key '{}': {}", key, ex.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Load Stripe configuration from database.
     * This is a fallback mechanism for backward compatibility.
     */
    private Map<String, Object> loadStripeConfigFromDb() {
        Optional<PlatformIntegrationConfig> row = integrationRepository.findByIntegrationKey(STRIPE_INTEGRATION_KEY);
        if (row.isEmpty() || !StringUtils.hasText(row.get().getConfigJson())) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(row.get().getConfigJson(), new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Failed to parse Stripe config from database: {}", ex.getMessage());
            return Map.of();
        }
    }
}

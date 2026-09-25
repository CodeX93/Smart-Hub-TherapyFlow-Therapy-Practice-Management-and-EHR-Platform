package com.smart.therapy.flow.payment.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.service.EncryptionService;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.payment.dto.TenantStripeConfigResponse;
import com.smart.therapy.flow.payment.dto.TenantStripeConfigUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StripeTenantConfigService {

    private final OrganisationRepository organisationRepository;
    private final EncryptionService encryptionService;

    @Transactional(readOnly = true)
    public TenantStripeConfigResponse getConfig(Long organisationId) {
        Organisation org = requireOrganisation(organisationId);
        return toResponse(org);
    }

    @Transactional
    public TenantStripeConfigResponse upsertConfig(Long organisationId, TenantStripeConfigUpsertRequest request) {
        Organisation org = requireOrganisation(organisationId);
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }

        if (request.getPublishableKey() != null) {
            String publishable = normalizeNullable(request.getPublishableKey());
            validatePublishableKey(publishable);
            org.setStripePublishableKey(publishable);
        }

        if (request.getSecretKey() != null) {
            String secret = normalizeNullable(request.getSecretKey());
            validateSecretKey(secret);
            org.setStripeSecretKeyEncrypted(StringUtils.hasText(secret)
                    ? encryptionService.encrypt(secret)
                    : null);
        }

        if (request.getWebhookSecret() != null) {
            String webhookSecret = normalizeNullable(request.getWebhookSecret());
            validateWebhookSecret(webhookSecret);
            org.setStripeWebhookSecretEncrypted(StringUtils.hasText(webhookSecret)
                    ? encryptionService.encrypt(webhookSecret)
                    : null);
        }

        if (request.getWebhookEndpointUrl() != null) {
            String webhookUrl = normalizeNullable(request.getWebhookEndpointUrl());
            validateWebhookUrl(webhookUrl);
            org.setStripeWebhookEndpointUrl(webhookUrl);
        }

        Organisation saved = organisationRepository.save(org);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<String> findTenantSecretKey(Long organisationId) {
        Organisation org = requireOrganisation(organisationId);
        return decryptOptional(org.getStripeSecretKeyEncrypted());
    }

    @Transactional(readOnly = true)
    public Optional<String> findTenantPublishableKey(Long organisationId) {
        Organisation org = requireOrganisation(organisationId);
        return Optional.ofNullable(normalizeNullable(org.getStripePublishableKey()));
    }

    @Transactional(readOnly = true)
    public Optional<String> findTenantWebhookSecret(Long organisationId) {
        Organisation org = requireOrganisation(organisationId);
        return decryptOptional(org.getStripeWebhookSecretEncrypted());
    }

    @Transactional(readOnly = true)
    public String requireTenantWebhookSecret(Long organisationId) {
        return findTenantWebhookSecret(organisationId)
                .orElseThrow(() -> new BadRequestException("Tenant Stripe webhook secret is not configured"));
    }

    @Transactional(readOnly = true)
    public Organisation requireBySlugOrSubdomain(String tenantKey) {
        if (!StringUtils.hasText(tenantKey)) {
            throw new BadRequestException("Tenant key is required");
        }
        String normalized = tenantKey.trim().toLowerCase(Locale.ROOT);
        return organisationRepository.findBySlug(normalized)
                .or(() -> organisationRepository.findBySubdomain(normalized))
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
    }

    private static String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static void validatePublishableKey(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        if (!(key.startsWith("pk_") || key.startsWith("rk_"))) {
            throw new BadRequestException("Stripe publishable key must start with pk_ (or rk_ for restricted keys)");
        }
        if (key.length() > 255) {
            throw new BadRequestException("Stripe publishable key is too long");
        }
    }

    private static void validateSecretKey(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        if (!(key.startsWith("sk_") || key.startsWith("rk_"))) {
            throw new BadRequestException("Stripe secret key must start with sk_ (or rk_ for restricted keys)");
        }
    }

    private static void validateWebhookSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            return;
        }
        if (!secret.startsWith("whsec_")) {
            throw new BadRequestException("Stripe webhook secret must start with whsec_");
        }
    }

    private static void validateWebhookUrl(String webhookUrl) {
        if (!StringUtils.hasText(webhookUrl)) {
            return;
        }
        String value = webhookUrl.trim();
        if (!value.startsWith("https://")) {
            throw new BadRequestException("Webhook endpoint URL must start with https://");
        }
        if (value.length() > 500) {
            throw new BadRequestException("Webhook endpoint URL is too long");
        }
    }

    private Optional<String> decryptOptional(String encrypted) {
        if (!StringUtils.hasText(encrypted)) {
            return Optional.empty();
        }
        try {
            String decrypted = encryptionService.decrypt(encrypted);
            return StringUtils.hasText(decrypted) ? Optional.of(decrypted.trim()) : Optional.empty();
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private TenantStripeConfigResponse toResponse(Organisation org) {
        return TenantStripeConfigResponse.builder()
                .organisationId(org.getId())
                .publishableKey(normalizeNullable(org.getStripePublishableKey()))
                .secretKeyConfigured(StringUtils.hasText(org.getStripeSecretKeyEncrypted()))
                .webhookEndpointUrl(normalizeNullable(org.getStripeWebhookEndpointUrl()))
                .webhookSecretConfigured(StringUtils.hasText(org.getStripeWebhookSecretEncrypted()))
                .lastUpdatedAt(org.getUpdatedAt())
                .build();
    }

    private Organisation requireOrganisation(Long organisationId) {
        if (organisationId == null) {
            throw new BadRequestException("Organisation id is required");
        }
        return organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found"));
    }
}


package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.organisation.entity.TenantFeature;
import com.smart.therapy.flow.organisation.repository.TenantFeatureRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Feature flags per organisation (ADVANCED_BILLING, beta features).
 * For ADVANCED_BILLING, plan entitlement is the baseline and tenant_features acts as explicit override when present.
 * For non-core tenant features, when no row exists, isFeatureEnabled returns defaultForAbsent.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantFeatureService {

    public static final String FEATURE_ADVANCED_BILLING = "ADVANCED_BILLING";

    private final TenantFeatureRepository repository;
    private final SubscriptionFeatureService subscriptionFeatureService;

    /**
     * Returns true if the feature is enabled for the organisation.
     * When no row exists, returns defaultForAbsent (e.g. true so existing orgs keep SSO without migration).
     */
    public boolean isFeatureEnabled(Long organisationId, String featureKey, boolean defaultForAbsent) {
        if (organisationId == null || featureKey == null || featureKey.isBlank()) {
            return defaultForAbsent;
        }
        String normalizedKey = featureKey.trim().toUpperCase();

        // ADVANCED_BILLING uses subscription entitlement baseline; tenant_features is override when explicitly set.
        if (FEATURE_ADVANCED_BILLING.equals(normalizedKey)) {
            boolean baseline = subscriptionFeatureService.isFeatureEnabled(organisationId, normalizedKey, null);
            return repository.findByOrganisationIdAndFeatureKey(organisationId, normalizedKey)
                    .map(TenantFeature::getEnabled)
                    .orElse(baseline);
        }

        return repository.findByOrganisationIdAndFeatureKey(organisationId, normalizedKey)
                .map(TenantFeature::getEnabled)
                .orElse(defaultForAbsent);
    }

    /** Advanced billing: when no flag exists, treat as disabled (opt-in). */
    public boolean isAdvancedBillingEnabled(Long organisationId) {
        return isFeatureEnabled(organisationId, FEATURE_ADVANCED_BILLING, false);
    }

    public List<TenantFeature> getFeaturesForOrganisation(Long organisationId) {
        return repository.findByOrganisationId(organisationId);
    }

    /** Set or update a feature flag. Creates row if absent. */
    @Transactional
    public TenantFeature setFeature(Long organisationId, String featureKey, boolean enabled) {
        String key = featureKey != null ? featureKey.trim().toUpperCase() : "";
        TenantFeature f = repository.findByOrganisationIdAndFeatureKey(organisationId, key)
                .orElseGet(() -> {
                    TenantFeature n = new TenantFeature();
                    n.setOrganisationId(organisationId);
                    n.setFeatureKey(key);
                    return n;
                });
        f.setEnabled(enabled);
        return repository.save(f);
    }

    /** Set multiple features at once (e.g. from super-admin UI). Keys in map are feature_key, value is enabled. */
    @Transactional
    public List<TenantFeature> setFeatures(Long organisationId, Map<String, Boolean> features) {
        if (features == null || features.isEmpty()) {
            return getFeaturesForOrganisation(organisationId);
        }
        return features.entrySet().stream()
                .map(e -> setFeature(organisationId, e.getKey(), Boolean.TRUE.equals(e.getValue())))
                .collect(Collectors.toList());
    }
}

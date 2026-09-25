package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.AppFeature;
import com.smart.therapy.flow.subscription.entity.FeaturePricing;
import com.smart.therapy.flow.subscription.entity.OrgAddonBillingLineItem;
import com.smart.therapy.flow.subscription.entity.OrgFeaturePurchase;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import com.smart.therapy.flow.subscription.enums.AddonCatalogStatus;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.feature.FeatureScope;
import com.smart.therapy.flow.subscription.feature.FeatureType;
import com.smart.therapy.flow.subscription.repository.AppFeatureRepository;
import com.smart.therapy.flow.subscription.repository.FeaturePricingRepository;
import com.smart.therapy.flow.subscription.repository.OrgAddonBillingLineItemRepository;
import com.smart.therapy.flow.subscription.repository.OrgFeaturePurchaseRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.repository.PlanFeatureVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminAddonService {

    private final AppFeatureRepository appFeatureRepository;
    private final FeaturePricingRepository featurePricingRepository;
    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final OrgFeaturePurchaseRepository orgFeaturePurchaseRepository;
    private final OrgAddonBillingLineItemRepository orgAddonBillingLineItemRepository;
    private final OrganisationRepository organisationRepository;
    private final PlatformAuditService platformAuditService;
    private final PlanFeatureVersionRepository planFeatureVersionRepository;

    @Transactional(readOnly = true)
    public List<CatalogItem> listCatalog() {
        Map<Long, FeaturePricing> pricingByFeatureId = featurePricingRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getFeature().getId(), p -> p, (a, b) -> b));
        return appFeatureRepository.findAll().stream()
                .sorted(Comparator.comparing(AppFeature::getCode))
                .map(feature -> {
                    FeaturePricing price = pricingByFeatureId.get(feature.getId());
                    return new CatalogItem(
                            feature.getCode(),
                            feature.getName(),
                            feature.getDescription(),
                            price != null ? price.getPricePerUnit() : null,
                            price != null ? price.getBillingCycle() : null,
                            price != null ? price.getUnitValue() : null,
                            price != null ? price.getStatus() : null
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogItem> listCatalogEntries() {
        return listCatalog().stream()
                .filter(item -> item.pricePerUnit() != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogItem getCatalogEntry(String code) {
        if (code == null || code.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "code is required");
        }
        AppFeature feature = appFeatureRepository.findByCode(code.trim().toUpperCase(Locale.ROOT)).orElse(null);
        if (feature == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ADDON_NOT_FOUND", "Add-on not found");
        }
        FeaturePricing pricing = featurePricingRepository.findByFeatureId(feature.getId()).orElse(null);
        if (pricing == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ADDON_NOT_FOUND", "Add-on not found");
        }
        return new CatalogItem(
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                pricing.getPricePerUnit(),
                pricing.getBillingCycle(),
                pricing.getUnitValue(),
                pricing.getStatus()
        );
    }

    @Transactional
    public CatalogItem createCatalogEntry(String code,
                                          String name,
                                          String description,
                                          BigDecimal pricePerUnit,
                                          AddonBillingCycle billingCycle,
                                          AddonCatalogStatus status,
                                          Long actorAuthId) {
        if (code == null || code.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "code is required");
        }
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        AppFeature existing = appFeatureRepository.findByCode(normalizedCode).orElse(null);
        if (existing != null && featurePricingRepository.findByFeatureId(existing.getId()).isPresent()) {
            throw new StoryApiException(HttpStatus.CONFLICT, "ADDON_CODE_EXISTS", "Add-on code already exists");
        }
        return upsertCatalogEntry(normalizedCode, name, description, pricePerUnit, billingCycle, status, actorAuthId);
    }

    @Transactional
    public CatalogItem upsertCatalogEntry(String code,
                                          String name,
                                          String description,
                                          BigDecimal pricePerUnit,
                                          AddonBillingCycle billingCycle,
                                          AddonCatalogStatus status,
                                          Long actorAuthId) {
        if (code == null || code.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "code is required");
        }
        if (name == null || name.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name is required");
        }
        if (pricePerUnit == null || pricePerUnit.compareTo(BigDecimal.ZERO) < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "pricePerUnit must be >= 0");
        }
        if (billingCycle == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "billingCycle is required");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        AddonCatalogStatus resolvedStatus = status != null ? status : AddonCatalogStatus.ACTIVE;

        CoreFeature coreFeature = CoreFeature.fromCode(normalizedCode);
        AppFeature feature = coreFeature != null ? findOrCreateFeature(coreFeature)
                : appFeatureRepository.findByCode(normalizedCode).orElseGet(() ->
                        appFeatureRepository.save(AppFeature.builder()
                                .code(normalizedCode)
                                .name(name.trim())
                                .description(description)
                                .type(FeatureType.CUSTOM)
                                .scope(FeatureScope.TENANT)
                                .defaultEnabled(false)
                                .build()));

        if (name != null && !name.isBlank()) {
            feature.setName(name.trim());
        }
        if (description != null) {
            feature.setDescription(description);
        }
        feature = appFeatureRepository.save(feature);

        FeaturePricing pricing = featurePricingRepository.findByFeatureId(feature.getId()).orElseGet(FeaturePricing::new);
        pricing.setFeature(feature);
        pricing.setPricePerUnit(pricePerUnit);
        pricing.setBillingCycle(billingCycle);
        pricing.setUnitValue(1);
        pricing.setStatus(resolvedStatus);
        pricing = featurePricingRepository.save(pricing);

        platformAuditService.log(
                actorAuthId,
                "ADDON_CATALOG_UPSERTED",
                "AppFeature",
                feature.getCode(),
                "pricePerUnit=" + pricing.getPricePerUnit() + ", billingCycle=" + pricing.getBillingCycle() +
                        ", unitValue=" + pricing.getUnitValue() + ", status=" + pricing.getStatus()
        );
        return new CatalogItem(
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                pricing.getPricePerUnit(),
                pricing.getBillingCycle(),
                pricing.getUnitValue(),
                pricing.getStatus()
        );
    }

    @Transactional
    public CatalogItem patchCatalogEntry(String code,
                                         String name,
                                         String description,
                                         BigDecimal pricePerUnit,
                                         AddonBillingCycle billingCycle,
                                         AddonCatalogStatus status,
                                         Long actorAuthId) {
        if (code == null || code.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "code is required");
        }
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        AppFeature feature = appFeatureRepository.findByCode(normalizedCode).orElse(null);
        if (feature == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ADDON_NOT_FOUND", "Add-on not found");
        }
        FeaturePricing pricing = featurePricingRepository.findByFeatureId(feature.getId()).orElse(null);
        if (pricing == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ADDON_NOT_FOUND", "Add-on not found");
        }
        if (pricePerUnit != null && pricePerUnit.compareTo(BigDecimal.ZERO) < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "pricePerUnit must be >= 0");
        }

        if (name != null && !name.isBlank()) {
            feature.setName(name.trim());
        }
        if (description != null) {
            feature.setDescription(description);
        }
        feature = appFeatureRepository.save(feature);

        if (pricePerUnit != null) {
            pricing.setPricePerUnit(pricePerUnit);
        }
        if (billingCycle != null) {
            pricing.setBillingCycle(billingCycle);
        }
        if (status != null) {
            pricing.setStatus(status);
        }
        pricing = featurePricingRepository.save(pricing);

        platformAuditService.log(
                actorAuthId,
                "ADDON_CATALOG_PATCHED",
                "AppFeature",
                feature.getCode(),
                "pricePerUnit=" + pricing.getPricePerUnit() + ", billingCycle=" + pricing.getBillingCycle() +
                        ", unitValue=" + pricing.getUnitValue() + ", status=" + pricing.getStatus()
        );
        return new CatalogItem(
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                pricing.getPricePerUnit(),
                pricing.getBillingCycle(),
                pricing.getUnitValue(),
                pricing.getStatus()
        );
    }

    @Transactional
    public CatalogItem activateCatalogEntry(String code, Long actorAuthId) {
        CatalogItem current = getCatalogEntry(code);
        AppFeature feature = appFeatureRepository.findByCode(current.featureCode()).orElseThrow();
        FeaturePricing pricing = featurePricingRepository.findByFeatureId(feature.getId()).orElseThrow();
        pricing.setStatus(AddonCatalogStatus.ACTIVE);
        pricing = featurePricingRepository.save(pricing);

        platformAuditService.log(
                actorAuthId,
                "ADDON_CATALOG_ACTIVATED",
                "AppFeature",
                feature.getCode(),
                "status=" + pricing.getStatus()
        );
        return new CatalogItem(
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                pricing.getPricePerUnit(),
                pricing.getBillingCycle(),
                pricing.getUnitValue(),
                pricing.getStatus()
        );
    }

    @Transactional
    public CatalogItem deactivateCatalogEntry(String code, Long actorAuthId) {
        CatalogItem current = getCatalogEntry(code);
        AppFeature feature = appFeatureRepository.findByCode(current.featureCode()).orElseThrow();
        FeaturePricing pricing = featurePricingRepository.findByFeatureId(feature.getId()).orElseThrow();
        pricing.setStatus(AddonCatalogStatus.INACTIVE);
        pricing = featurePricingRepository.save(pricing);

        platformAuditService.log(
                actorAuthId,
                "ADDON_CATALOG_DEACTIVATED",
                "AppFeature",
                feature.getCode(),
                "status=" + pricing.getStatus()
        );
        return new CatalogItem(
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                pricing.getPricePerUnit(),
                pricing.getBillingCycle(),
                pricing.getUnitValue(),
                pricing.getStatus()
        );
    }

    /**
     * Permanently removes the add-on from the catalog (hard delete).
     * Archive/deactivate continues to use {@link #deactivateCatalogEntry}.
     */
    @Transactional
    public void deleteCatalogEntryPermanently(String code, Long actorAuthId) {
        CatalogItem current = getCatalogEntry(code);
        AppFeature feature = appFeatureRepository.findByCode(current.featureCode()).orElseThrow();
        FeaturePricing pricing = featurePricingRepository.findByFeatureId(feature.getId()).orElseThrow();

        long activePurchases = orgFeaturePurchaseRepository.countActiveByFeatureId(feature.getId(), Instant.now());
        if (activePurchases > 0) {
            throw new StoryApiException(HttpStatus.CONFLICT, "ADDON_IN_USE",
                    "Cannot permanently delete this add-on while organisations still have active purchases. Archive it instead, or end those purchases first.");
        }

        long historicalPurchases = orgFeaturePurchaseRepository.countByFeatureId(feature.getId());
        long planLinks = planFeatureVersionRepository.countByFeatureId(feature.getId());
        boolean isCoreFeature = feature.getType() == FeatureType.CORE || CoreFeature.fromCode(feature.getCode()) != null;

        featurePricingRepository.delete(pricing);

        boolean deletedFeature = false;
        if (!isCoreFeature && historicalPurchases == 0 && planLinks == 0) {
            appFeatureRepository.delete(feature);
            deletedFeature = true;
        }

        platformAuditService.log(
                actorAuthId,
                "ADDON_CATALOG_DELETED",
                "AppFeature",
                current.featureCode(),
                "hardDelete=true, featureRowDeleted=" + deletedFeature
                        + ", historicalPurchases=" + historicalPurchases
                        + ", planLinks=" + planLinks
        );
    }

    @Transactional
    public CatalogItem upsertCatalogPrice(String featureCode,
                                          BigDecimal pricePerUnit,
                                          AddonBillingCycle billingCycle,
                                          Integer unitValue,
                                          AddonCatalogStatus status,
                                          Long actorAuthId) {
        CoreFeature coreFeature = CoreFeature.fromCode(featureCode);
        if (coreFeature == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_CODE", "Unknown featureCode: " + featureCode);
        }
        if (pricePerUnit == null || pricePerUnit.compareTo(BigDecimal.ZERO) < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "pricePerUnit must be >= 0");
        }
        if (unitValue == null || unitValue < 1) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NEGATIVE_QUANTITY", "unitValue must be >= 1");
        }
        if (billingCycle == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "billingCycle is required");
        }
        AddonCatalogStatus resolvedStatus = status != null ? status : AddonCatalogStatus.ACTIVE;

        AppFeature feature = findOrCreateFeature(coreFeature);
        FeaturePricing pricing = featurePricingRepository.findByFeatureId(feature.getId()).orElseGet(FeaturePricing::new);
        pricing.setFeature(feature);
        pricing.setPricePerUnit(pricePerUnit);
        pricing.setBillingCycle(billingCycle);
        pricing.setUnitValue(unitValue);
        pricing.setStatus(resolvedStatus);
        pricing = featurePricingRepository.save(pricing);

        platformAuditService.log(
                actorAuthId,
                "ADDON_CATALOG_UPSERTED",
                "AppFeature",
                coreFeature.getCode(),
                "pricePerUnit=" + pricing.getPricePerUnit() + ", billingCycle=" + pricing.getBillingCycle() +
                        ", unitValue=" + pricing.getUnitValue() + ", status=" + pricing.getStatus()
        );
        return new CatalogItem(
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                pricing.getPricePerUnit(),
                pricing.getBillingCycle(),
                pricing.getUnitValue(),
                pricing.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public List<OrgAddonItem> listOrganisationAddons(Long organisationId) {
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId).orElse(null);
        if (sub == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No current subscription for organisation");
        }
        Instant now = Instant.now();
        return orgFeaturePurchaseRepository.findActiveBySubscriptionIdAt(sub.getId(), now).stream()
                .map(p -> new OrgAddonItem(
                        p.getId(),
                        p.getFeature().getCode(),
                        p.getFeature().getName(),
                        p.getQuantity(),
                        p.getPricePerUnitAtTime(),
                        resolveUnitValue(p),
                        resolveBillingCycle(p),
                        p.getStartAt(),
                        p.getEndAt()
                ))
                .toList();
    }

    @Transactional
    public OrgAddonItem assignAddon(Long organisationId,
                                    String featureCode,
                                    Integer quantityInput,
                                    Long actorAuthId) {
        if (!organisationRepository.existsById(organisationId)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId).orElse(null);
        if (sub == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No current subscription for organisation");
        }

        String normalizedCode = featureCode != null ? featureCode.trim().toUpperCase(Locale.ROOT) : null;
        CoreFeature coreFeature = CoreFeature.fromCode(normalizedCode);
        AppFeature feature = coreFeature != null ? findOrCreateFeature(coreFeature)
                : appFeatureRepository.findByCode(normalizedCode).orElse(null);
        if (feature == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_CODE", "Unknown featureCode: " + featureCode);
        }

        int quantity = quantityInput != null ? quantityInput : 1;
        if (quantity < 1) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NEGATIVE_QUANTITY", "quantity must be >= 1");
        }

        FeaturePricing pricing = featurePricingRepository.findByFeatureId(feature.getId()).orElse(null);
        if (pricing == null || pricing.getStatus() == AddonCatalogStatus.INACTIVE) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PLAN_NOT_FOUND", "Add-on catalog entry missing or inactive");
        }
        BigDecimal price = pricing.getPricePerUnit() != null ? pricing.getPricePerUnit() : BigDecimal.ZERO;
        int unitValue = pricing.getUnitValue() != null ? pricing.getUnitValue() : 1;
        Instant now = Instant.now();

        OrgFeaturePurchase purchase = new OrgFeaturePurchase();
        purchase.setSubscription(sub);
        purchase.setFeature(feature);
        purchase.setQuantity(quantity);
        purchase.setPricePerUnitAtTime(price);
        purchase.setStartAt(now);
        purchase.setEndAt(null);
        purchase.setCreatedAt(now);
        purchase = orgFeaturePurchaseRepository.save(purchase);

        BigDecimal totalAmount = price.multiply(BigDecimal.valueOf((long) quantity * unitValue));
        orgAddonBillingLineItemRepository.save(OrgAddonBillingLineItem.builder()
                .organisationId(organisationId)
                .subscriptionId(sub.getId())
                .orgFeaturePurchaseId(purchase.getId())
                .featureCode(feature.getCode())
                .quantity(quantity)
                .unitValue(unitValue)
                .unitPriceUsd(price)
                .totalAmountUsd(totalAmount)
                .billingCycle(pricing.getBillingCycle())
                .createdBy(actorAuthId)
                .createdAt(now)
                .build());

        platformAuditService.log(
                actorAuthId,
                "ORG_ADDON_ASSIGNED",
                "Organisation",
                String.valueOf(organisationId),
                "feature=" + feature.getCode() + ", quantity=" + quantity + ", price=" + price +
                        ", unitValue=" + unitValue + ", total=" + totalAmount + ", billingCycle=" + pricing.getBillingCycle()
        );

        return new OrgAddonItem(
                purchase.getId(),
                feature.getCode(),
                feature.getName(),
                purchase.getQuantity(),
                purchase.getPricePerUnitAtTime(),
                unitValue,
                pricing.getBillingCycle(),
                purchase.getStartAt(),
                purchase.getEndAt()
        );
    }

    /**
     * Ends an active org add-on purchase ({@code endAt = now}). Soft unassign — keeps history.
     */
    @Transactional
    public OrgAddonItem unassignAddon(Long organisationId, Long purchaseId, Long actorAuthId) {
        if (!organisationRepository.existsById(organisationId)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found");
        }
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId).orElse(null);
        if (sub == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No current subscription for organisation");
        }

        OrgFeaturePurchase purchase = orgFeaturePurchaseRepository.findById(purchaseId).orElse(null);
        if (purchase == null
                || purchase.getSubscription() == null
                || !purchase.getSubscription().getId().equals(sub.getId())) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ADDON_PURCHASE_NOT_FOUND",
                    "Add-on purchase not found for this organisation");
        }

        Instant now = Instant.now();
        if (purchase.getEndAt() != null && !purchase.getEndAt().isAfter(now)) {
            throw new StoryApiException(HttpStatus.CONFLICT, "ADDON_ALREADY_ENDED",
                    "This add-on purchase is already ended");
        }

        purchase.setEndAt(now);
        purchase = orgFeaturePurchaseRepository.save(purchase);

        platformAuditService.log(
                actorAuthId,
                "ORG_ADDON_UNASSIGNED",
                "Organisation",
                String.valueOf(organisationId),
                "purchaseId=" + purchase.getId()
                        + ", feature=" + (purchase.getFeature() != null ? purchase.getFeature().getCode() : null)
                        + ", endAt=" + now
        );

        return new OrgAddonItem(
                purchase.getId(),
                purchase.getFeature() != null ? purchase.getFeature().getCode() : null,
                purchase.getFeature() != null ? purchase.getFeature().getName() : null,
                purchase.getQuantity(),
                purchase.getPricePerUnitAtTime(),
                resolveUnitValue(purchase),
                resolveBillingCycle(purchase),
                purchase.getStartAt(),
                purchase.getEndAt()
        );
    }

    /**
     * Ends all active purchases of a feature code for the organisation's current subscription.
     */
    @Transactional
    public List<OrgAddonItem> unassignAddonByFeatureCode(Long organisationId, String featureCode, Long actorAuthId) {
        if (featureCode == null || featureCode.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "featureCode is required");
        }
        String normalized = featureCode.trim().toUpperCase(Locale.ROOT);
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId).orElse(null);
        if (sub == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No current subscription for organisation");
        }
        Instant now = Instant.now();
        List<OrgFeaturePurchase> active = orgFeaturePurchaseRepository.findActiveBySubscriptionIdAt(sub.getId(), now).stream()
                .filter(p -> p.getFeature() != null && normalized.equalsIgnoreCase(p.getFeature().getCode()))
                .toList();
        if (active.isEmpty()) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ADDON_PURCHASE_NOT_FOUND",
                    "No active add-on purchase found for featureCode=" + normalized);
        }
        List<OrgAddonItem> ended = new java.util.ArrayList<>();
        for (OrgFeaturePurchase purchase : active) {
            ended.add(unassignAddon(organisationId, purchase.getId(), actorAuthId));
        }
        return ended;
    }

    public record CatalogItem(String featureCode, String featureName, String description, BigDecimal pricePerUnit,
                              AddonBillingCycle billingCycle, Integer unitValue, AddonCatalogStatus status) {
    }

    public record OrgAddonItem(Long purchaseId, String featureCode, String featureName, Integer quantity,
                               BigDecimal pricePerUnitAtTime, Integer unitValue,
                               AddonBillingCycle billingCycle, Instant startAt, Instant endAt) {
    }

    private AppFeature findOrCreateFeature(CoreFeature coreFeature) {
        return appFeatureRepository.findByCode(coreFeature.getCode())
                .orElseGet(() -> appFeatureRepository.save(AppFeature.builder()
                        .code(coreFeature.getCode())
                        .name(coreFeature.getCode().replace('_', ' '))
                        .description(coreFeature.getDescription())
                        .type(com.smart.therapy.flow.subscription.feature.FeatureType.CORE)
                        .scope(com.smart.therapy.flow.subscription.feature.FeatureScope.TENANT)
                        .defaultEnabled(coreFeature.getCatalogDefaultEnabled())
                        .build()));
    }

    private AddonBillingCycle resolveBillingCycle(OrgFeaturePurchase purchase) {
        return featurePricingRepository.findByFeatureId(purchase.getFeature().getId())
                .map(FeaturePricing::getBillingCycle)
                .orElse(AddonBillingCycle.MONTHLY);
    }

    private Integer resolveUnitValue(OrgFeaturePurchase purchase) {
        return featurePricingRepository.findByFeatureId(purchase.getFeature().getId())
                .map(FeaturePricing::getUnitValue)
                .orElse(1);
    }
}

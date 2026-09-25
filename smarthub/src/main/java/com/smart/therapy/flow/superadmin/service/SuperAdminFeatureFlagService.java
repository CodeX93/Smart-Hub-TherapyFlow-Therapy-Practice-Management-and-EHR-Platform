package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.subscription.entity.AppFeature;
import com.smart.therapy.flow.subscription.repository.AppFeatureRepository;
import com.smart.therapy.flow.subscription.repository.OrgFeatureOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SuperAdminFeatureFlagService {

    private static final String CONFIRM_KEYWORD = "CONFIRM";

    private final AppFeatureRepository appFeatureRepository;
    private final OrgFeatureOverrideRepository orgFeatureOverrideRepository;
    private final OrganisationRepository organisationRepository;
    private final PlatformAuditService platformAuditService;

    @Transactional
    public Map<String, Object> toggleFeature(String key, boolean enabled, Long actorAuthId) {
        if (!StringUtils.hasText(key)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "flag key is required");
        }
        AppFeature feature = appFeatureRepository.findByCodeIgnoreCase(key.trim())
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "feature not found"));
        if (Boolean.TRUE.equals(feature.getIsDeleted()) || Boolean.TRUE.equals(feature.getIsDeprecated())) {
            throw new StoryApiException(HttpStatus.CONFLICT, "FEATURE_INACTIVE", "feature is deprecated or deleted");
        }
        feature.setDefaultEnabled(enabled);
        appFeatureRepository.save(feature);
        platformAuditService.log(
                actorAuthId,
                "FEATURE_FLAG_TOGGLED",
                "AppFeature",
                String.valueOf(feature.getId()),
                "key=" + feature.getCode() + ", enabled=" + enabled
        );
        return Map.of(
                "key", feature.getCode(),
                "enabled", feature.getDefaultEnabled()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewToggle(String key) {
        if (!StringUtils.hasText(key)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "flag key is required");
        }
        AppFeature feature = appFeatureRepository.findByCodeIgnoreCase(key.trim())
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "feature not found"));
        long overrideCount = orgFeatureOverrideRepository.countByFeatureKeyIgnoreCase(feature.getCode());
        long orgCount = organisationRepository.count();
        return Map.of(
                "key", feature.getCode(),
                "currentDefaultEnabled", feature.getDefaultEnabled(),
                "overridesCount", overrideCount,
                "totalOrganisations", orgCount
        );
    }

    @Transactional
    public Map<String, Object> disableAllFeatures(String confirmation, Long actorAuthId) {
        if (!StringUtils.hasText(confirmation) || !CONFIRM_KEYWORD.equals(confirmation.trim().toUpperCase(Locale.ROOT))) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "CONFIRMATION_REQUIRED", "confirmation must be CONFIRM");
        }
        int updated = appFeatureRepository.disableAllActiveFeatures();
        platformAuditService.log(
                actorAuthId,
                "FEATURE_FLAGS_DISABLED_ALL",
                "AppFeature",
                "ALL",
                "disabledCount=" + updated
        );
        return Map.of(
                "disabledCount", updated,
                "status", "disabled"
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewDisableAll() {
        long activeFeatures = appFeatureRepository.countByIsDeletedFalseAndIsDeprecatedFalse();
        long orgCount = organisationRepository.count();
        return Map.of(
                "activeFeatures", activeFeatures,
                "totalOrganisations", orgCount
        );
    }
}

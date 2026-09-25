package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.AppFeature;
import com.smart.therapy.flow.subscription.entity.FeaturePricing;
import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import com.smart.therapy.flow.subscription.enums.AddonCatalogStatus;
import com.smart.therapy.flow.subscription.feature.FeatureScope;
import com.smart.therapy.flow.subscription.feature.FeatureType;
import com.smart.therapy.flow.subscription.repository.AppFeatureRepository;
import com.smart.therapy.flow.subscription.repository.FeaturePricingRepository;
import com.smart.therapy.flow.subscription.repository.OrgAddonBillingLineItemRepository;
import com.smart.therapy.flow.subscription.repository.OrgFeaturePurchaseRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.repository.PlanFeatureVersionRepository;
import com.smart.therapy.flow.superadmin.service.SuperAdminAddonService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SuperAdminAddonServiceTest {

    @Test
    void createCatalogEntryRejectsNegativePrice() {
        SuperAdminAddonService service = new SuperAdminAddonService(
                mock(AppFeatureRepository.class),
                mock(FeaturePricingRepository.class),
                mock(OrgSubscriptionRepository.class),
                mock(OrgFeaturePurchaseRepository.class),
                mock(OrgAddonBillingLineItemRepository.class),
                mock(OrganisationRepository.class),
                mock(PlatformAuditService.class),
                mock(PlanFeatureVersionRepository.class)
        );

        StoryApiException ex = assertThrows(StoryApiException.class, () ->
                service.createCatalogEntry("extra_storage", "Extra Storage", null,
                        new BigDecimal("-1"), AddonBillingCycle.MONTHLY, AddonCatalogStatus.ACTIVE, 1L)
        );
        assertEquals("VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void patchCatalogEntryUpdatesPriceAndStatus() {
        AppFeature feature = AppFeature.builder()
                .id(10L)
                .code("EXTRA_STORAGE")
                .name("Extra Storage")
                .type(FeatureType.CUSTOM)
                .scope(FeatureScope.TENANT)
                .build();
        FeaturePricing pricing = FeaturePricing.builder()
                .id(20L)
                .feature(feature)
                .pricePerUnit(new BigDecimal("50"))
                .billingCycle(AddonBillingCycle.MONTHLY)
                .status(AddonCatalogStatus.ACTIVE)
                .unitValue(1)
                .build();

        AppFeatureRepository appFeatureRepository = mock(AppFeatureRepository.class);
        FeaturePricingRepository featurePricingRepository = mock(FeaturePricingRepository.class);

        when(appFeatureRepository.findByCode("EXTRA_STORAGE")).thenReturn(Optional.of(feature));
        when(featurePricingRepository.findByFeatureId(10L)).thenReturn(Optional.of(pricing));
        when(appFeatureRepository.save(any(AppFeature.class))).thenAnswer(inv -> inv.getArgument(0));
        when(featurePricingRepository.save(any(FeaturePricing.class))).thenAnswer(inv -> inv.getArgument(0));

        SuperAdminAddonService service = new SuperAdminAddonService(
                appFeatureRepository,
                featurePricingRepository,
                mock(OrgSubscriptionRepository.class),
                mock(OrgFeaturePurchaseRepository.class),
                mock(OrgAddonBillingLineItemRepository.class),
                mock(OrganisationRepository.class),
                mock(PlatformAuditService.class),
                mock(PlanFeatureVersionRepository.class)
        );

        var result = service.patchCatalogEntry("extra_storage", null, null,
                new BigDecimal("75"), AddonBillingCycle.ANNUAL, AddonCatalogStatus.INACTIVE, 1L);

        assertEquals(new BigDecimal("75"), result.pricePerUnit());
        assertEquals(AddonBillingCycle.ANNUAL, result.billingCycle());
        assertEquals(AddonCatalogStatus.INACTIVE, result.status());
    }
}

package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.subscription.entity.OrgFeatureOverride;
import com.smart.therapy.flow.subscription.repository.AppFeatureRepository;
import com.smart.therapy.flow.subscription.repository.FeatureUsageRepository;
import com.smart.therapy.flow.subscription.repository.OrgFeatureOverrideRepository;
import com.smart.therapy.flow.subscription.repository.OrgFeaturePurchaseRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.repository.PlanFeatureVersionRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.superadmin.service.FeatureRolloutService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionFeatureService limit enablement tests")
class SubscriptionFeatureServiceLimitEnablementTest {

    @Mock
    private OrgSubscriptionRepository orgSubscriptionRepository;
    @Mock
    private PlanFeatureVersionRepository planFeatureVersionRepository;
    @Mock
    private OrgFeaturePurchaseRepository orgFeaturePurchaseRepository;
    @Mock
    private AppFeatureRepository appFeatureRepository;
    @Mock
    private FeatureUsageRepository featureUsageRepository;
    @Mock
    private OrgFeatureOverrideRepository orgFeatureOverrideRepository;
    @Mock
    private FeatureRolloutService featureRolloutService;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private SubscriptionFeatureService subscriptionFeatureService;

    @Test
    @DisplayName("Disabled limit entitlement resolves as unlimited")
    void shouldResolveDisabledLimitAsUnlimited() {
        OrgFeatureOverride override = OrgFeatureOverride.builder()
                .enabled(false)
                .usageLimit(3)
                .build();

        when(orgFeatureOverrideRepository.findByOrganisationIdAndFeatureKey(1L, "THERAPIST_LIMIT"))
                .thenReturn(Optional.of(override));

        Integer limit = subscriptionFeatureService.getEffectiveLimit(
                1L,
                SubscriptionFeatureService.FEATURE_THERAPIST_SEATS,
                Instant.now());

        assertThat(limit).isNull();
    }

    @Test
    @DisplayName("Enabled limit entitlement still returns its numeric cap")
    void shouldResolveEnabledLimitAsNumericCap() {
        OrgFeatureOverride override = OrgFeatureOverride.builder()
                .enabled(true)
                .usageLimit(3)
                .build();

        when(orgFeatureOverrideRepository.findByOrganisationIdAndFeatureKey(1L, "THERAPIST_LIMIT"))
                .thenReturn(Optional.of(override));

        Integer limit = subscriptionFeatureService.getEffectiveLimit(
                1L,
                SubscriptionFeatureService.FEATURE_THERAPIST_SEATS,
                Instant.now());

        assertThat(limit).isEqualTo(3);
    }
}

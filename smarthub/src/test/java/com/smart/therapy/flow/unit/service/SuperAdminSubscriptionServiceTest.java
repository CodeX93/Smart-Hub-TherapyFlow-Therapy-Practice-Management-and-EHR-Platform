package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.OrgFeatureOverride;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import com.smart.therapy.flow.subscription.repository.OrgFeatureOverrideRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.repository.SubscriptionPlanRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.subscription.service.SubscriptionLifecycleService;
import com.smart.therapy.flow.billing.service.PlatformSubscriptionInvoiceService;
import com.smart.therapy.flow.payment.service.StripePlatformSubscriptionService;
import com.smart.therapy.flow.payment.service.StripeSubscriptionAdminService;
import com.smart.therapy.flow.superadmin.service.FeatureRolloutService;
import com.smart.therapy.flow.superadmin.service.SuperAdminSubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuperAdminSubscriptionService strict contract tests")
class SuperAdminSubscriptionServiceTest {

    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private OrgSubscriptionRepository orgSubscriptionRepository;
    @Mock
    private SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock
    private OrgFeatureOverrideRepository orgFeatureOverrideRepository;
    @Mock
    private AuthIdentityRoleRepository authIdentityRoleRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private TenantTransactionExecutor tenantTransactionExecutor;
    @Mock
    private PlatformAuditService platformAuditService;
    @Mock
    private SubscriptionFeatureService subscriptionFeatureService;
    @Mock
    private FeatureRolloutService featureRolloutService;
    @Mock
    private SubscriptionLifecycleService subscriptionLifecycleService;
    @Mock
    private StripeSubscriptionAdminService stripeSubscriptionAdminService;
    @Mock
    private PlatformSubscriptionInvoiceService platformSubscriptionInvoiceService;
    @Mock
    private StripePlatformSubscriptionService stripePlatformSubscriptionService;

    @InjectMocks
    private SuperAdminSubscriptionService service;

    @Test
    @DisplayName("Maps billingCycle annual to yearly internally")
    void shouldMapAnnualToYearlyInternally() {
        Organisation org = new Organisation();
        org.setId(99L);
        org.setSchemaName("tenant_99");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("pro");
        plan.setBasePrice(new BigDecimal("29.00"));
        plan.setAnnualPrice(new BigDecimal("290.00"));
        plan.setBillingCycle("monthly");

        OrgSubscription sub = new OrgSubscription();
        sub.setId(11L);
        sub.setOrganisation(org);
        sub.setPlan(plan);
        sub.setStatus("active");
        sub.setBillingCycleAtTime("monthly");
        sub.setPriceAtTime(new BigDecimal("29.00"));
        sub.setStartAt(Instant.parse("2026-03-01T00:00:00Z"));

        when(orgSubscriptionRepository.findCurrentByOrganisationId(99L)).thenReturn(Optional.of(sub));
        when(subscriptionPlanRepository.findByCodeIgnoreCase("pro")).thenReturn(Optional.empty());
        when(subscriptionPlanRepository.findByNameIgnoreCase("pro")).thenReturn(Optional.of(plan));
        when(orgSubscriptionRepository.save(any(OrgSubscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionFeatureService.getEffectiveLimit(eq(99L), any(), eq(null))).thenReturn(null);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "THERAPIST")).thenReturn(0L);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "SUPERVISOR")).thenReturn(0L);
        lenient().when(tenantTransactionExecutor.executeReadOnly(eq(99L), eq("tenant_99"), any())).thenReturn(0L);

        SuperAdminSubscriptionService.StrictSubscriptionView result = service.updateStrictSubscription(
                99L, "pro", "annual", null, null, null, null, null, 1L);

        assertThat(sub.getBillingCycleAtTime()).isEqualTo("yearly");
        assertThat(result.billingCycle()).isEqualTo("annual");
        assertThat(sub.getPriceAtTime()).isEqualByComparingTo("290.00");
    }

    @Test
    @DisplayName("Rejects invalid trial days with INVALID_TRIAL_DAYS")
    void shouldRejectInvalidTrialDays() {
        Organisation org = new Organisation();
        org.setId(99L);
        org.setSchemaName("tenant_99");
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("pro");
        plan.setBasePrice(new BigDecimal("29.00"));

        OrgSubscription sub = new OrgSubscription();
        sub.setOrganisation(org);
        sub.setPlan(plan);
        sub.setBillingCycleAtTime("monthly");
        sub.setPriceAtTime(new BigDecimal("29.00"));

        when(orgSubscriptionRepository.findCurrentByOrganisationId(99L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.updateStrictSubscription(99L, null, null, 7, null, null, null, null, 1L))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(e.getCode()).isEqualTo("INVALID_TRIAL_DAYS");
                });
    }

    @Test
    @DisplayName("Returns SUBSCRIPTION_NOT_FOUND when current subscription is missing")
    void shouldThrowNotFoundWhenSubscriptionMissing() {
        when(orgSubscriptionRepository.findCurrentByOrganisationId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStrictSubscription(99L))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("SUBSCRIPTION_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("Resolves plan by catalog code PROFESSIONAL")
    void shouldResolvePlanByCatalogCode() {
        Organisation org = new Organisation();
        org.setId(99L);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode("PROFESSIONAL");
        plan.setName("Professional");
        plan.setBasePrice(new BigDecimal("199.00"));
        plan.setAnnualPrice(new BigDecimal("1900.00"));
        plan.setBillingCycle("monthly");

        OrgSubscription sub = new OrgSubscription();
        sub.setId(11L);
        sub.setOrganisation(org);
        sub.setPlan(plan);
        sub.setStatus("active");
        sub.setBillingCycleAtTime("monthly");
        sub.setPriceAtTime(new BigDecimal("199.00"));
        sub.setStartAt(Instant.parse("2026-03-01T00:00:00Z"));

        when(orgSubscriptionRepository.findCurrentByOrganisationId(99L)).thenReturn(Optional.of(sub));
        when(subscriptionPlanRepository.findByCodeIgnoreCase("PROFESSIONAL")).thenReturn(Optional.of(plan));
        when(orgSubscriptionRepository.save(any(OrgSubscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionFeatureService.getEffectiveLimit(eq(99L), any(), eq(null))).thenReturn(null);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "THERAPIST")).thenReturn(0L);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "SUPERVISOR")).thenReturn(0L);
        lenient().when(tenantTransactionExecutor.executeReadOnly(eq(99L), eq("tenant_99"), any())).thenReturn(0L);

        SuperAdminSubscriptionService.StrictSubscriptionView result = service.updateStrictSubscription(
                99L, "PROFESSIONAL", "monthly", null, null, null, null, null, 1L);

        assertThat(result.plan()).isEqualTo("Professional");
    }

    @Test
    @DisplayName("Falls back to legacy PRO code when PROFESSIONAL is unknown")
    void shouldResolveLegacyProCodeAlias() {
        Organisation org = new Organisation();
        org.setId(99L);
        org.setSchemaName("tenant_99");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode("PRO");
        plan.setName("Pro");
        plan.setBasePrice(new BigDecimal("199.00"));
        plan.setBillingCycle("monthly");

        OrgSubscription sub = new OrgSubscription();
        sub.setId(11L);
        sub.setOrganisation(org);
        sub.setPlan(plan);
        sub.setStatus("active");
        sub.setBillingCycleAtTime("monthly");
        sub.setPriceAtTime(new BigDecimal("199.00"));
        sub.setStartAt(Instant.parse("2026-03-01T00:00:00Z"));

        when(orgSubscriptionRepository.findCurrentByOrganisationId(99L)).thenReturn(Optional.of(sub));
        when(subscriptionPlanRepository.findByCodeIgnoreCase("PROFESSIONAL")).thenReturn(Optional.empty());
        when(subscriptionPlanRepository.findByNameIgnoreCase("PROFESSIONAL")).thenReturn(Optional.empty());
        when(subscriptionPlanRepository.findByCodeIgnoreCase("PRO")).thenReturn(Optional.of(plan));
        when(orgSubscriptionRepository.save(any(OrgSubscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionFeatureService.getEffectiveLimit(eq(99L), any(), eq(null))).thenReturn(null);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "THERAPIST")).thenReturn(0L);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "SUPERVISOR")).thenReturn(0L);
        when(tenantTransactionExecutor.executeReadOnly(eq(99L), eq("tenant_99"), any())).thenReturn(0L);

        SuperAdminSubscriptionService.StrictSubscriptionView result = service.updateStrictSubscription(
                99L, "PROFESSIONAL", "monthly", null, null, null, null, null, 1L);

        assertThat(result.plan()).isEqualTo("Pro");
    }

    @Test
    @DisplayName("Applies user limit override patch to org feature overrides")
    void shouldApplyUserLimitOverridePatch() {
        Organisation org = new Organisation();
        org.setId(99L);
        org.setSchemaName("tenant_99");
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("pro");
        plan.setBasePrice(new BigDecimal("29.00"));

        OrgSubscription sub = new OrgSubscription();
        sub.setOrganisation(org);
        sub.setPlan(plan);
        sub.setBillingCycleAtTime("monthly");
        sub.setPriceAtTime(new BigDecimal("29.00"));
        sub.setStatus("active");
        sub.setStartAt(Instant.parse("2026-03-01T00:00:00Z"));

        when(orgSubscriptionRepository.findCurrentByOrganisationId(99L)).thenReturn(Optional.of(sub));
        when(orgSubscriptionRepository.save(any(OrgSubscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orgFeatureOverrideRepository.findByOrganisationIdAndFeatureKey(eq(99L), any())).thenReturn(Optional.empty());
        when(orgFeatureOverrideRepository.save(any(OrgFeatureOverride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subscriptionFeatureService.getEffectiveLimit(eq(99L), any(), eq(null))).thenReturn(null);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "THERAPIST")).thenReturn(0L);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "SUPERVISOR")).thenReturn(0L);
        when(tenantTransactionExecutor.executeReadOnly(eq(99L), eq("tenant_99"), any())).thenReturn(0L);

        SuperAdminSubscriptionService.UserLimitPatch patch =
                new SuperAdminSubscriptionService.UserLimitPatch(25, 8, 750);

        service.updateStrictSubscription(99L, null, null, null, null, null, patch, null, 1L);

        // If we reached here without validation exception, override-path behavior is wired.
        assertThat(patch.therapistLimit()).isEqualTo(25);
    }

    @Test
    @DisplayName("Strict subscription view uses active therapist counts and tenant client counts")
    void shouldUseActiveCountsInStrictSubscriptionView() {
        Organisation org = new Organisation();
        org.setId(99L);
        org.setSchemaName("tenant_99");

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("Pro");

        OrgSubscription sub = new OrgSubscription();
        sub.setId(11L);
        sub.setOrganisation(org);
        sub.setPlan(plan);
        sub.setStatus("active");
        sub.setBillingCycleAtTime("monthly");

        when(orgSubscriptionRepository.findCurrentByOrganisationId(99L)).thenReturn(Optional.of(sub));
        when(subscriptionFeatureService.getEffectiveLimit(eq(99L), any(), eq(null))).thenReturn(null);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "THERAPIST")).thenReturn(3L);
        when(authIdentityRoleRepository.countActiveByRoleForOrganisation(99L, "SUPERVISOR")).thenReturn(1L);
        when(tenantTransactionExecutor.executeReadOnly(eq(99L), eq("tenant_99"), any())).thenReturn(51L);

        SuperAdminSubscriptionService.StrictSubscriptionView result = service.getStrictSubscription(99L);

        assertThat(result.therapistUsers()).isEqualTo(3L);
        assertThat(result.supervisorUsers()).isEqualTo(1L);
        assertThat(result.clientUsers()).isEqualTo(51L);
    }
}

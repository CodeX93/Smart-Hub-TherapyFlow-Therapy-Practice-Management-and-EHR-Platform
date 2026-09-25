package com.smart.therapy.flow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.admin.controller.SuperAdminController;
import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.superadmin.service.SuperAdminAddonService;
import com.smart.therapy.flow.superadmin.service.SuperAdminBillingService;
import com.smart.therapy.flow.superadmin.service.SuperAdminOnboardingService;
import com.smart.therapy.flow.superadmin.service.SuperAdminOrganisationListService;
import com.smart.therapy.flow.superadmin.service.SuperAdminOrganisationQueryService;
import com.smart.therapy.flow.superadmin.service.OrganisationFeatureOverrideService;
import com.smart.therapy.flow.superadmin.service.SuperAdminPlanCatalogService;
import com.smart.therapy.flow.superadmin.service.SuperAdminSubscriptionService;
import com.smart.therapy.flow.superadmin.service.SuperAdminTenantOpsFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({
        SuperAdminController.class,
        SuperAdminMainControllerSecurityValidationTest.TestSecurityConfig.class
})
@DisplayName("Super Admin Main Controller Security and Contract Tests")
class SuperAdminMainControllerSecurityValidationTest extends BaseControllerContractTest {

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    void shouldResolveFeatureDetailsThroughServicesAndPreserveAcl() throws Exception {
        when(superAdminOrganisationQueryService.resolveOrganisationId("acme")).thenReturn(42L);
        when(organisationFeatureOverrideService.getEffectiveFeatures(42L)).thenReturn(Map.of(
                "CLIENT_LIMIT", new OrganisationFeatureOverrideService.FeatureState(true, 75),
                "BILLING_MODULE", new OrganisationFeatureOverrideService.FeatureState(true, null)));
        when(superAdminRoleService.getPermissionCatalog()).thenReturn(java.util.List.of(
                com.smart.therapy.flow.auth.entity.Permission.builder().name("CLIENT_VIEW_ALL").build(),
                com.smart.therapy.flow.auth.entity.Permission.builder().name("BILLING_VIEW").build(),
                com.smart.therapy.flow.auth.entity.Permission.builder().name("CLIENT_VIEW_ALL").build()));
        mockMvc.perform(get("/api/v1/super-admin/organisations/acme/features/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("BILLING_MODULE"))
                .andExpect(jsonPath("$[0].accessControlList[0]").value("BILLING_VIEW"))
                .andExpect(jsonPath("$[0].usageLimitations.currentUsageLimit").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].key").value("CLIENT_LIMIT"))
                .andExpect(jsonPath("$[1].accessControlList.length()").value(1))
                .andExpect(jsonPath("$[1].usageLimitations.currentUsageLimit").value(75));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    void shouldSeedDefaultsUsingOrganisationQueryService() throws Exception {
        var organisation = com.smart.therapy.flow.organisation.entity.Organisation.builder()
                .id(42L).schemaName("tenant_fixture").build();
        when(superAdminOrganisationQueryService.getOrganisation(42L)).thenReturn(java.util.Optional.of(organisation));
        when(fixtureTenantFlywayMigrator.seedTenantDefaults(organisation)).thenReturn(
                new com.smart.therapy.flow.organisation.service.TenantFlywayMigrator.TenantSeedResult(1, 2, 3, 4, 5, 6));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/v1/super-admin/organisations/42/tenant-defaults/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaName").value("tenant_fixture"))
                .andExpect(jsonPath("$.staffProfilesCreated").value(1))
                .andExpect(jsonPath("$.clinicalTemplateRecordsCreated").value(6));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    void shouldRejectMissingOrganisationAndPublicSchemaBeforeSeeding() throws Exception {
        when(superAdminOrganisationQueryService.getOrganisation(42L)).thenReturn(java.util.Optional.empty());
        when(superAdminOrganisationQueryService.getOrganisation(43L)).thenReturn(java.util.Optional.of(
                com.smart.therapy.flow.organisation.entity.Organisation.builder().id(43L).schemaName("public").build()));
        for (long id : new long[]{42, 43}) {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                            "/api/v1/super-admin/organisations/{id}/tenant-defaults/seed", id))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
        org.mockito.Mockito.verifyNoInteractions(fixtureTenantFlywayMigrator);
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    void shouldRejectAuditorBeforeResolvingSeedOrganisation() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/v1/super-admin/organisations/42/tenant-defaults/seed"))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(superAdminOrganisationQueryService, fixtureTenantFlywayMigrator);
    }

    @MockBean
    private com.smart.therapy.flow.superadmin.service.SuperAdminUsageService fixtureSuperAdminUsageService;

    @MockBean
    private com.smart.therapy.flow.superadmin.service.SuperAdminUserService fixtureSuperAdminUserService;

    @MockBean
    private com.smart.therapy.flow.superadmin.service.SuperAdminImpersonationService fixtureSuperAdminImpersonationService;

    @MockBean
    private com.smart.therapy.flow.superadmin.service.SuperAdminTenantStaffBootstrapService fixtureSuperAdminTenantStaffBootstrapService;

    @MockBean
    private com.smart.therapy.flow.superadmin.service.SuperAdminClinicalTemplateBackfillService fixtureSuperAdminClinicalTemplateBackfillService;

    @MockBean
    private com.smart.therapy.flow.organisation.service.TenantFlywayMigrator fixtureTenantFlywayMigrator;

    @MockBean
    private com.smart.therapy.flow.payment.service.StripeTenantConfigService fixtureStripeTenantConfigService;

    @MockBean
    private com.smart.therapy.flow.superadmin.service.SuperAdminRoleService superAdminRoleService;



    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionFeatureService subscriptionFeatureService;

    @MockBean
    private SuperAdminOrganisationListService superAdminOrganisationListService;

    @MockBean
    private SuperAdminOrganisationQueryService superAdminOrganisationQueryService;

    @MockBean
    private SuperAdminSubscriptionService superAdminSubscriptionService;

    @MockBean
    private SuperAdminOnboardingService superAdminOnboardingService;

    @MockBean
    private SuperAdminPlanCatalogService superAdminPlanCatalogService;

    @MockBean
    private SuperAdminAddonService superAdminAddonService;

    @MockBean
    private SuperAdminTenantOpsFacade superAdminTenantOpsFacade;

    @MockBean
    private SuperAdminBillingService superAdminBillingService;

    @MockBean
    private OrganisationFeatureOverrideService organisationFeatureOverrideService;

    @Test
    @DisplayName("Should block unauthenticated super-admin organisations list")
    void shouldBlockUnauthenticatedOrganisationList() throws Exception {
        mockMvc.perform(get("/api/v1/super-admin/organisations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should allow platform auditor to list organisations")
    void shouldAllowAuditorToListOrganisations() throws Exception {
        ResponseEntity<Object> response = ResponseEntity.ok(Map.of(
                "items", java.util.List.of(),
                "page", 1,
                "pageSize", 25,
                "total", 0
        ));
        doReturn(response).when(superAdminOrganisationQueryService).listOrganisations(any(), eq(false));

        mockMvc.perform(get("/api/v1/super-admin/organisations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(25))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should block auditor from plan update endpoint")
    void shouldBlockAuditorFromPlanUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/super-admin/plans/basic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Updated plan",
                                "basePrice", 19.99,
                                "annualPrice", 199.99,
                                "billingCycle", "monthly"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should allow super admin to update plan with valid payload")
    void shouldAllowSuperAdminToUpdatePlan() throws Exception {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("basic");
        plan.setDescription("Updated plan");
        plan.setBasePrice(new BigDecimal("19.99"));
        plan.setAnnualPrice(new BigDecimal("199.99"));
        plan.setBillingCycle("monthly");

        when(superAdminPlanCatalogService.updatePlanDetails(eq("basic"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(plan);

        mockMvc.perform(put("/api/v1/super-admin/plans/basic")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Updated plan",
                                "basePrice", 19.99,
                                "annualPrice", 199.99,
                                "billingCycle", "monthly"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("basic"))
                .andExpect(jsonPath("$.billingCycle").value("monthly"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should return not found for retired legacy subscription endpoint in SuperAdminController")
    void shouldReturnNotFoundForRetiredLegacySubscriptionEndpoint() throws Exception {
        mockMvc.perform(put("/api/v1/super-admin/organisations/99/subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planName", ""))))
                .andExpect(status().isNotFound());
    }

    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }
}

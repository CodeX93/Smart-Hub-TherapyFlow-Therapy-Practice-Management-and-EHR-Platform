package com.smart.therapy.flow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.superadmin.controller.SuperAdminImpersonationController;
import com.smart.therapy.flow.superadmin.controller.SuperAdminOperationsController;
import com.smart.therapy.flow.superadmin.controller.SuperAdminPartnerController;
import com.smart.therapy.flow.superadmin.entity.PlatformBackupJob;
import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationPolicy;
import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationSession;
import com.smart.therapy.flow.superadmin.entity.PlatformPartnerAccess;
import com.smart.therapy.flow.superadmin.service.SuperAdminImpersonationService;
import com.smart.therapy.flow.superadmin.service.SuperAdminNotificationService;
import com.smart.therapy.flow.superadmin.service.SuperAdminOperationsService;
import com.smart.therapy.flow.superadmin.service.SuperAdminPartnerAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({
        SuperAdminImpersonationController.class,
        SuperAdminPartnerController.class,
        SuperAdminOperationsController.class,
        SuperAdminExtendedControllerSecurityValidationTest.TestSecurityConfig.class
})
@DisplayName("Super Admin Extended Controller Security and Validation Tests")
class SuperAdminExtendedControllerSecurityValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SuperAdminImpersonationService impersonationService;

    @MockBean
    private SuperAdminPartnerAccessService partnerAccessService;

    @MockBean
    private SuperAdminOperationsService operationsService;

    @MockBean
    private SuperAdminNotificationService notificationService;

    @Test
    @DisplayName("Should block unauthenticated impersonation policy read")
    void shouldBlockUnauthenticatedImpersonationPolicyRead() throws Exception {
        mockMvc.perform(get("/api/v1/super-admin/impersonation/policy"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should allow platform auditor to read impersonation policy")
    void shouldAllowAuditorToReadImpersonationPolicy() throws Exception {
        PlatformImpersonationPolicy policy = PlatformImpersonationPolicy.builder()
                .id(1L)
                .enabled(true)
                .requireReason(true)
                .minReasonLength(10)
                .maxDurationMinutes(60)
                .allowCrossOrganisation(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(impersonationService.getPolicy()).thenReturn(policy);

        mockMvc.perform(get("/api/v1/super-admin/impersonation/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.maxDurationMinutes").value(60));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should validate impersonation start payload")
    void shouldValidateImpersonationStartPayload() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/impersonation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetAuthId", 1001,
                                "organisationId", 101,
                                "reason", "",
                                "durationMinutes", -1
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should allow super admin to start impersonation with valid payload")
    void shouldAllowSuperAdminToStartImpersonationWithValidPayload() throws Exception {
        Organisation org = organisation(101L, "harbor");
        PlatformImpersonationSession session = PlatformImpersonationSession.builder()
                .id(77L)
                .superAdminAuthId(1L)
                .targetAuthId(1001L)
                .organisation(org)
                .status("active")
                .startedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .tokenPrefix("imp_abc")
                .tokenHash("hashed")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(impersonationService.startSession(any(), anyLong(), anyLong(), anyString(), any()))
                .thenReturn(new SuperAdminImpersonationService.CreatedImpersonation(session, "raw_impersonation_token"));

        mockMvc.perform(post("/api/v1/super-admin/impersonation/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetAuthId", 1001,
                                "organisationId", 101,
                                "reason", "Support case triage",
                                "durationMinutes", 30
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(77))
                .andExpect(jsonPath("$.impersonationToken").value("raw_impersonation_token"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should block auditor from impersonation token exchange")
    void shouldBlockAuditorFromImpersonationExchange() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/impersonation/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "impersonationToken", "imp_token"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should allow super admin to exchange impersonation token")
    void shouldAllowSuperAdminToExchangeImpersonationToken() throws Exception {
        Organisation org = organisation(101L, "harbor");
        PlatformImpersonationSession session = PlatformImpersonationSession.builder()
                .id(88L)
                .superAdminAuthId(1L)
                .targetAuthId(1001L)
                .organisation(org)
                .status("active")
                .startedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(120))
                .tokenPrefix("imp_abc")
                .tokenHash("hashed")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(impersonationService.exchangeSessionToken(any(), anyString(), any()))
                .thenReturn(new SuperAdminImpersonationService.ExchangedImpersonation(
                        session,
                        "jwt_token",
                        "target@example.com",
                        List.of("ADMIN"),
                        List.of("USER_VIEW")
                ));

        mockMvc.perform(post("/api/v1/super-admin/impersonation/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "impersonationToken", "imp_live_token"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(88))
                .andExpect(jsonPath("$.auth.accessToken").value("jwt_token"))
                .andExpect(jsonPath("$.auth.roles[0]").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should block auditor from partner access upsert")
    void shouldBlockAuditorFromPartnerAccessUpsert() throws Exception {
        mockMvc.perform(put("/api/v1/super-admin/partners/partner_1/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "organisationIds", List.of(101L)
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should validate partner access upsert payload")
    void shouldValidatePartnerAccessPayload() throws Exception {
        mockMvc.perform(put("/api/v1/super-admin/partners/partner_1/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "organisationIds", List.of()
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should allow super admin to upsert partner access")
    void shouldAllowSuperAdminToUpsertPartnerAccess() throws Exception {
        PlatformPartnerAccess access = PlatformPartnerAccess.builder()
                .id(12L)
                .partnerId("partner_1")
                .organisation(organisation(101L, "harbor"))
                .featureFlagsJson("{\"api_access\":true}")
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(partnerAccessService.upsertAccess(anyString(), anyList(), anyMap(), any(), any()))
                .thenReturn(List.of(access));

        mockMvc.perform(put("/api/v1/super-admin/partners/partner_1/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "organisationIds", List.of(101L),
                                "featureFlags", Map.of("api_access", true),
                                "active", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].featureFlags.api_access").value(true));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should allow platform auditor to read backup jobs")
    void shouldAllowAuditorToReadBackupJobs() throws Exception {
        PlatformBackupJob backupJob = PlatformBackupJob.builder()
                .id(45L)
                .organisation(organisation(101L, "harbor"))
                .status("completed")
                .storageLocation("s3://bucket/backup.sql.gz")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(operationsService.listBackupJobs(101L)).thenReturn(List.of(backupJob));

        mockMvc.perform(get("/api/v1/super-admin/organisations/101/backups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value(45))
                .andExpect(jsonPath("$[0].status").value("completed"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should validate bulk action payload")
    void shouldValidateBulkActionPayload() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/bulk/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "",
                                "organisationIds", List.of()
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should block auditor from bulk actions")
    void shouldBlockAuditorFromBulkActions() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/bulk/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "suspend",
                                "organisationIds", List.of(101L)
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should allow super admin to run bulk actions")
    void shouldAllowSuperAdminToRunBulkActions() throws Exception {
        when(operationsService.applyBulkAction(anyList(), anyString(), any(), any()))
                .thenReturn(Map.of("requested", 2, "success", 2, "failed", 0, "errors", List.of()));

        mockMvc.perform(post("/api/v1/super-admin/bulk/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "action", "suspend",
                                "organisationIds", List.of(101L, 102L),
                                "reason", "policy breach"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(2))
                .andExpect(jsonPath("$.failed").value(0));
    }

    private static Organisation organisation(Long id, String slug) {
        return Organisation.builder()
                .id(id)
                .name("Org " + id)
                .slug(slug)
                .status("ACTIVE")
                .schemaName("tenant_" + id)
                .subdomain(slug)
                .build();
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

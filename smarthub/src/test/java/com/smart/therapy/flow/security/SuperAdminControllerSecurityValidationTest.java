package com.smart.therapy.flow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.superadmin.controller.SuperAdminIntegrationController;
import com.smart.therapy.flow.superadmin.controller.TenantRoutingController;
import com.smart.therapy.flow.superadmin.entity.PlatformApiKey;
import com.smart.therapy.flow.superadmin.service.SuperAdminIntegrationService;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({
        SuperAdminIntegrationController.class,
        TenantRoutingController.class,
        SuperAdminControllerSecurityValidationTest.TestSecurityConfig.class
})
@DisplayName("Super Admin Controller Security and Validation Tests")
class SuperAdminControllerSecurityValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SuperAdminIntegrationService integrationService;

    @MockBean
    private PlatformAuditService platformAuditService;

    @MockBean
    private TenantResolutionService tenantResolutionService;

    @Test
    @DisplayName("Should block unauthenticated create API key request")
    void shouldBlockUnauthenticatedCreateApiKeyRequest() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "keyName", "Partner Integration",
                                "scopes", List.of("read", "write")
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should block auditor from super admin write API key endpoint")
    void shouldBlockAuditorFromWriteApiKeyEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "keyName", "Partner Integration",
                                "scopes", List.of("read", "write")
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should validate create API key payload")
    void shouldValidateCreateApiKeyPayload() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "keyName", "",
                                "scopes", List.of()
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("Should allow super admin to create API key with valid payload")
    void shouldAllowSuperAdminToCreateApiKeyWithValidPayload() throws Exception {
        PlatformApiKey key = PlatformApiKey.builder()
                .id(11L)
                .keyName("Partner Integration")
                .keyPrefix("sk_live_abcd")
                .keyHash("hashed")
                .scopesJson("[\"read\",\"write\"]")
                .isActive(true)
                .expiresAt(Instant.now().plusSeconds(86400))
                .createdAt(Instant.now())
                .build();

        when(integrationService.createApiKey(anyString(), anyList(), any(), any()))
                .thenReturn(new SuperAdminIntegrationService.CreatedApiKey(key, "raw_generated_key"));

        mockMvc.perform(post("/api/v1/super-admin/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "keyName", "Partner Integration",
                                "scopes", List.of("read", "write")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.apiKey").value("raw_generated_key"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should block non platform-read role from tenant resolve")
    void shouldBlockNonPlatformReadRoleFromTenantResolve() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/tenants/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "test@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should validate tenant resolve email payload")
    void shouldValidateTenantResolveEmailPayload() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/tenants/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "invalid-email"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("Should allow platform auditor to resolve tenant by valid email")
    void shouldAllowPlatformAuditorToResolveTenantByValidEmail() throws Exception {
        Organisation organisation = Organisation.builder()
                .id(101L)
                .name("Harbor Wellness")
                .slug("harbor-wellness")
                .status("ACTIVE")
                .schemaName("tenant_101")
                .subdomain("harbor")
                .build();

        UserOrganisation row = UserOrganisation.builder()
                .organisation(organisation)
                .createdAt(Instant.now())
                .build();

        when(tenantResolutionService.resolveByEmail(eq("auditor@example.com")))
                .thenReturn(List.of(row));

        mockMvc.perform(post("/api/v1/super-admin/tenants/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "auditor@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.organisations[0].organisationId").value(101));
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

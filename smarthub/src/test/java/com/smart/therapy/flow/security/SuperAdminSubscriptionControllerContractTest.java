package com.smart.therapy.flow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.GlobalExceptionHandler;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.superadmin.controller.SuperAdminSubscriptionController;
import com.smart.therapy.flow.superadmin.service.SuperAdminSubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuperAdminSubscriptionController.class)
@Import({
        SuperAdminSubscriptionController.class,
        GlobalExceptionHandler.class,
        SuperAdminSubscriptionControllerContractTest.TestSecurityConfig.class
})
@DisplayName("Super Admin Subscription Controller Contract Tests")
class SuperAdminSubscriptionControllerContractTest extends BaseControllerContractTest {

    @MockBean
    private com.smart.therapy.flow.billing.service.PlatformSubscriptionBackfillService platformSubscriptionBackfillService;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SuperAdminSubscriptionService superAdminSubscriptionService;

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("GET returns strict payload with user limits and usage")
    void shouldReturnStrictPayloadOnGet() throws Exception {
        when(superAdminSubscriptionService.getStrictSubscription(99L))
                .thenReturn(new SuperAdminSubscriptionService.StrictSubscriptionView(
                        99L,
                        11L,
                        "pro",
                        "trialing",
                        "annual",
                        new BigDecimal("199.00"),
                        Instant.parse("2026-03-01T00:00:00Z"),
                        null,
                        Instant.parse("2026-03-30T00:00:00Z"),
                        "cus_123",
                        "sub_123",
                        10,
                        3,
                        500,
                        7L,
                        2L,
                        120L
                ));

        mockMvc.perform(get("/api/v1/super-admin/organisations/99/subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("trialing"))
                .andExpect(jsonPath("$.trialEndsAt").value("2026-03-30T00:00:00Z"))
                .andExpect(jsonPath("$.userLimits.therapistLimit").value(10))
                .andExpect(jsonPath("$.userLimits.supervisorLimit").value(3))
                .andExpect(jsonPath("$.userLimits.clientLimit").value(500))
                .andExpect(jsonPath("$.userUsage.therapistUsers").value(7))
                .andExpect(jsonPath("$.userUsage.supervisorUsers").value(2))
                .andExpect(jsonPath("$.userUsage.clientUsers").value(120))
                .andExpect(jsonPath("$.userUsage.totalUsers").value(129));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("PUT trialDays=7 returns 422 INVALID_TRIAL_DAYS")
    void shouldReturn422ForInvalidTrialDays() throws Exception {
        when(superAdminSubscriptionService.updateStrictSubscription(eq(99L), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TRIAL_DAYS", "trialDays must be 14 or 30"));

        mockMvc.perform(put("/api/v1/super-admin/organisations/99/subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("trialDays", 7))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_TRIAL_DAYS"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("PUT unknown plan returns 422 PLAN_NOT_FOUND")
    void shouldReturn422ForUnknownPlan() throws Exception {
        when(superAdminSubscriptionService.updateStrictSubscription(eq(99L), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PLAN_NOT_FOUND", "Unknown plan: x"));

        mockMvc.perform(put("/api/v1/super-admin/organisations/99/subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("plan", "x"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("PUT forbidden for non super admin")
    void shouldReturnForbiddenForAuditorPut() throws Exception {
        mockMvc.perform(put("/api/v1/super-admin/organisations/99/subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("trialDays", 14))))
                .andExpect(status().isForbidden());
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

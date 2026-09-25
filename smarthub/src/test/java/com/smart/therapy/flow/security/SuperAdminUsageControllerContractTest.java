package com.smart.therapy.flow.security;

import com.smart.therapy.flow.common.exception.GlobalExceptionHandler;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.superadmin.controller.SuperAdminUsageController;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUsageMetricResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUsageResponse;
import com.smart.therapy.flow.superadmin.service.SuperAdminUsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SuperAdminUsageController.class)
@Import({
        SuperAdminUsageController.class,
        GlobalExceptionHandler.class,
        SuperAdminUsageControllerContractTest.TestSecurityConfig.class
})
@DisplayName("Super Admin Usage Controller Contract Tests")
class SuperAdminUsageControllerContractTest extends BaseControllerContractTest {


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SuperAdminUsageService superAdminUsageService;

    @Test
    @WithMockUser(roles = "PLATFORM_AUDITOR")
    @DisplayName("GET usage returns metrics list")
    void shouldReturnUsageMetrics() throws Exception {
        SuperAdminUsageResponse response = new SuperAdminUsageResponse();
        response.setOrganisationId(99L);
        response.setPeriod("2026-03");
        response.setMetrics(List.of(
                new SuperAdminUsageMetricResponse("AI_REPORTS_PER_MONTH", 500L, 500),
                new SuperAdminUsageMetricResponse("CLIENT_LIMIT", 120L, 200)
        ));
        when(superAdminUsageService.getUsage(99L, "2026-03", null)).thenReturn(response);

        mockMvc.perform(get("/api/v1/super-admin/usage")
                        .param("orgId", "99")
                        .param("period", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organisationId").value(99))
                .andExpect(jsonPath("$.period").value("2026-03"))
                .andExpect(jsonPath("$.metrics[0].featureKey").value("AI_REPORTS_PER_MONTH"))
                .andExpect(jsonPath("$.metrics[0].used").value(500))
                .andExpect(jsonPath("$.metrics[0].limit").value(500));
    }

    @Test
    @WithMockUser(roles = "PLATFORM_SUPER_ADMIN")
    @DisplayName("GET usage invalid period returns 400 INVALID_PERIOD")
    void shouldReturn400InvalidPeriod() throws Exception {
        when(superAdminUsageService.getUsage(99L, "2026/03", null))
                .thenThrow(new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "period must be in YYYY-MM format"));

        mockMvc.perform(get("/api/v1/super-admin/usage")
                        .param("orgId", "99")
                        .param("period", "2026/03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET usage forbidden for non-platform role")
    void shouldReturnForbiddenForNonPlatformRole() throws Exception {
        mockMvc.perform(get("/api/v1/super-admin/usage")
                        .param("orgId", "99"))
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

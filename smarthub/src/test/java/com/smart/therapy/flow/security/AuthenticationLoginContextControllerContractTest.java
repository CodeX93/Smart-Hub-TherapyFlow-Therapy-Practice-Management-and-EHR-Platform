package com.smart.therapy.flow.security;

import com.smart.therapy.flow.auth.controller.AuthenticationController;
import com.smart.therapy.flow.auth.dto.LoginContextResponse;
import com.smart.therapy.flow.auth.service.AuthenticationService;
import com.smart.therapy.flow.auth.service.LoginRoutingService;
import com.smart.therapy.flow.common.exception.GlobalExceptionHandler;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.superadmin.service.TenantResolutionService;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import({
        AuthenticationController.class,
        GlobalExceptionHandler.class,
        AuthenticationLoginContextControllerContractTest.TestSecurityConfig.class
})
@DisplayName("Authentication Login Context Controller Contract Tests")
class AuthenticationLoginContextControllerContractTest extends BaseControllerContractTest {

    @MockBean
    private com.smart.therapy.flow.auth.service.AuthMeService fixtureAuthMeService;


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private TenantResolutionService tenantResolutionService;

    @MockBean
    private LoginRoutingService loginRoutingService;

    @Test
    @DisplayName("GET login-context returns org context for orgSlug")
    void shouldReturnLoginContextForOrgSlug() throws Exception {
        LoginContextResponse response = LoginContextResponse.builder()
                .email("user@example.com")
                .orgSlug("acme")
                .organisationId(10L)
                .organisationName("Acme")
                .branding(LoginContextResponse.Branding.builder()
                        .logoUrl("https://cdn.example.com/logo.png")
                        .brandPrimaryColor("#112233")
                        .build())
                .ssoProviders(List.of("GOOGLE"))
                .organisations(List.of())
                .count(1)
                .resolvedAt(Instant.parse("2026-03-27T10:15:30Z"))
                .build();
        when(loginRoutingService.resolveContext(null, "acme")).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/login-context")
                        .param("orgSlug", "acme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orgSlug").value("acme"))
                .andExpect(jsonPath("$.organisationId").value(10))
                .andExpect(jsonPath("$.branding.logoUrl").value("https://cdn.example.com/logo.png"))
                .andExpect(jsonPath("$.ssoProviders[0]").value("GOOGLE"));
    }

    @Test
    @DisplayName("GET login-context returns 400 when email not in org")
    void shouldReturnBadRequestForEmailNotInOrg() throws Exception {
        when(loginRoutingService.resolveContext("user@example.com", "beta"))
                .thenThrow(new StoryApiException(HttpStatus.BAD_REQUEST, "EMAIL_NOT_IN_ORG", "Email does not belong to organisation"));

        mockMvc.perform(get("/api/v1/auth/login-context")
                        .param("email", "user@example.com")
                        .param("orgSlug", "beta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_IN_ORG"));
    }

    @Test
    @DisplayName("GET login-context returns 400 when parameters missing")
    void shouldReturnValidationErrorWhenMissingParams() throws Exception {
        when(loginRoutingService.resolveContext(null, null))
                .thenThrow(new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "email or orgSlug is required"));

        mockMvc.perform(get("/api/v1/auth/login-context"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Configuration
    @EnableMethodSecurity(prePostEnabled = true)
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }
}

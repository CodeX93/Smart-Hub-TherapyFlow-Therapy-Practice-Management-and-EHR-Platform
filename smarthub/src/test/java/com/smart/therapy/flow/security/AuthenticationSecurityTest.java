package com.smart.therapy.flow.security;

import com.smart.therapy.flow.auth.dto.LoginRequest;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.BaseTenantPerformanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Authentication Security Tests")
class AuthenticationSecurityTest extends BaseTenantPerformanceTest {

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired
    private com.smart.therapy.flow.common.security.JwtTokenProvider tokenProvider;

    @Test
    void shouldRefreshSessionActivityUnderConcurrentRequestsAndStillRejectRevocation() throws Exception {
        String token = getAuthToken(testUser.getEmail(), "password123");
        String jti = tokenProvider.getJtiFromToken(token);
        jdbc.update("UPDATE public.auth_sessions SET last_activity_at = CURRENT_TIMESTAMP - INTERVAL '2 minutes' WHERE jwt_id = ?", jti);
        runConcurrent(20, 40, 30, requestId ->
                mockMvc.perform(get("/api/v1/auth/me").headers(createHeaders(token)))
                        .andExpect(status().isOk()));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT last_activity_at > CURRENT_TIMESTAMP - INTERVAL '1 minute' FROM public.auth_sessions WHERE jwt_id = ?",
                Boolean.class, jti)).isTrue();
        jdbc.update("UPDATE public.auth_sessions SET revoked = true WHERE jwt_id = ?", jti);
        mockMvc.perform(get("/api/v1/auth/me").headers(createHeaders(token)))
                .andExpect(status().isUnauthorized());
    }

    @Autowired
    private com.smart.therapy.flow.common.config.RateLimitingConfig rateLimitConfig;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = persistStaff(uniqueEmail("testUser"), "password123", "THERAPIST");
    }

    @Test
    @DisplayName("Should authenticate with valid credentials")
    void shouldAuthenticateWithValidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername(testUser.getEmail());
        request.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .headers(createHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("Should reject authentication with invalid password")
    void shouldRejectAuthenticationWithInvalidPassword() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername(testUser.getEmail());
        request.setPassword("wrong-password");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .headers(createHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Invalid")));
    }

    @Test
    @DisplayName("Should reject authentication with non-existent username")
    void shouldRejectAuthenticationWithNonExistentUsername() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent@example.com");
        request.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .headers(createHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should prevent SQL injection in login username")
    void shouldPreventSqlInjectionInLoginUsername() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("admin' OR '1'='1");
        request.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .headers(createHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should rate limit login attempts")
    void shouldRateLimitLoginAttempts() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(testUser.getEmail());
        request.setPassword("wrong-password");
        var headers = createHeaders();
        headers.set("X-Forwarded-For", "192.0.2.123");
        boolean originallyEnabled = rateLimitConfig.isRateLimitingEnabled();
        org.springframework.test.util.ReflectionTestUtils.setField(rateLimitConfig, "rateLimitingEnabled", true);
        try {
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(post("/api/v1/auth/login").headers(headers)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
            }
            mockMvc.perform(post("/api/v1/auth/login").headers(headers)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("X-RateLimit-Remaining", "0"));
        } finally {
            org.springframework.test.util.ReflectionTestUtils.setField(rateLimitConfig, "rateLimitingEnabled", originallyEnabled);
        }
    }

    @Test
    @DisplayName("Should require authentication for protected endpoints")
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/clients")
                        .headers(createHeaders()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should validate JWT token format")
    void shouldValidateJwtTokenFormat() throws Exception {
        // Arrange
        String invalidToken = "invalid-token-format";

        // Act & Assert
        mockMvc.perform(get("/api/v1/clients")
                        .headers(createHeaders(invalidToken)))
                .andExpect(status().isUnauthorized());
    }
}

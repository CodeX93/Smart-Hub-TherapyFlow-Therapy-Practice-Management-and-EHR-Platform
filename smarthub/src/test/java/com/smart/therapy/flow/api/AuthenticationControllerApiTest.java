package com.smart.therapy.flow.api;

import com.smart.therapy.flow.auth.dto.LoginRequest;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.service.AuthRefreshTokenService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthenticationController API Tests")
class AuthenticationControllerApiTest extends BaseTenantApiTest {

    private User testUser;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        testUser = persistStaff("auth-" + UUID.randomUUID() + "@example.test", "password123", "THERAPIST");
    }

    @Test
    @DisplayName("Should login successfully via API")
    void shouldLoginSuccessfullyViaApi() throws Exception {
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
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("tf_refresh="),
                        containsString("Path=/api/v1/auth"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"))))
                .andExpect(jsonPath("$.username").value(testUser.getEmail()))
                .andExpect(jsonPath("$.tenantSchema").value(TENANT_SCHEMA))
                .andExpect(jsonPath("$.organisationId").value(tenantOrganisation.getId().intValue()));
    }

    @Test
    @DisplayName("Should return 401 with invalid credentials")
    void shouldReturn401WithInvalidCredentials() throws Exception {
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
    @DisplayName("Should return 400 with missing credentials")
    void shouldReturn400WithMissingCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        // Missing email and password

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .headers(createHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Arrange - First login to get refresh token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUser.getEmail());
        loginRequest.setPassword("password123");

        String refreshToken = mockMvc.perform(post("/api/v1/auth/login")
                        .headers(createHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(refreshToken).path("refreshToken").asText();
        assertThat(token).isNotBlank();
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .headers(createHeaders())
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(not(equalTo(token))))
                .andExpect(jsonPath("$.tenantSchema").value(TENANT_SCHEMA))
                .andExpect(jsonPath("$.organisationId").value(tenantOrganisation.getId().intValue()));
    }

    @Test
    @DisplayName("Should refresh from the HttpOnly cookie alone and rotate it")
    void shouldRefreshFromCookie() throws Exception {
        Cookie issued = staffLogin().getResponse().getCookie("tf_refresh");
        assertThat(issued).isNotNull();
        assertThat(issued.isHttpOnly()).isTrue();
        assertThat(issued.getMaxAge()).isPositive();

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .headers(createHeaders())
                        .cookie(issued))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie rotated = refreshed.getResponse().getCookie("tf_refresh");
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotBlank().isNotEqualTo(issued.getValue());
    }

    @Test
    @DisplayName("Should refuse a refresh that carries no token at all")
    void shouldRejectRefreshWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").headers(createHeaders()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout should revoke the cookie's refresh token and clear the cookie")
    void shouldRevokeCookieOnLogout() throws Exception {
        MvcResult login = staffLogin();
        Cookie issued = login.getResponse().getCookie("tf_refresh");
        String accessToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("accessToken").asText();

        MvcResult logout = mockMvc.perform(post("/api/v1/auth/logout")
                        .headers(createHeaders(accessToken))
                        .cookie(issued))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cleared = logout.getResponse().getCookie("tf_refresh");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .headers(createHeaders())
                        .cookie(issued))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout with an expired access token should still revoke the cookie's refresh token")
    void shouldRevokeCookieOnLogoutWithoutAccessToken() throws Exception {
        Cookie issued = staffLogin().getResponse().getCookie("tf_refresh");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .headers(createHeaders())
                        .cookie(issued))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .headers(createHeaders())
                        .cookie(issued))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Portal sessions should get their own cookie, scoped to the portal")
    void shouldIssueAndRefreshPortalCookie() throws Exception {
        Client client = persistClient(testUser);
        Cookie issued = portalLogin(client).getResponse().getCookie("tf_portal_refresh");
        assertThat(issued).isNotNull();
        assertThat(issued.getPath()).isEqualTo("/api/v1/portal");
        assertThat(issued.isHttpOnly()).isTrue();

        MvcResult refreshed = mockMvc.perform(post("/api/v1/portal/refresh")
                        .headers(createHeaders())
                        .cookie(issued))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        assertThat(refreshed.getResponse().getCookie("tf_portal_refresh")).isNotNull();
    }

    private MvcResult staffLogin() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername(testUser.getEmail());
        request.setPassword("password123");
        return mockMvc.perform(post("/api/v1/auth/login")
                        .headers(createHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void shouldAuthenticateProtectedProfileWithIssuedToken() throws Exception {
        String accessToken = getAuthToken(testUser.getEmail(), "password123");
        mockMvc.perform(get("/api/v1/auth/me")
                        .headers(createHeaders(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantSchema").value(TENANT_SCHEMA))
                .andExpect(jsonPath("$.organisationId").value(tenantOrganisation.getId().intValue()))
                .andExpect(jsonPath("$.user.email").value(testUser.getEmail()));
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").headers(createHeaders())
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid-fixture-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Replaying a rotated refresh token revokes the whole family")
    void replayedRefreshTokenRevokesFamily() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(testUser.getEmail());
        loginRequest.setPassword("password123");
        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .headers(createHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String original = objectMapper.readTree(loginBody).path("refreshToken").asText();

        String refreshBody = mockMvc.perform(post("/api/v1/auth/refresh")
                        .headers(createHeaders())
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", original))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String rotated = objectMapper.readTree(refreshBody).path("refreshToken").asText();
        assertThat(rotated).isNotBlank().isNotEqualTo(original);
        rotatedLongAgo(original);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .headers(createHeaders())
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", original))))
                .andExpect(status().isUnauthorized());

        // The replay is treated as theft: the newest token of the family must be dead too.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .headers(createHeaders())
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", rotated))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A page reloaded mid-refresh may present the token it never saw replaced")
    void replayWithinRotationGraceIsALostResponseNotTheft() throws Exception {
        Cookie original = staffLogin().getResponse().getCookie("tf_refresh");

        Cookie rotated = mockMvc.perform(post("/api/v1/auth/refresh").headers(createHeaders()).cookie(original))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("tf_refresh");

        // The browser dropped that response: the next load still holds the original cookie.
        Cookie recovered = mockMvc.perform(post("/api/v1/auth/refresh").headers(createHeaders()).cookie(original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getCookie("tf_refresh");
        assertThat(recovered.getValue()).isNotEqualTo(original.getValue()).isNotEqualTo(rotated.getValue());

        // Nothing was revoked: both live branches of the family keep working.
        mockMvc.perform(post("/api/v1/auth/refresh").headers(createHeaders()).cookie(rotated))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh").headers(createHeaders()).cookie(recovered))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Logout is not undone by replaying a just-rotated token")
    void replayWithinGraceAfterLogoutStaysRevoked() throws Exception {
        Cookie original = staffLogin().getResponse().getCookie("tf_refresh");
        Cookie rotated = mockMvc.perform(post("/api/v1/auth/refresh").headers(createHeaders()).cookie(original))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("tf_refresh");

        mockMvc.perform(post("/api/v1/auth/logout").headers(createHeaders()).cookie(rotated))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh").headers(createHeaders()).cookie(original))
                .andExpect(status().isUnauthorized());
    }

    /** Moves a rotated token's rotation outside the grace window, as if the replay came much later. */
    private void rotatedLongAgo(String refreshToken) {
        int updated = jdbc.update(
                "UPDATE public.auth_refresh_tokens SET revoked_at = now() - interval '1 hour' WHERE token_hash = ?",
                AuthRefreshTokenService.hashToken(refreshToken));
        assertThat(updated).isEqualTo(1);
    }
}

package com.smart.therapy.flow.security;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Authorization Security Tests")
class AuthorizationSecurityTest extends BaseTenantApiTest {

    private User therapist;
    private User admin;
    private Client client;
    private String therapistToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        therapistToken = getAuthToken(therapist.getEmail(), "password123");

        admin = persistStaff(uniqueEmail("admin"), "password123", "ADMIN");
        adminToken = getAuthToken(admin.getEmail(), "password123");

        client = persistClient(therapist);
        client = clientRepository.save(client);
    }

    @Test
    @DisplayName("Should allow therapist to access their own clients")
    void shouldAllowTherapistToAccessOwnClients() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/clients/{id}", client.getId())
                        .headers(createHeaders(therapistToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should prevent therapist from accessing other therapist's clients")
    void shouldPreventTherapistFromAccessingOtherTherapistClients() throws Exception {
        // Arrange
        User otherTherapist = persistStaff(uniqueEmail("other"), "password123", "THERAPIST");
        Client otherClient = persistClient(otherTherapist);

        // Act & Assert
        mockMvc.perform(get("/api/v1/clients/{id}", otherClient.getId())
                        .headers(createHeaders(therapistToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should allow admin to access all clients")
    void shouldAllowAdminToAccessAllClients() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/clients/{id}", client.getId())
                        .headers(createHeaders(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should prevent therapist from creating users")
    void shouldPreventTherapistFromCreatingUsers() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .headers(createHeaders(therapistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow admin to create users")
    void shouldAllowAdminToCreateUsers() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .headers(createHeaders(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // Bad request due to validation, but not forbidden
    }

    @Test
    @DisplayName("Should prevent unauthorized access to admin endpoints")
    void shouldPreventUnauthorizedAccessToAdminEndpoints() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users")
                        .headers(createHeaders(therapistToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should enforce permissions for authenticated restricted staff")
    void shouldEnforceRoleBasedAccessControl() throws Exception {
        User restricted = persistStaff(uniqueEmail("restricted"), "password123", "STAFF");
        String restrictedToken = getAuthToken(restricted.getEmail(), "password123");
        mockMvc.perform(get("/api/v1/users")
                        .headers(createHeaders(restrictedToken)))
                .andExpect(status().isForbidden());
    }
    @Test
    void shouldRejectClientPortalTokenOnStaffEndpoint() throws Exception {
        String token = portalToken(client);
        mockMvc.perform(get("/api/v1/clients").headers(createHeaders(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectTokenRoutedToAnotherTenant() throws Exception {
        registerOtherTenant();
        var headers = createHeaders(therapistToken);
        headers.set("X-Tenant-Subdomain", "api-other");
        mockMvc.perform(get("/api/v1/clients/{id}", client.getId()).headers(headers))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token tenant does not match request tenant."));
    }

}


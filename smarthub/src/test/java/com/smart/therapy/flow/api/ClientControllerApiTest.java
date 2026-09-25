package com.smart.therapy.flow.api;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ClientController API Tests")
class ClientControllerApiTest extends BaseTenantApiTest {

    private User therapist;
    private String authToken;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        authToken = getAuthToken(therapist.getEmail(), "password123");
    }

    @Test
    @DisplayName("Should create client via API successfully")
    void shouldCreateClientViaApiSuccessfully() throws Exception {
        // Arrange
        CreateClientRequest request = new CreateClientRequest();
        request.setStatus("active");
        request.setFullName("API Test Client");
        request.setEmail("apitest@example.com");
        request.setPhone("123-456-7890");

        // Act & Assert
        mockMvc.perform(post("/api/v1/clients")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fullName").value("API Test Client"))
                .andExpect(jsonPath("$.email").value("apitest@example.com"));
    }

    @Test
    @DisplayName("Should return 400 when creating client with invalid data")
    void shouldReturn400WhenCreatingClientWithInvalidData() throws Exception {
        // Arrange
        CreateClientRequest request = new CreateClientRequest();
        request.setStatus("active");
        request.setFullName(""); // Invalid: empty name
        request.setEmail("invalid-email"); // Invalid: not a valid email

        // Act & Assert
        mockMvc.perform(post("/api/v1/clients")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when creating client with duplicate email")
    void shouldReturn400WhenCreatingClientWithDuplicateEmail() throws Exception {
        // Arrange
        String duplicateEmail = uniqueEmail("duplicate");
        Client existingClient = persistClient(therapist, duplicateEmail);
        // Note: Email is now stored in normalized ClientContact entity, not directly on Client
        clientRepository.save(existingClient);

        CreateClientRequest request = new CreateClientRequest();
        request.setStatus("active");
        request.setFullName("New Client");
        request.setEmail(duplicateEmail); // Duplicate email

        // Act & Assert
        mockMvc.perform(post("/api/v1/clients")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Email already in use")));
    }

    @Test
    @DisplayName("Should get client by ID via API successfully")
    void shouldGetClientByIdViaApiSuccessfully() throws Exception {
        // Arrange
        Client client = persistClient(therapist);
        // Note: Email is stored in ClientContact entity, not directly on Client
        client = clientRepository.save(client);

        // Act & Assert
        mockMvc.perform(get("/api/v1/clients/{id}", client.getId())
                        .headers(createHeaders(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(client.getId()))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should return 404 when client not found")
    void shouldReturn404WhenClientNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/clients/99999")
                        .headers(createHeaders(authToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update client via API successfully")
    void shouldUpdateClientViaApiSuccessfully() throws Exception {
        // Arrange
        Client client = persistClient(therapist);
        // Note: Email is stored in ClientContact entity, not directly on Client
        client = clientRepository.save(client);

        com.smart.therapy.flow.client.dto.UpdateClientRequest updateRequest = 
                new com.smart.therapy.flow.client.dto.UpdateClientRequest();
        updateRequest.setFullName("Updated Name");
        updateRequest.setPhone("999-999-9999");

        // Act & Assert
        mockMvc.perform(patch("/api/v1/clients/{id}", client.getId())
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("fullName", "Updated Name", "phone", "999-999-9999"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.phone").value("999-999-9999"));
    }

    @Test
    @DisplayName("Should delete client via API successfully")
    void shouldDeleteClientViaApiSuccessfully() throws Exception {
        // Arrange
        Client client = persistClient(therapist);
        // Note: Email is stored in ClientContact entity, not directly on Client
        client = clientRepository.save(client);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/clients/{id}", client.getId())
                        .headers(createHeaders(authToken)))
                .andExpect(status().isNoContent());

        // Verify deleted
        enterFixtureTenant();
        assertThat(clientRepository.findByIdIncludingDeleted(client.getId()).orElseThrow().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return 403 when accessing API without authentication")
    void shouldReturn403WhenAccessingApiWithoutAuthentication() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/clients/1")
                        .headers(createHeaders()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return list of clients via API")
    void shouldReturnListOfClientsViaApi() throws Exception {
        // Arrange
        Client client1 = persistClient(therapist);
        // Note: Email is stored in ClientContact entity, not directly on Client
        clientRepository.save(client1);

        Client client2 = persistClient(therapist);
        // Note: Email is stored in ClientContact entity, not directly on Client
        clientRepository.save(client2);

        // Act & Assert
        mockMvc.perform(get("/api/v1/clients")
                        .headers(createHeaders(authToken))
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(greaterThanOrEqualTo(2)));
    }
}


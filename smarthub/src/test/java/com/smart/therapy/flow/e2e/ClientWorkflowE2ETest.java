package com.smart.therapy.flow.e2e;

import com.smart.therapy.flow.auth.dto.LoginRequest;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import com.smart.therapy.flow.session.dto.CreateSessionRequest;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Client Workflow End-to-End Tests")
class ClientWorkflowE2ETest extends BaseTenantApiTest {

    @Autowired
    private SessionRepository sessionRepository;

    private User therapist;
    private String authToken;
    private Long clientId;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        authToken = getAuthToken(therapist.getEmail(), "password123");
    }

    @Test
    @DisplayName("Should complete full client lifecycle workflow")
    void shouldCompleteFullClientLifecycleWorkflow() throws Exception {
        // Step 1: Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(therapist.getEmail());
        loginRequest.setPassword("password123");

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .headers(createHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Step 2: Create Client
        CreateClientRequest createRequest = new CreateClientRequest();
        createRequest.setStatus("active");
        createRequest.setFullName("E2E Test Client");
        // Note: Email and phone are now stored in normalized ClientContact entity
        // The request DTO still accepts these fields, but they're processed differently
        createRequest.setEmail("e2e@example.com");
        createRequest.setPhone("123-456-7890");

        String createResponse = mockMvc.perform(post("/api/v1/clients")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract client ID from response (simplified - in real test, parse JSON)
        enterFixtureTenant();
        Client savedClient = clientRepository.findById(objectMapper.readTree(createResponse).path("id").asLong()).orElseThrow();
        assertThat(savedClient).isNotNull();
        clientId = savedClient.getId();

        // Step 3: Get Client
        mockMvc.perform(get("/api/v1/clients/{id}", clientId)
                        .headers(createHeaders(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId))
                .andExpect(jsonPath("$.fullName").value("E2E Test Client"));

        // Step 4: Create Session for Client
        CreateSessionRequest sessionRequest = new CreateSessionRequest();
        sessionRequest.setClientId(clientId);
        sessionRequest.setTherapistId(therapist.getId());
        sessionRequest.setServiceId(fixtureService.getId());
        sessionRequest.setRoomId(fixtureRoom.getId());
        sessionRequest.setSessionType("individual");
        sessionRequest.setSessionDate(fixtureSessionDate(1));
        sessionRequest.setDuration(60);
        sessionRequest.setSessionMode("in_person");
        sessionRequest.setSessionMode("in_person");

        mockMvc.perform(post("/api/v1/sessions")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        // Step 5: Verify session exists for client
        enterFixtureTenant();
        assertThat(sessionRepository.findByClientId(clientId)).hasSize(1);

        // Step 6: Update Client
        com.smart.therapy.flow.client.dto.UpdateClientRequest updateRequest = 
                new com.smart.therapy.flow.client.dto.UpdateClientRequest();
        updateRequest.setFullName("Updated E2E Client");

        mockMvc.perform(patch("/api/v1/clients/{id}", clientId)
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("fullName", "Updated E2E Client"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated E2E Client"));

        // Step 7: Delete Client
        mockMvc.perform(delete("/api/v1/clients/{id}", clientId)
                        .headers(createHeaders(authToken)))
                .andExpect(status().isNoContent());

        // Step 8: Verify client deleted
        enterFixtureTenant();
        assertThat(clientRepository.findByIdIncludingDeleted(clientId).orElseThrow().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle client creation to session booking workflow")
    void shouldHandleClientCreationToSessionBookingWorkflow() throws Exception {
        // Create client
        CreateClientRequest createRequest = new CreateClientRequest();
        createRequest.setStatus("active");
        createRequest.setFullName("Workflow Test Client");
        createRequest.setEmail("workflow@example.com");

        String response = mockMvc.perform(post("/api/v1/clients")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        enterFixtureTenant();
        Client client = clientRepository.findById(objectMapper.readTree(response).path("id").asLong()).orElseThrow();
        assertThat(client).isNotNull();

        // Book session
        CreateSessionRequest sessionRequest = new CreateSessionRequest();
        sessionRequest.setClientId(client.getId());
        sessionRequest.setTherapistId(therapist.getId());
        sessionRequest.setServiceId(fixtureService.getId());
        sessionRequest.setRoomId(fixtureRoom.getId());
        sessionRequest.setSessionType("individual");
        sessionRequest.setSessionDate(fixtureSessionDate(1));
        sessionRequest.setDuration(60);
        sessionRequest.setSessionMode("in_person");

        mockMvc.perform(post("/api/v1/sessions")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionRequest)))
                .andExpect(status().isCreated());

        // Verify workflow completed
        enterFixtureTenant();
        assertThat(sessionRepository.findByClientId(client.getId())).hasSize(1);
    }
}



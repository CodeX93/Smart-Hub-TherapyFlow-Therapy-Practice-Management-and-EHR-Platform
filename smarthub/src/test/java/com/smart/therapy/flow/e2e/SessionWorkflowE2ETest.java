package com.smart.therapy.flow.e2e;

import com.smart.therapy.flow.auth.entity.User;
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

@DisplayName("Session Workflow End-to-End Tests")
class SessionWorkflowE2ETest extends BaseTenantApiTest {

    @Autowired
    private SessionRepository sessionRepository;

    private User therapist;
    private Client client;
    private String authToken;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        authToken = getAuthToken(therapist.getEmail(), "password123");

        client = persistClient(therapist);
        client = clientRepository.save(client);
    }

    @Test
    @DisplayName("Should complete full session lifecycle workflow")
    void shouldCompleteFullSessionLifecycleWorkflow() throws Exception {
        // Step 1: Create Session
        CreateSessionRequest createRequest = new CreateSessionRequest();
        createRequest.setClientId(client.getId());
        createRequest.setTherapistId(therapist.getId());
        createRequest.setServiceId(fixtureService.getId());
        createRequest.setRoomId(fixtureRoom.getId());
        createRequest.setSessionType("individual");
        createRequest.setSessionDate(fixtureSessionDate(1));
        createRequest.setDuration(60);
        createRequest.setSessionMode("in_person");

        String createResponse = mockMvc.perform(post("/api/v1/sessions")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract session ID (simplified - in real test, parse JSON)
        enterFixtureTenant();
        Session savedSession = sessionRepository.findByClientId(client.getId()).stream()
                .findFirst()
                .orElse(null);
        assertThat(savedSession).isNotNull();
        Long sessionId = savedSession.getId();

        // Step 2: Get Session
        mockMvc.perform(get("/api/v1/sessions/{id}", sessionId)
                        .headers(createHeaders(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId))
                .andExpect(jsonPath("$.duration").value(60));

        // Step 3: Update Session
        com.smart.therapy.flow.session.dto.UpdateSessionRequest updateRequest = new com.smart.therapy.flow.session.dto.UpdateSessionRequest();
        updateRequest.setDuration(90);
        updateRequest.setSessionDate(fixtureSessionDate(2));

        mockMvc.perform(put("/api/v1/sessions/{id}", sessionId)
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("duration", 90, "sessionDate", updateRequest.getSessionDate()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration").value(90));

        // Step 4: Cancel Session
        mockMvc.perform(put("/api/v1/sessions/{id}/status", sessionId).content("{\"status\":\"cancelled\"}")
                        .headers(createHeaders(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));

        // Step 5: Verify session is cancelled
        enterFixtureTenant();
        Session cancelledSession = sessionRepository.findById(sessionId).orElse(null);
        assertThat(cancelledSession).isNotNull();
        assertThat(cancelledSession.getStatus()).isEqualTo("cancelled");
    }

    @Test
    @DisplayName("Should handle session booking to completion workflow")
    void shouldHandleSessionBookingToCompletionWorkflow() throws Exception {
        // Step 1: Create Session
        CreateSessionRequest createRequest = new CreateSessionRequest();
        createRequest.setClientId(client.getId());
        createRequest.setTherapistId(therapist.getId());
        createRequest.setServiceId(fixtureService.getId());
        createRequest.setRoomId(fixtureRoom.getId());
        createRequest.setSessionType("individual");
        createRequest.setSessionDate(fixtureSessionDate(1));
        createRequest.setDuration(60);
        createRequest.setSessionMode("in_person");

        mockMvc.perform(post("/api/v1/sessions")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Step 2: Verify session exists
        enterFixtureTenant();
        assertThat(sessionRepository.findByClientId(client.getId())).hasSize(1);

        // Step 3: Get sessions for client
        mockMvc.perform(get("/api/v1/sessions")
                        .headers(createHeaders(authToken))
                        .param("clientId", client.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(greaterThanOrEqualTo(1)));
    }
}



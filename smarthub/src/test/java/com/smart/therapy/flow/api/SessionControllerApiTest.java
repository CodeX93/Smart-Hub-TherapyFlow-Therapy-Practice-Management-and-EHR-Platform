package com.smart.therapy.flow.api;

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


import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SessionController API Tests")
class SessionControllerApiTest extends BaseTenantApiTest {

    @Autowired
    private SessionRepository sessionRepository;

    private User therapist;
    private Client client;
    private Session session;
    private String authToken;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        authToken = getAuthToken(therapist.getEmail(), "password123");

        client = persistClient(therapist);
        client = clientRepository.save(client);

        session = Session.builder()
                .client(client)
                .service(fixtureService)
                .therapist(therapist)
                .sessionDate(fixtureSessionDate(1))
                .duration(60)
                .status(com.smart.therapy.flow.session.enums.SessionStatus.SCHEDULED.getValue())
                .sessionType(com.smart.therapy.flow.session.enums.SessionType.IN_PERSON.getValue())
                .clinicalSessionType("psychotherapy")
                .build();
        session = sessionRepository.save(session);
    }

    @Test
    @DisplayName("Should create session via API successfully")
    void shouldCreateSessionViaApiSuccessfully() throws Exception {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setClientId(client.getId());
        request.setTherapistId(therapist.getId());
        request.setServiceId(fixtureService.getId());
        request.setRoomId(fixtureRoom.getId());
        request.setSessionType("individual");
        request.setSessionDate(fixtureSessionDate(2));
        request.setDuration(60);
        request.setSessionMode("in_person");

        // Act & Assert
        mockMvc.perform(post("/api/v1/sessions")
                        .headers(createHeaders(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.duration").value(60));
    }

    @Test
    @DisplayName("Should get session by ID via API successfully")
    void shouldGetSessionByIdViaApiSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/sessions/{id}", session.getId())
                        .headers(createHeaders(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.duration").value(60));
    }

    @Test
    @DisplayName("Should return 404 when session not found")
    void shouldReturn404WhenSessionNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/sessions/99999")
                        .headers(createHeaders(authToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return list of sessions via API")
    void shouldReturnListOfSessionsViaApi() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/sessions")
                        .headers(createHeaders(authToken))
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Should cancel session via API successfully")
    void shouldCancelSessionViaApiSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/v1/sessions/{id}/status", session.getId()).content("{\"status\":\"cancelled\"}")
                        .headers(createHeaders(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));
    }
}


